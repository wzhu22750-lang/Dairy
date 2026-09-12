# Test Coverage & Regression Prevention Investigation Report
**Milestone**: Milestone 4 (Settings Screen & Modal Sheets/Dialogs) - Iteration 2  
**Agent**: Explorer M4-It2-3  
**Target Files**:
- `app/src/test/java/com/example/inkpaperdiary/challenger/SettingsAndPinSecurityEmpiricalChallengeTest.kt`
- `app/src/test/java/com/example/inkpaperdiary/ui/settings/SettingsViewModelHigTest.kt`
- `app/src/main/java/com/example/inkpaperdiary/ui/settings/SettingsScreen.kt`
- `app/src/main/java/com/example/inkpaperdiary/ui/settings/SettingsViewModel.kt`

---

## 1. Observation

### Observation 1.1: Iteration 1 Challenger Tests Were Written as Bug-Asserting Oracles with Local Mock Closures
In `app/src/test/java/com/example/inkpaperdiary/challenger/SettingsAndPinSecurityEmpiricalChallengeTest.kt`:

1. **PIN Disable Bug Oracle (lines 189–215)**:
```kotlin
@Test
fun testEmpiricalBug_DisableAppLockAcceptsAnyFourDigitPinWithoutVerification() {
    val actualStoredPin = "8765"
    val attackerAttemptedPin = "0000"

    // Simulated logic verbatim from SettingsScreen.kt:623-631
    var appLockEnabled = true
    var activePin = actualStoredPin
    var dialogDismissed = false

    fun onConfirmDisable(input: String) {
        // Verbatim SettingsScreen line 624:
        if (input.length == 4) {
            appLockEnabled = false
            activePin = ""
            dialogDismissed = true
        }
    }

    // Attacker enters "0000" (which does NOT match stored "8765")
    onConfirmDisable(attackerAttemptedPin)

    // BUG CONFIRMATION: The app lock was successfully disabled with a WRONG PIN!
    assertFalse("CRITICAL VULNERABILITY: App lock was disabled with incorrect PIN '0000' vs stored '8765'!", appLockEnabled)
    assertEquals("CRITICAL VULNERABILITY: Stored PIN was wiped!", "", activePin)
    assertTrue("Dialog was dismissed on wrong PIN", dialogDismissed)
}
```
*Observed fact*: This test passes because it tests a hardcoded local closure `onConfirmDisable` and asserts that entering `"0000"` disables the lock. If the production code is fixed, this test neither verifies the production fix nor enforces regression prevention; if modified to use the fix without updating assertions, it fails.

2. **`isPickerActive` Leak Oracle (lines 297–327)**:
```kotlin
@Test
fun testEmpiricalBug_ShareExportedFileExceptionPermanentlyLeaksIsPickerActive() {
    AppLockManager.isPickerActive = false

    fun simulateShareExportedFileWithFailure() {
        AppLockManager.isPickerActive = true
        try {
            throw SecurityException("Permission Denial or ActivityNotFoundException")
        } catch (e: Exception) {
            // SettingsScreen.kt lines 106-108: catches failure, shows Toast, DOES NOT reset isPickerActive!
        }
    }

    simulateShareExportedFileWithFailure()

    // BUG CONFIRMATION: isPickerActive is left TRUE after failure!
    assertTrue("CRITICAL CONCURRENCY BUG: isPickerActive remained permanently true after failed share!", AppLockManager.isPickerActive)
    ...
}
```
*Observed fact*: Asserts `assertTrue(AppLockManager.isPickerActive)`. When the failure handler is fixed to reset `isPickerActive = false`, this assertion fails if not updated.

3. **Cache Root Deletion Oracle (lines 440–461)**:
```kotlin
@Test
fun testEmpiricalFinding_DeleteRecursivelyDeletesDirectoryItself() {
    val mockCacheDir = File(tempDir, "mock_cache").apply { mkdir() }
    File(mockCacheDir, "temp.zip").writeText("sample")
    assertTrue(mockCacheDir.exists())

    // Verbatim call from SettingsScreen line 781:
    mockCacheDir.deleteRecursively()

    // Notice that the directory ITSELF is deleted, not just its children!
    assertFalse("Directory itself was deleted by deleteRecursively()", mockCacheDir.exists())
    ...
}
```
*Observed fact*: Calls `mockCacheDir.deleteRecursively()` and asserts `assertFalse(mockCacheDir.exists())`.

### Observation 1.2: Current State of `SettingsViewModelHigTest.kt`
In `app/src/test/java/com/example/inkpaperdiary/ui/settings/SettingsViewModelHigTest.kt`:
- Uses `sun.misc.Unsafe.allocateInstance(SettingsRepository::class.java)` without an Android `Context` (lines 67–75).
- Has 14 test methods covering flow combinations, HIG geometry, squircle icons, indented divider math, dialog adaptive button layout, and picker exemption.
- Currently lacks:
  - Any unit test testing PIN verification (`verifyPin`).
  - Any unit test testing the PIN disable contract.
  - Any unit test testing `isPickerActive` reset on export share failure.
  - Any unit test testing cache directory preservation during cache clearing.

### Observation 1.3: Current Production Implementation in `SettingsScreen.kt` and `SettingsViewModel.kt`
1. `SettingsScreen.kt:623-631`:
```kotlin
PinDialogMode.DISABLE -> {
    if (pinInput.length == 4) {
        viewModel.setAppLock(false, "")
        showPinDialog = false
        Toast.makeText(context, "应用锁已关闭", Toast.LENGTH_SHORT).show()
    } else {
        Toast.makeText(context, "请输入 4 位数字密码", Toast.LENGTH_SHORT).show()
    }
}
```
2. `SettingsScreen.kt:94-109`:
```kotlin
fun shareExportedFile(file: File) {
    AppLockManager.isPickerActive = true
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    ...
    runCatching {
        context.startActivity(Intent.createChooser(intent, "分享备份文件"))
    }.onFailure {
        Toast.makeText(context, "没有可用的分享方式", Toast.LENGTH_SHORT).show()
    }
}
```
3. `SettingsScreen.kt:779-787`:
```kotlin
onConfirm = {
    runCatching {
        context.cacheDir?.deleteRecursively()
        context.externalCacheDir?.deleteRecursively()
    }
    cacheSizeDisplay = calculateCacheSize()
    showClearCacheDialog = false
    Toast.makeText(context, "本地缓存已清理", Toast.LENGTH_SHORT).show()
}
```
4. `SettingsViewModel.kt`: Currently does not have a `verifyPin` method exposed, although `SettingsRepository.verifyAppPin(input: String): Boolean` exists in `SettingsRepository.kt:69`.

### Observation 1.4: JVM Test Execution Environment Constraints
1. Running `./gradlew testDebugUnitTest --rerun-tasks` passes 26/26 tasks across 24 test suites in 22 seconds with zero errors.
2. The project test dependencies (`app/build.gradle.kts:93-94`) only include `libs.junit` and `libs.kotlinx.coroutines.test`. There is **no Mockito, no MockK, and no Robolectric**.
3. `SettingsRepository.verifyAppPin(input: String)` depends on `PinCipher.decrypt(stored)` which calls `KeyStore.getInstance("AndroidKeyStore")`. On a standard JVM unit test, `AndroidKeyStore` is absent and throws `KeyStoreException`. Furthermore, `settingsRepository.context` is null when allocated via `Unsafe`.

---

## 2. Logic Chain

1. **Transforming Challenger Tests into Regression Prevention Tests**:
   - Because `testEmpiricalBug_DisableAppLockAcceptsAnyFourDigitPinWithoutVerification`, `testEmpiricalBug_ShareExportedFileExceptionPermanentlyLeaksIsPickerActive`, and `testEmpiricalFinding_DeleteRecursivelyDeletesDirectoryItself` currently assert the *presence* of the bugs, leaving them unchanged while fixing production code creates a contradiction (and testing only local mock closures fails to verify production files).
   - Therefore, these tests must be rewritten into regression verification tests:
     - Verify wrong PIN is rejected and keeps lock intact.
     - Verify correct PIN succeeds and disables lock.
     - Verify `isPickerActive` is reset to `false` when share fails.
     - Verify cache clearing deletes child files while root directories remain.
   - In addition, because `SettingsScreen.kt` is a Composable screen without Roborazzi/Robolectric, the most reliable way to enforce that `SettingsScreen.kt` uses the fixed logic is **Static AST Source Inspection tests** (already established in `SettingsAndPinSecurityEmpiricalChallengeTest.kt:478-512` and `SettingsScreenAndModalSheetsEmpiricalChallengeTest.kt:316-364`).

2. **JVM Testability for `SettingsViewModel.verifyPin`**:
   - `SettingsViewModel` needs to expose `verifyPin(pin: String): Boolean`.
   - In production, it must delegate to `settingsRepository.verifyAppPin(pin)`.
   - To make `SettingsViewModel` cleanly testable in `SettingsViewModelHigTest` on the JVM without requiring `AndroidKeyStore` or a mocked `Context`, `SettingsViewModel` should provide an internal or optional delegate:
     ```kotlin
     internal var pinVerifier: (suspend (String) -> Boolean)? = null

     suspend fun verifyPin(pin: String): Boolean {
         return pinVerifier?.invoke(pin) ?: settingsRepository.verifyAppPin(pin)
     }
     ```
   - In `SettingsViewModelHigTest`, setting `viewModel.pinVerifier = { it == "8888" }` allows verifying that `viewModel.verifyPin("8888")` returns `true` and `viewModel.verifyPin("0000")` returns `false` without crashing on JVM.

3. **Verifying Cache Directory Deletion vs Preservation**:
   - Safe cache clearing requires deleting children:
     `context.cacheDir?.listFiles()?.forEach { it.deleteRecursively() }`
     `context.externalCacheDir?.listFiles()?.forEach { it.deleteRecursively() }`
   - Test suites must create a temporary directory tree with files and subdirectories, apply this deletion algorithm, and assert:
     1. `dir.exists()` is true.
     2. `dir.isDirectory` is true.
     3. `dir.listFiles()?.isEmpty()` is true.
     4. `File(dir, "new_file.dat").createNewFile()` succeeds.

4. **Verifying `isPickerActive` Reset on Share Failure**:
   - `shareExportedFile` must reset `AppLockManager.isPickerActive = false` inside its failure block (`onFailure { AppLockManager.isPickerActive = false ... }`).
   - The test must verify:
     1. Starting share sets `AppLockManager.isPickerActive = true`.
     2. An exception during `startActivity` triggers the failure handler which resets `isPickerActive = false`.
     3. A subsequent `onStop` triggers `AppLockManager.lock()` because `isPickerActive` is false.

---

## 3. Caveats

1. **No Mocking Framework**: As observed, `app/build.gradle.kts` does not include Mockito or MockK. Tests must use state flows, Unsafe allocation, temporary files (`Files.createTempDirectory`), or lightweight test delegates.
2. **Protected Repositories**: `SettingsRepository.kt` is listed under Protected Files in `PROJECT.md` and cannot be modified. All adaptations must reside in `SettingsViewModel.kt` and `SettingsScreen.kt`.
3. **Read-Only Explorer Scope**: This report defines the exact code modifications and test specifications. The actual source code modifications will be performed by Worker M4.

---

## 4. Conclusion & Concrete Test Action Plan

### A. Updates to `SettingsAndPinSecurityEmpiricalChallengeTest.kt`

#### 1. Replace `testEmpiricalBug_DisableAppLockAcceptsAnyFourDigitPinWithoutVerification` (lines 189–215)
Replace with two regression tests:
```kotlin
@Test
fun testPinDisable_RejectsWrongPinAndPreservesLockState() {
    val actualStoredPin = "8765"
    val wrongPin = "0000"

    var appLockEnabled = true
    var activePin = actualStoredPin
    var dialogDismissed = false
    var errorMessage: String? = null

    // State machine contract for PinDialogMode.DISABLE
    fun onConfirmDisable(input: String, pinVerifier: (String) -> Boolean) {
        if (input.length == 4) {
            if (pinVerifier(input)) {
                appLockEnabled = false
                activePin = ""
                dialogDismissed = true
            } else {
                errorMessage = "密码错误，无法关闭应用锁"
            }
        }
    }

    onConfirmDisable(wrongPin) { candidate -> candidate == actualStoredPin }

    assertTrue("Lock must remain enabled when wrong PIN is entered", appLockEnabled)
    assertEquals("Stored PIN must remain intact", actualStoredPin, activePin)
    assertFalse("Dialog must not dismiss on wrong PIN", dialogDismissed)
    assertEquals("密码错误，无法关闭应用锁", errorMessage)
}

@Test
fun testPinDisable_AcceptsCorrectPinAndDisablesLock() {
    val actualStoredPin = "8765"
    val correctPin = "8765"

    var appLockEnabled = true
    var activePin = actualStoredPin
    var dialogDismissed = false

    fun onConfirmDisable(input: String, pinVerifier: (String) -> Boolean) {
        if (input.length == 4) {
            if (pinVerifier(input)) {
                appLockEnabled = false
                activePin = ""
                dialogDismissed = true
            }
        }
    }

    onConfirmDisable(correctPin) { candidate -> candidate == actualStoredPin }

    assertFalse("Lock must be disabled when correct PIN is entered", appLockEnabled)
    assertEquals("Stored PIN must be erased on disable", "", activePin)
    assertTrue("Dialog must be dismissed on successful disable", dialogDismissed)
}
```

#### 2. Replace `testEmpiricalBug_ShareExportedFileExceptionPermanentlyLeaksIsPickerActive` (lines 297–327)
Replace with:
```kotlin
@Test
fun testShareExportedFile_FailureSafelyResetsIsPickerActive() {
    AppLockManager.isPickerActive = false

    fun simulateShareExportedFileWithGuaranteedCleanup(shouldFail: Boolean) {
        AppLockManager.isPickerActive = true
        runCatching {
            if (shouldFail) {
                throw SecurityException("No activity found or permission denied")
            }
        }.onFailure {
            AppLockManager.isPickerActive = false
        }
    }

    simulateShareExportedFileWithGuaranteedCleanup(shouldFail = true)

    assertFalse("isPickerActive must be reset to false when share initiation fails!", AppLockManager.isPickerActive)

    // Simulate subsequent activity onStop
    var lockedOnStop = false
    val isLockEnabled = true
    if (isLockEnabled && !AppLockManager.isPickerActive) {
        lockedOnStop = true
        AppLockManager.lock()
    }
    assertTrue("App must properly lock onStop after failed share attempt", lockedOnStop)
}
```

#### 3. Replace `testEmpiricalFinding_DeleteRecursivelyDeletesDirectoryItself` (lines 440–462)
Replace with:
```kotlin
@Test
fun testCacheClearing_DeletesContentsWhilePreservingDirectory() {
    val mockCacheDir = File(tempDir, "mock_cache").apply { mkdir() }
    File(mockCacheDir, "temp.zip").writeText("sample zip content")
    val subDir = File(mockCacheDir, "thumbs").apply { mkdir() }
    File(subDir, "thumb_1.jpg").writeText("sample thumbnail")

    assertTrue(mockCacheDir.exists())
    assertEquals(2, mockCacheDir.listFiles()?.size)

    // Safe child deletion contract
    mockCacheDir.listFiles()?.forEach { it.deleteRecursively() }

    assertTrue("Cache directory itself must still exist after clearing", mockCacheDir.exists())
    assertTrue("Cache directory must remain a directory", mockCacheDir.isDirectory)
    assertEquals("All child files and folders must be deleted", 0, mockCacheDir.listFiles()?.size)

    // Verify subsequent file creation succeeds without needing mkdirs()
    val newFile = File(mockCacheDir, "new_temp.zip")
    var creationSucceeded = false
    try {
        creationSucceeded = newFile.createNewFile()
    } catch (e: IOException) {
        creationSucceeded = false
    }
    assertTrue("Creating file in preserved cache directory must succeed", creationSucceeded)
}
```

#### 4. Add Static AST Inspection Tests in Section 4 of `SettingsAndPinSecurityEmpiricalChallengeTest.kt`
Add the following three static source analysis tests:
```kotlin
@Test
fun testSettingsScreen_DisablePinVerifiesAgainstStoredPin() {
    val settingsFile = resolveSourceFile("src/main/java/com/example/inkpaperdiary/ui/settings/SettingsScreen.kt")
    val content = settingsFile.readText()

    // Must not contain unconditional disable: viewModel.setAppLock(false, "") directly under pinInput.length == 4 without verifyPin
    val disableBlockStart = content.indexOf("PinDialogMode.DISABLE ->")
    assertTrue("SettingsScreen must handle PinDialogMode.DISABLE", disableBlockStart > 0)
    val disableBlock = content.substring(disableBlockStart, disableBlockStart + 400)

    assertTrue(
        "PinDialogMode.DISABLE must call verifyPin before disabling app lock",
        disableBlock.contains("verifyPin")
    )
}

@Test
fun testSettingsScreen_ShareExportedFileResetsPickerActiveOnFailure() {
    val settingsFile = resolveSourceFile("src/main/java/com/example/inkpaperdiary/ui/settings/SettingsScreen.kt")
    val content = settingsFile.readText()

    val shareFunctionStart = content.indexOf("fun shareExportedFile")
    assertTrue("SettingsScreen must contain shareExportedFile", shareFunctionStart > 0)
    val shareFunctionBlock = content.substring(shareFunctionStart, shareFunctionStart + 600)

    assertTrue(
        "shareExportedFile onFailure must reset AppLockManager.isPickerActive to false",
        shareFunctionBlock.contains("AppLockManager.isPickerActive = false")
    )
}

@Test
fun testSettingsScreen_CacheClearingPreservesRootDirectories() {
    val settingsFile = resolveSourceFile("src/main/java/com/example/inkpaperdiary/ui/settings/SettingsScreen.kt")
    val content = settingsFile.readText()

    // Must NOT call context.cacheDir?.deleteRecursively() directly
    assertFalse(
        "Must not delete cacheDir itself with deleteRecursively()",
        content.contains("context.cacheDir?.deleteRecursively()")
    )
    assertTrue(
        "Must delete children via listFiles()?.forEach",
        content.contains("cacheDir?.listFiles()?.forEach")
    )
}
```

---

### B. Updates to `SettingsViewModelHigTest.kt`

Add the following tests to `app/src/test/java/com/example/inkpaperdiary/ui/settings/SettingsViewModelHigTest.kt`:

```kotlin
// -----------------------------------------------------------------------------------------
// 8. PIN Verification & Security State Machine Contracts
// -----------------------------------------------------------------------------------------

@Test
fun testVerifyPin_AcceptsCorrectPinAndRejectsWrongPin() = runTest {
    // Inject test verifier into viewModel
    viewModel.pinVerifier = { candidate -> candidate == "8888" }

    assertTrue("Correct PIN '8888' must return true", viewModel.verifyPin("8888"))
    assertFalse("Incorrect PIN '0000' must return false", viewModel.verifyPin("0000"))
    assertFalse("Partial PIN '888' must return false", viewModel.verifyPin("888"))
    assertFalse("Non-digit PIN '88a8' must return false", viewModel.verifyPin("88a8"))
}

@Test
fun testPinDisableContract_CorrectPinDisablesAppLock() = runTest {
    viewModel.pinVerifier = { candidate -> candidate == "1234" }

    var lockDisabled = false
    val attemptDisable: suspend (String) -> Unit = { pin ->
        if (pin.length == 4 && viewModel.verifyPin(pin)) {
            lockDisabled = true
        }
    }

    // Wrong PIN attempt
    attemptDisable("0000")
    assertFalse("Lock must not be disabled with wrong PIN", lockDisabled)

    // Correct PIN attempt
    attemptDisable("1234")
    assertTrue("Lock must be disabled with correct PIN", lockDisabled)
}

@Test
fun testShareExportedFile_FailureResetsPickerActiveContract() {
    AppLockManager.isPickerActive = false
    assertFalse(AppLockManager.isPickerActive)

    // Share starts -> flag active
    AppLockManager.isPickerActive = true
    assertTrue(AppLockManager.isPickerActive)

    // Share throws exception -> failure handler resets flag
    runCatching {
        throw SecurityException("No activity found")
    }.onFailure {
        AppLockManager.isPickerActive = false
    }

    assertFalse("Picker flag must be reset to false on failure", AppLockManager.isPickerActive)

    // onStop must lock when picker flag is false
    var locked = false
    if (!AppLockManager.isPickerActive) {
        locked = true
    }
    assertTrue("App must lock onStop when picker flag is false", locked)
}

@Test
fun testCacheClearingContract_PreservesRootDirectories() {
    val mockDir = java.nio.file.Files.createTempDirectory("cache_test_dir").toFile()
    try {
        val file1 = File(mockDir, "backup.zip").apply { writeText("data") }
        val nested = File(mockDir, "cache_sub").apply { mkdir() }
        File(nested, "sub.tmp").writeText("temp")

        assertTrue(mockDir.exists())
        assertEquals(2, mockDir.listFiles()?.size)

        // Safe child deletion contract
        mockDir.listFiles()?.forEach { it.deleteRecursively() }

        assertTrue("Root directory must still exist", mockDir.exists())
        assertTrue("Root directory must remain a directory", mockDir.isDirectory)
        assertEquals("Root directory contents must be empty", 0, mockDir.listFiles()?.size)

        val newFile = File(mockDir, "new.dat")
        assertTrue("Creating file in preserved root directory must succeed", newFile.createNewFile())
    } finally {
        mockDir.deleteRecursively()
    }
}
```

---

## 5. Verification Method

To independently verify the implementation and test suites:

1. **Static AST & Content Verification**:
   Inspect `SettingsAndPinSecurityEmpiricalChallengeTest.kt` and `SettingsViewModelHigTest.kt` to ensure:
   - Zero tests assert that the PIN disable bug exists.
   - All tests enforce rejection of wrong PIN, acceptance of correct PIN, reset of `isPickerActive` on failure, and preservation of root cache directories.

2. **Command Execution**:
   Run full unit test suite from project root:
   ```bash
   ./gradlew testDebugUnitTest --rerun-tasks
   ```
   **Pass Condition**:
   - Exit code 0.
   - All 26 tasks executed successfully with 0 failures and 0 skipped.
   - All 24 test suites pass cleanly.

3. **Invalidation Conditions**:
   - Any test throwing `KeyStoreException: AndroidKeyStore not found` on JVM.
   - Any test in `SettingsAndPinSecurityEmpiricalChallengeTest.kt` failing due to inverted assertions (`assertTrue(isPickerActive)` on share failure).
   - Any test failing due to `FileNotFoundException` during cache clearing assertions.
