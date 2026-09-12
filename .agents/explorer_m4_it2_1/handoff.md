# Handoff Report: Explorer M4-It2-1 for Milestone 4 (Settings Screen & Modal Sheets/Dialogs) - Iteration 2

**Agent**: Explorer M4-It2-1 (explorer, investigator)  
**Working Directory**: `/Users/kuangqie/Documents/VibeCoding/日记本/.agents/explorer_m4_it2_1`  
**Date**: 2026-09-06T12:05:40Z  
**Handoff Type**: Hard (Investigation Complete)  
**Target Milestone**: Milestone 4 - Iteration 2 (Settings Screen & Modal Sheets/Dialogs)  

---

## 1. Observation

### 1.1 Existing Authentic Verification in Repository
In `app/src/main/java/com/example/inkpaperdiary/data/repository/SettingsRepository.kt` lines 69-82, authentic PIN verification is already fully implemented using Android Keystore AES-GCM with backward-compatible migration:
```kotlin
    /** 校验输入 PIN 是否正确；兼容旧版本明文存储的 PIN，验证通过后自动迁移为密文。 */
    suspend fun verifyAppPin(input: String): Boolean {
        val stored = appLockPin.first()
        if (stored.isBlank()) return false

        val plain = PinCipher.decrypt(stored)
        if (plain != null) return plain == input

        // 旧版本明文兼容：直接比较，成功后升级为密文
        if (stored == input) {
            context.dataStore.edit { prefs -> prefs[KEY_APP_LOCK_PIN] = PinCipher.encrypt(input) }
            return true
        }
        return false
    }
```
`SettingsRepository.kt` is marked as protected in `PROJECT.md:145` (`data/repository/**`). It requires zero changes.

### 1.2 The PIN Verification Gap in SettingsViewModel
In `app/src/main/java/com/example/inkpaperdiary/ui/settings/SettingsViewModel.kt`:
- Lines 126-130:
  ```kotlin
  fun setAppLock(enabled: Boolean, pin: String) {
      viewModelScope.launch {
          settingsRepository.setAppLock(enabled, pin)
      }
  }
  ```
- Observation: `SettingsViewModel` has no method that exposes `settingsRepository.verifyAppPin` to the UI or to tests. Neither `suspend fun verifyPin` nor callback-based `fun verifyPin` currently exists in `SettingsViewModel`.

### 1.3 The Facade Verification in SettingsScreen (PinDialogMode.DISABLE)
In `app/src/main/java/com/example/inkpaperdiary/ui/settings/SettingsScreen.kt`:
- Lines 589 & 594:
  ```kotlin
  val pinDialogTitle = when (pinDialogMode) { ... PinDialogMode.DISABLE -> "关闭应用锁" }
  val pinDialogMessage = when (pinDialogMode) { ... PinDialogMode.DISABLE -> "请输入当前 4 位数字密码以确认关闭应用锁" }
  ```
- Lines 623-631:
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
- Observation: The UI presents a password prompt asking for the user's current 4-digit PIN with a masked text field (`PasswordVisualTransformation`). However, `onConfirm` only checks `pinInput.length == 4` and unconditionally deletes the PIN (`viewModel.setAppLock(false, "")`). Any 4 digits (e.g. `0000`) disables the lock without authenticating against the stored PIN.

### 1.4 Unauthenticated PIN Change (PinDialogMode.CHANGE)
In `app/src/main/java/com/example/inkpaperdiary/ui/settings/SettingsScreen.kt`:
- Lines 366-371:
  ```kotlin
  onClick = {
      pinInput = ""
      pinDialogMode = PinDialogMode.CHANGE
      showPinDialog = true
  }
  ```
- Lines 614-622:
  ```kotlin
  PinDialogMode.CHANGE -> {
      if (pinInput.length == 4) {
          viewModel.setAppLock(true, pinInput)
          showPinDialog = false
          Toast.makeText(context, "PIN 密码修改成功", Toast.LENGTH_SHORT).show()
      } else {
          Toast.makeText(context, "新密码须为 4 位数字", Toast.LENGTH_SHORT).show()
      }
  }
  ```
- Observation: Tapping "修改 PIN 密码" immediately requests the new PIN and overwrites the active PIN without verifying the user's current PIN first.

### 1.5 Unit Test Contract Constraint: `PinDialogMode` Enum Size
In `app/src/test/java/com/example/inkpaperdiary/ui/settings/SettingsViewModelHigTest.kt` lines 332-338:
```kotlin
@Test
fun testPinLifecycle_ValidationAndStateTransitions() {
    val modes = PinDialogMode.values()
    assertEquals(3, modes.size)
    assertTrue(modes.contains(PinDialogMode.SETUP))
    assertTrue(modes.contains(PinDialogMode.CHANGE))
    assertTrue(modes.contains(PinDialogMode.DISABLE))
    ...
}
```
- Observation: Existing unit tests strictly assert that `PinDialogMode.values().size == 3`. Therefore, `PinDialogMode` MUST NOT be modified by adding or removing enum constants. 2-step verification for PIN change must be modeled via a dedicated sub-step state (e.g. `ChangePinStep`).

### 1.6 Additional Findings from Reviewer M4-2 & Challenger M4-2
1. **`isPickerActive` Leak in `shareExportedFile`**:
   In `SettingsScreen.kt` lines 104-109:
   ```kotlin
   runCatching {
       context.startActivity(Intent.createChooser(intent, "分享备份文件"))
   }.onFailure {
       Toast.makeText(context, "没有可用的分享方式", Toast.LENGTH_SHORT).show()
   }
   ```
   If `startActivity` fails, `AppLockManager.isPickerActive` is NOT reset to `false`. Since no external activity opens, `MainActivity.onResume()` is never triggered, leaving `isPickerActive = true` permanently and disabling lock protection on subsequent app backgrounding.
2. **Destructive Deletion of Cache Directory Roots**:
   In `SettingsScreen.kt` lines 780-784:
   ```kotlin
   context.cacheDir?.deleteRecursively()
   context.externalCacheDir?.deleteRecursively()
   ```
   `deleteRecursively()` deletes the cache directory folder itself rather than only its children.

---

## 2. Logic Chain

### 2.1 ViewModel Verification Exposure
1. From Observation 1.1: `SettingsRepository` already has `suspend fun verifyAppPin(input: String): Boolean`.
2. From Observation 1.2: `SettingsViewModel` serves as the sole bridge between UI and repositories, adhering to unidirectional data flow.
3. In `SettingsViewModel.kt`, asynchronous operations invoked from Compose (`testSupabaseConnection`, `exportMarkdownZip`, `exportJsonBackup`, etc.) follow the callback pattern `onResult: (T) -> Unit` launched within `viewModelScope`.
4. However, unit tests and coroutine-aware callers benefit from direct `suspend` execution.
5. In addition, Challenger M4-2 specifically recommended `suspend fun verifyAppPin(input: String): Boolean`, while Reviewer M4-2 recommended `fun verifyPin(pin: String, onResult: (Boolean) -> Unit)`.
6. Therefore, `SettingsViewModel` should provide:
   - `suspend fun verifyPin(pin: String): Boolean = settingsRepository.verifyAppPin(pin)`
   - `fun verifyPin(pin: String, onResult: (Boolean) -> Unit) { viewModelScope.launch { onResult(settingsRepository.verifyAppPin(pin)) } }`
   - `suspend fun verifyAppPin(pin: String): Boolean = verifyPin(pin)` (transparent alias).
7. This satisfies both call styles and guarantees 100% API compatibility with all test harnesses.

### 2.2 Preserving `PinDialogMode` Contract While Enabling 2-Step PIN Change
1. From Observation 1.5: `SettingsViewModelHigTest.kt:333` asserts `assertEquals(3, modes.size)` on `PinDialogMode.values()`. Adding enum values like `CHANGE_OLD` or `CHANGE_NEW` to `PinDialogMode` would cause test regression.
2. To provide authentic 2-step verification without altering `PinDialogMode`, define a dedicated sub-step enum:
   ```kotlin
   enum class ChangePinStep {
       VERIFY_OLD,
       ENTER_NEW
   }
   ```
3. In `SettingsScreen.kt`, introduce state:
   ```kotlin
   var changePinStep by remember { mutableStateOf(ChangePinStep.VERIFY_OLD) }
   ```
4. When the user taps "修改 PIN 密码" (Row 2.2):
   - `pinInput = ""`
   - `pinDialogMode = PinDialogMode.CHANGE`
   - `changePinStep = ChangePinStep.VERIFY_OLD`
   - `showPinDialog = true`
5. Step 1 (Verify Old PIN):
   - Dialog Title: `"验证原 PIN 密码"`
   - Dialog Message: `"请输入当前 4 位数字密码以验证身份"`
   - Confirm Text: `"下一步"`
   - On Confirm: calls `viewModel.verifyPin(pinInput) { isValid -> ... }`:
     - If `isValid == true`: clears `pinInput = ""` and advances `changePinStep = ChangePinStep.ENTER_NEW`.
     - If `isValid == false`: clears `pinInput = ""` and displays Toast `"原 PIN 密码错误，请重新输入"`.
6. Step 2 (Enter New PIN):
   - Dialog Title: `"设置新 PIN 密码"`
   - Dialog Message: `"请输入新的 4 位数字安全密码"`
   - Confirm Text: `"确定"`
   - On Confirm: checks `pinInput.length == 4`, calls `viewModel.setAppLock(true, pinInput)`, resets `changePinStep = ChangePinStep.VERIFY_OLD`, closes dialog, and displays Toast `"PIN 密码修改成功"`.
7. Cancellation / Dismiss:
   - Dismissing or canceling at either step resets `showPinDialog = false`, `pinInput = ""`, `changePinStep = ChangePinStep.VERIFY_OLD`.
   - The active PIN is completely preserved if the user cancels.

### 2.3 Authentic Verification in `PinDialogMode.DISABLE`
1. From Observation 1.3: Disabling the lock currently accepts any 4 digits without verification.
2. When the user toggles off the "应用锁" switch (Row 2.1):
   - `pinInput = ""`
   - `pinDialogMode = PinDialogMode.DISABLE`
   - `showPinDialog = true`
   - Dialog Title: `"关闭应用锁"`
   - Dialog Message: `"请输入当前 4 位数字密码以确认关闭应用锁"`
3. On Confirm:
   - Checks `pinInput.length == 4`.
   - Calls `viewModel.verifyPin(pinInput) { isValid -> ... }`:
     - If `isValid == true`:
       - Calls `viewModel.setAppLock(false, "")`.
       - `showPinDialog = false`
       - `pinInput = ""`
       - Displays Toast `"应用锁已关闭"`.
     - If `isValid == false`:
       - `pinInput = ""`
       - Displays Toast `"PIN 密码错误，无法关闭应用锁"`.
       - Does NOT call `setAppLock(false, "")`.
       - App lock remains securely enabled.
4. Cancellation / Dismiss:
   - If the user cancels the dialog, `showPinDialog = false`, `pinInput = ""`.
   - `viewModel.setAppLock(false, "")` was never called, so `uiState.appLockEnabled` remains `true`. The switch stays enabled.

### 2.4 Companion Fixes Rationale
1. **`shareExportedFile` Failure**: Resetting `AppLockManager.isPickerActive = false` inside `onFailure` ensures the flag is restored even if no external activity handles the intent, preventing a permanent lock bypass on future backgroundings.
2. **Clear Cache**: Deleting `context.cacheDir?.listFiles()?.forEach { it.deleteRecursively() }` and `context.externalCacheDir?.listFiles()?.forEach { it.deleteRecursively() }` removes all cached files while keeping the root directory descriptors intact, avoiding `FileNotFoundException` in subsequent file creation operations.

---

## 3. Caveats

1. **Physical Biometric & Hardware Keystore**: Pure JVM unit test runners (`testDebugUnitTest`) do not have access to the hardware `AndroidKeyStore`. Verification logic at the ViewModel level must be verified via mocks/delegates, while end-to-end Keystore encryption is verified on an Android runtime environment.
2. **Auto-Lock Timeout Persistence**: `autoLockTimeout`, `themeMode`, and `fontMode` are maintained as reactive Compose states in `SettingsScreen.kt` because `SettingsRepository.kt` (protected file) does not declare DataStore keys for them. This intentional design maintains strict boundary protection.

---

## 4. Conclusion & Proposed Implementation Specification

### 4.1 Changes to `SettingsViewModel.kt`
**File**: `app/src/main/java/com/example/inkpaperdiary/ui/settings/SettingsViewModel.kt`  
**Location**: After line 130 (after `setAppLock`)

```kotlin
<<<< BEFORE (Line 126-130)
    fun setAppLock(enabled: Boolean, pin: String) {
        viewModelScope.launch {
            settingsRepository.setAppLock(enabled, pin)
        }
    }
==== AFTER
    fun setAppLock(enabled: Boolean, pin: String) {
        viewModelScope.launch {
            settingsRepository.setAppLock(enabled, pin)
        }
    }

    /** 校验输入 PIN 是否与存储的 PIN 匹配（挂起函数） */
    suspend fun verifyPin(pin: String): Boolean {
        return settingsRepository.verifyAppPin(pin)
    }

    /** 异步校验 PIN，通过回调返回校验结果，方便直接在 Compose 回调中调用 */
    fun verifyPin(pin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val isValid = settingsRepository.verifyAppPin(pin)
            onResult(isValid)
        }
    }

    /** 兼容别名：与 SettingsRepository.verifyAppPin 保持同名 */
    suspend fun verifyAppPin(pin: String): Boolean = verifyPin(pin)
>>>>
```

### 4.2 Changes to `SettingsScreen.kt`
**File**: `app/src/main/java/com/example/inkpaperdiary/ui/settings/SettingsScreen.kt`

#### Change 4.2.1: Add `ChangePinStep` Enum
**Location**: Lines 42-46
```kotlin
<<<< BEFORE (Line 42-46)
enum class PinDialogMode {
    SETUP,
    CHANGE,
    DISABLE
}
==== AFTER
enum class PinDialogMode {
    SETUP,
    CHANGE,
    DISABLE
}

enum class ChangePinStep {
    VERIFY_OLD,
    ENTER_NEW
}
>>>>
```

#### Change 4.2.2: Add `changePinStep` State Variable
**Location**: Lines 150-153
```kotlin
<<<< BEFORE (Line 150-153)
    var showPinDialog by remember { mutableStateOf(false) }
    var pinDialogMode by remember { mutableStateOf(PinDialogMode.SETUP) }
    var pinInput by remember { mutableStateOf("") }
==== AFTER
    var showPinDialog by remember { mutableStateOf(false) }
    var pinDialogMode by remember { mutableStateOf(PinDialogMode.SETUP) }
    var changePinStep by remember { mutableStateOf(ChangePinStep.VERIFY_OLD) }
    var pinInput by remember { mutableStateOf("") }
>>>>
```

#### Change 4.2.3: Fix `isPickerActive` Leak in `shareExportedFile`
**Location**: Lines 104-109
```kotlin
<<<< BEFORE (Line 104-109)
        runCatching {
            context.startActivity(Intent.createChooser(intent, "分享备份文件"))
        }.onFailure {
            Toast.makeText(context, "没有可用的分享方式", Toast.LENGTH_SHORT).show()
        }
==== AFTER
        runCatching {
            context.startActivity(Intent.createChooser(intent, "分享备份文件"))
        }.onFailure {
            AppLockManager.isPickerActive = false
            Toast.makeText(context, "没有可用的分享方式", Toast.LENGTH_SHORT).show()
        }
>>>>
```

#### Change 4.2.4: Trigger 2-Step PIN Change
**Location**: Lines 366-371
```kotlin
<<<< BEFORE (Line 366-371)
                        onClick = {
                            pinInput = ""
                            pinDialogMode = PinDialogMode.CHANGE
                            showPinDialog = true
                        },
==== AFTER
                        onClick = {
                            pinInput = ""
                            pinDialogMode = PinDialogMode.CHANGE
                            changePinStep = ChangePinStep.VERIFY_OLD
                            showPinDialog = true
                        },
>>>>
```

#### Change 4.2.5: Implement Authentic PIN Verification and 2-Step Dialog Flow
**Location**: Lines 585-643
```kotlin
<<<< BEFORE (Line 585-635)
    // 2. PIN Lifecycle Dialog (IosModalDialog)
    val pinDialogTitle = when (pinDialogMode) {
        PinDialogMode.SETUP -> "设置 PIN 密码"
        PinDialogMode.CHANGE -> "修改 PIN 密码"
        PinDialogMode.DISABLE -> "关闭应用锁"
    }
    val pinDialogMessage = when (pinDialogMode) {
        PinDialogMode.SETUP -> "请输入 4 位数字安全密码用于应用解锁"
        PinDialogMode.CHANGE -> "请输入新的 4 位数字安全密码"
        PinDialogMode.DISABLE -> "请输入当前 4 位数字密码以确认关闭应用锁"
    }

    IosModalDialog(
        visible = showPinDialog,
        title = pinDialogTitle,
        message = pinDialogMessage,
        confirmText = "确定",
        cancelText = "取消",
        onConfirm = {
            when (pinDialogMode) {
                PinDialogMode.SETUP -> {
                    if (pinInput.length == 4) {
                        viewModel.setAppLock(true, pinInput)
                        showPinDialog = false
                        Toast.makeText(context, "应用锁已启用", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "密码须为 4 位数字", Toast.LENGTH_SHORT).show()
                    }
                }
                PinDialogMode.CHANGE -> {
                    if (pinInput.length == 4) {
                        viewModel.setAppLock(true, pinInput)
                        showPinDialog = false
                        Toast.makeText(context, "PIN 密码修改成功", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "新密码须为 4 位数字", Toast.LENGTH_SHORT).show()
                    }
                }
                PinDialogMode.DISABLE -> {
                    if (pinInput.length == 4) {
                        viewModel.setAppLock(false, "")
                        showPinDialog = false
                        Toast.makeText(context, "应用锁已关闭", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "请输入 4 位数字密码", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        },
        onDismissRequest = { showPinDialog = false }
    ) {
==== AFTER
    // 2. PIN Lifecycle Dialog (IosModalDialog)
    val pinDialogTitle = when (pinDialogMode) {
        PinDialogMode.SETUP -> "设置 PIN 密码"
        PinDialogMode.CHANGE -> if (changePinStep == ChangePinStep.VERIFY_OLD) "验证原 PIN 密码" else "设置新 PIN 密码"
        PinDialogMode.DISABLE -> "关闭应用锁"
    }
    val pinDialogMessage = when (pinDialogMode) {
        PinDialogMode.SETUP -> "请输入 4 位数字安全密码用于应用解锁"
        PinDialogMode.CHANGE -> if (changePinStep == ChangePinStep.VERIFY_OLD) "请输入当前 4 位数字密码以验证身份" else "请输入新的 4 位数字安全密码"
        PinDialogMode.DISABLE -> "请输入当前 4 位数字密码以确认关闭应用锁"
    }
    val pinDialogConfirmText = if (pinDialogMode == PinDialogMode.CHANGE && changePinStep == ChangePinStep.VERIFY_OLD) "下一步" else "确定"

    IosModalDialog(
        visible = showPinDialog,
        title = pinDialogTitle,
        message = pinDialogMessage,
        confirmText = pinDialogConfirmText,
        cancelText = "取消",
        onConfirm = {
            when (pinDialogMode) {
                PinDialogMode.SETUP -> {
                    if (pinInput.length == 4) {
                        viewModel.setAppLock(true, pinInput)
                        showPinDialog = false
                        pinInput = ""
                        Toast.makeText(context, "应用锁已启用", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "密码须为 4 位数字", Toast.LENGTH_SHORT).show()
                    }
                }
                PinDialogMode.CHANGE -> {
                    when (changePinStep) {
                        ChangePinStep.VERIFY_OLD -> {
                            if (pinInput.length == 4) {
                                viewModel.verifyPin(pinInput) { isValid ->
                                    if (isValid) {
                                        pinInput = ""
                                        changePinStep = ChangePinStep.ENTER_NEW
                                    } else {
                                        pinInput = ""
                                        Toast.makeText(context, "原 PIN 密码错误，请重新输入", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                Toast.makeText(context, "请输入 4 位当前密码", Toast.LENGTH_SHORT).show()
                            }
                        }
                        ChangePinStep.ENTER_NEW -> {
                            if (pinInput.length == 4) {
                                viewModel.setAppLock(true, pinInput)
                                showPinDialog = false
                                pinInput = ""
                                changePinStep = ChangePinStep.VERIFY_OLD
                                Toast.makeText(context, "PIN 密码修改成功", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "新密码须为 4 位数字", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                PinDialogMode.DISABLE -> {
                    if (pinInput.length == 4) {
                        viewModel.verifyPin(pinInput) { isValid ->
                            if (isValid) {
                                viewModel.setAppLock(false, "")
                                showPinDialog = false
                                pinInput = ""
                                Toast.makeText(context, "应用锁已关闭", Toast.LENGTH_SHORT).show()
                            } else {
                                pinInput = ""
                                Toast.makeText(context, "PIN 密码错误，无法关闭应用锁", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(context, "请输入 4 位数字密码", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        },
        onDismissRequest = {
            showPinDialog = false
            pinInput = ""
            changePinStep = ChangePinStep.VERIFY_OLD
        }
    ) {
>>>>
```

#### Change 4.2.6: Fix Cache Directory Deletion
**Location**: Lines 780-784
```kotlin
<<<< BEFORE (Line 780-784)
        onConfirm = {
            runCatching {
                context.cacheDir?.deleteRecursively()
                context.externalCacheDir?.deleteRecursively()
            }
            cacheSizeDisplay = calculateCacheSize()
            showClearCacheDialog = false
            Toast.makeText(context, "本地缓存已清理", Toast.LENGTH_SHORT).show()
        }
==== AFTER
        onConfirm = {
            runCatching {
                context.cacheDir?.listFiles()?.forEach { it.deleteRecursively() }
                context.externalCacheDir?.listFiles()?.forEach { it.deleteRecursively() }
            }
            cacheSizeDisplay = calculateCacheSize()
            showClearCacheDialog = false
            Toast.makeText(context, "本地缓存已清理", Toast.LENGTH_SHORT).show()
        }
>>>>
```

---

## 5. Verification Method

### 5.1 Verification Commands
To independently verify the implementation once applied:
```bash
# 1. Compile Kotlin source files to ensure zero syntax or type errors
./gradlew compileDebugKotlin

# 2. Run Settings screen HIG test suite
./gradlew testDebugUnitTest --tests "com.example.inkpaperdiary.ui.settings.SettingsViewModelHigTest"

# 3. Run Challenger test suites
./gradlew testDebugUnitTest --tests "com.example.inkpaperdiary.challenger.SettingsAndPinSecurityEmpiricalChallengeTest"
./gradlew testDebugUnitTest --tests "com.example.inkpaperdiary.challenger.SettingsScreenAndModalSheetsEmpiricalChallengeTest"

# 4. Run full unit test suite (must pass 100%)
./gradlew testDebugUnitTest

# 5. Full assembleDebug compilation
./gradlew assembleDebug
```

### 5.2 Unit Tests to Add / Update
In `app/src/test/java/com/example/inkpaperdiary/ui/settings/SettingsViewModelHigTest.kt`:
1. Add test for `viewModel.verifyPin(pin, onResult)`: verify that passing correct and incorrect PINs triggers the callback with expected boolean values.
2. Add test for `ChangePinStep` state transitions: assert initial `VERIFY_OLD` -> invalid PIN stays `VERIFY_OLD` -> valid PIN advances to `ENTER_NEW` -> confirm saves new PIN and resets to `VERIFY_OLD`.
3. Update `SettingsAndPinSecurityEmpiricalChallengeTest.kt` line 190 (`testEmpiricalBug_DisableAppLockAcceptsAnyFourDigitPinWithoutVerification`): assert that incorrect PIN fails verification and does NOT disable the lock, proving the vulnerability is resolved.

### 5.3 Invalidation Conditions
This strategy is invalidated if:
1. `PinDialogMode.DISABLE` disables app lock without invoking `viewModel.verifyPin` / `settingsRepository.verifyAppPin`.
2. `PinDialogMode.CHANGE` permits modifying the PIN without first verifying the user's existing PIN.
3. `PinDialogMode` enum is modified (extra constants added), breaking `assertEquals(3, modes.size)`.
4. Any Room DAOs, `SettingsRepository`, `PinCipher`, or `SyncManager` files are modified or destabilized.
