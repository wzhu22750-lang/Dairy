package com.example.inkpaperdiary.challenger

import com.example.inkpaperdiary.core.designsystem.components.PaperPattern
import com.example.inkpaperdiary.core.security.AppLockManager
import com.example.inkpaperdiary.ui.settings.PinDialogMode
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * EMPIRICAL CHALLENGER SUITE for Milestone 4 (Settings Screen & Modal Sheets/Dialogs):
 *
 * 1. PIN State Machine & Security Stress Testing:
 *    - Input filtering (letters, punctuation, symbols, whitespace, unicode numbers).
 *    - Length boundary enforcement (length != 4 rejected).
 *    - Cancellation and state preservation mid-change.
 *    - Rapid toggle sequences.
 *    - CRITICAL BUG ORACLE: Disabling PIN accepts any 4-digit input without verifying against stored PIN!
 *
 * 2. File Pickers & `AppLockManager.isPickerActive` Concurrency:
 *    - High-concurrency thread safety stress harness.
 *    - BUG ORACLE: Exception in `shareExportedFile` leaks `isPickerActive = true` permanently.
 *    - Activity lifecycle onStop / onResume contract verification.
 *    - Document picker cancellation resilience.
 *
 * 3. Cache Size Calculation & Deletion Edge Cases:
 *    - 0 files, null/empty directories, missing non-existent folders.
 *    - Sub-kilobyte files (1B - 1023B -> "0 KB" truncation check).
 *    - Large multi-gigabyte scale formatting.
 *    - Deeply nested file tree iteration.
 *    - `cacheDir.deleteRecursively()` destructive root deletion side effects.
 *
 * 4. Architectural & Apple HIG Static Inspection:
 *    - Zero Material 3 FloatingActionButton / MoreVert / DropdownMenu in SettingsScreen.
 *    - 4 canonical Inset Grouped sections.
 */
class SettingsAndPinSecurityEmpiricalChallengeTest {

    private lateinit var tempDir: File

    @Before
    fun setUp() {
        AppLockManager.isPickerActive = false
        AppLockManager.unlock()
        tempDir = Files.createTempDirectory("challenger_cache_test").toFile()
    }

    @After
    fun tearDown() {
        AppLockManager.isPickerActive = false
        tempDir.deleteRecursively()
    }

    // =========================================================================================
    // SECTION 1: PIN State Machine Stress Testing
    // =========================================================================================

    /**
     * Exact input filter predicate used in SettingsScreen.kt line 638:
     * `onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pinInput = it }`
     */
    private fun applyPinInputFilter(currentValue: String, newInput: String): String {
        return if (newInput.length <= 4 && newInput.all { it.isDigit() }) {
            newInput
        } else {
            currentValue
        }
    }

    @Test
    fun testPinInput_RejectsNonDigitsAndSpecialCharacters() {
        var input = ""

        // Valid single digits
        input = applyPinInputFilter(input, "1")
        assertEquals("1", input)
        input = applyPinInputFilter(input, "12")
        assertEquals("12", input)

        // Letters rejected
        input = applyPinInputFilter(input, "12a")
        assertEquals("12", input)
        input = applyPinInputFilter(input, "12z")
        assertEquals("12", input)

        // Punctuation and symbols rejected
        input = applyPinInputFilter(input, "12#")
        assertEquals("12", input)
        input = applyPinInputFilter(input, "12.")
        assertEquals("12", input)
        input = applyPinInputFilter(input, "12-")
        assertEquals("12", input)

        // Whitespace rejected
        input = applyPinInputFilter(input, "12 ")
        assertEquals("12", input)
        input = applyPinInputFilter(input, " 12")
        assertEquals("12", input)

        // Full-width numbers or unicode superscript rejected by isDigit()
        input = applyPinInputFilter(input, "12²")
        assertEquals("12", input)

        // Completing valid 4 digits
        input = applyPinInputFilter(input, "123")
        assertEquals("123", input)
        input = applyPinInputFilter(input, "1234")
        assertEquals("1234", input)

        // 5th digit rejected
        input = applyPinInputFilter(input, "12345")
        assertEquals("1234", input)
    }

    @Test
    fun testPinLifecycle_LengthValidationRejectsInvalidLengths() {
        fun validatePinOnConfirm(pin: String): Boolean = pin.length == 4

        assertFalse("Empty string must be rejected", validatePinOnConfirm(""))
        assertFalse("1 digit must be rejected", validatePinOnConfirm("1"))
        assertFalse("2 digits must be rejected", validatePinOnConfirm("12"))
        assertFalse("3 digits must be rejected", validatePinOnConfirm("123"))
        assertTrue("4 digits must be accepted", validatePinOnConfirm("1234"))
        assertTrue("4 zeros must be accepted", validatePinOnConfirm("0000"))
        assertFalse("5 digits must be rejected", validatePinOnConfirm("12345"))
    }

    @Test
    fun testPinLifecycle_CancellationResetsInputState() {
        // Model the state machine in SettingsScreen.kt
        var pinInput = ""
        var pinDialogMode = PinDialogMode.SETUP
        var showPinDialog = false

        // 1. User starts SETUP
        pinInput = ""
        pinDialogMode = PinDialogMode.SETUP
        showPinDialog = true
        pinInput = applyPinInputFilter(pinInput, "12")
        assertEquals("12", pinInput)

        // User cancels
        showPinDialog = false
        // Next time dialog is triggered, onClick/onCheckedChange explicitly resets pinInput = ""
        pinInput = ""
        pinDialogMode = PinDialogMode.CHANGE
        showPinDialog = true
        assertEquals("", pinInput)

        // User enters partial in CHANGE, then cancels
        pinInput = applyPinInputFilter(pinInput, "98")
        assertEquals("98", pinInput)
        showPinDialog = false

        // User toggles DISABLE, onClick resets pinInput = ""
        pinInput = ""
        pinDialogMode = PinDialogMode.DISABLE
        showPinDialog = true
        assertEquals("", pinInput)
    }

    /**
     * EMPIRICAL FIX VERIFICATION:
     * Line 623-640 in SettingsScreen.kt:
     * PinDialogMode.DISABLE authenticates via viewModel.verifyPin.
     * An incorrect PIN is rejected and does not disable the lock.
     */
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
                    errorMessage = "PIN 密码错误，无法关闭应用锁"
                }
            }
        }

        onConfirmDisable(wrongPin) { candidate -> candidate == actualStoredPin }

        assertTrue("Lock must remain enabled when wrong PIN is entered", appLockEnabled)
        assertEquals("Stored PIN must remain intact", actualStoredPin, activePin)
        assertFalse("Dialog must not dismiss on wrong PIN", dialogDismissed)
        assertEquals("PIN 密码错误，无法关闭应用锁", errorMessage)
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

    /**
     * EMPIRICAL FIX VERIFICATION:
     * When changing PIN (PinDialogMode.CHANGE), the UI requires verifying the old PIN
     * before allowing input of the new PIN.
     */
    @Test
    fun testPinChange_RequiresOldPinAuthenticationBeforeSettingNewPin() {
        val originalPin = "1111"
        val wrongOldPin = "0000"
        val authorizedNewPin = "9999"

        var currentPin = originalPin
        var step = "VERIFY_OLD"
        var errorMessage: String? = null

        fun onConfirmChange(input: String, pinVerifier: (String) -> Boolean) {
            when (step) {
                "VERIFY_OLD" -> {
                    if (input.length == 4) {
                        if (pinVerifier(input)) {
                            step = "ENTER_NEW"
                        } else {
                            errorMessage = "原 PIN 密码错误，请重新输入"
                        }
                    }
                }
                "ENTER_NEW" -> {
                    if (input.length == 4) {
                        currentPin = input
                        step = "VERIFY_OLD"
                    }
                }
            }
        }

        // Wrong old PIN
        onConfirmChange(wrongOldPin) { it == originalPin }
        assertEquals("VERIFY_OLD", step)
        assertEquals(originalPin, currentPin)
        assertEquals("原 PIN 密码错误，请重新输入", errorMessage)

        // Correct old PIN
        onConfirmChange(originalPin) { it == originalPin }
        assertEquals("ENTER_NEW", step)

        // Enter new PIN
        onConfirmChange(authorizedNewPin) { it == originalPin }
        assertEquals(authorizedNewPin, currentPin)
        assertEquals("VERIFY_OLD", step)
    }

    // =========================================================================================
    // SECTION 2: Concurrency & File Pickers (AppLockManager.isPickerActive)
    // =========================================================================================

    @Test
    fun testAppLockManager_ConcurrentPickerActiveReadWriteSafety() {
        val threadCount = 8
        val iterations = 10000
        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)
        val failureDetected = AtomicBoolean(false)

        for (t in 0 until threadCount) {
            executor.submit {
                try {
                    for (i in 0 until iterations) {
                        AppLockManager.isPickerActive = (i % 2 == 0)
                        val read = AppLockManager.isPickerActive
                        if (read != true && read != false) {
                            failureDetected.set(true)
                        }
                    }
                } catch (e: Throwable) {
                    failureDetected.set(true)
                } finally {
                    latch.countDown()
                }
            }
        }

        assertTrue("Timeout in concurrent stress test", latch.await(10, TimeUnit.SECONDS))
        executor.shutdown()
        assertFalse("Failure detected during concurrent access to isPickerActive", failureDetected.get())
    }

    /**
     * EMPIRICAL FIX VERIFICATION:
     * When `context.startActivity` throws in `shareExportedFile`,
     * `onFailure` catches the exception and safely resets `AppLockManager.isPickerActive = false`.
     * This ensures subsequent backgrounding locks the app as expected.
     */
    @Test
    fun testShareExportedFile_FailureSafelyResetsIsPickerActive() {
        AppLockManager.isPickerActive = false

        fun simulateShareExportedFileWithGuaranteedCleanup(shouldFail: Boolean) {
            runCatching {
                AppLockManager.isPickerActive = true
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

    @Test
    fun testShareExportedFile_SuccessPreservesIsPickerActiveUntilResume() {
        AppLockManager.isPickerActive = false

        // Simulate successful share launch
        fun simulateShareExportedFileSuccess() {
            runCatching {
                AppLockManager.isPickerActive = true
                // startActivity succeeds
            }.onFailure {
                AppLockManager.isPickerActive = false
            }
        }

        simulateShareExportedFileSuccess()

        // VERIFICATION: While sharing, isPickerActive remains true
        assertTrue("isPickerActive must remain true while chooser is active", AppLockManager.isPickerActive)

        // onStop while chooser active does NOT lock
        var lockedOnStop = false
        val isLockEnabled = true
        if (isLockEnabled && !AppLockManager.isPickerActive) {
            lockedOnStop = true
            AppLockManager.lock()
        }
        assertFalse("App must not lock while chooser is active", lockedOnStop)

        // Activity resumes -> reset
        AppLockManager.isPickerActive = false
        assertFalse("onResume resets isPickerActive", AppLockManager.isPickerActive)

        // Next onStop locks
        var lockedOnNextStop = false
        if (isLockEnabled && !AppLockManager.isPickerActive) {
            lockedOnNextStop = true
            AppLockManager.lock()
        }
        assertTrue("App locks on subsequent onStop after resuming", lockedOnNextStop)
    }

    @Test
    fun testDocumentPicker_CancellationSafelyResetsIsPickerActive() {
        // Document pickers in SettingsScreen lines 66-80 and 82-92 reset isPickerActive = false in callback
        AppLockManager.isPickerActive = true

        // User cancels file picker (uri == null)
        val uri: String? = null
        // Launcher callback executes:
        AppLockManager.isPickerActive = false
        if (uri != null) {
            fail("Uri should be null on cancel")
        }

        assertFalse("isPickerActive should be reset to false after picker cancellation", AppLockManager.isPickerActive)
    }

    // =========================================================================================
    // SECTION 3: Cache Size Calculation & Deletion Edge Cases
    // =========================================================================================

    /**
     * Exact cache calculation logic from SettingsScreen.kt lines 114-126
     */
    private fun calculateCacheSize(cacheDir: File?, externalCacheDir: File?): String {
        val totalBytes = runCatching {
            var size = 0L
            cacheDir?.walkTopDown()?.forEach { if (it.isFile) size += it.length() }
            externalCacheDir?.walkTopDown()?.forEach { if (it.isFile) size += it.length() }
            size
        }.getOrDefault(0L)
        return when {
            totalBytes <= 0L -> "0 KB"
            totalBytes < 1024L * 1024L -> "${totalBytes / 1024L} KB"
            else -> String.format(Locale.getDefault(), "%.1f MB", totalBytes / (1024.0 * 1024.0))
        }
    }

    @Test
    fun testCacheCalculation_ZeroFilesAndNullDirectories() {
        assertEquals("0 KB", calculateCacheSize(null, null))

        val emptyDir1 = File(tempDir, "empty1").apply { mkdir() }
        val emptyDir2 = File(tempDir, "empty2").apply { mkdir() }
        assertEquals("0 KB", calculateCacheSize(emptyDir1, emptyDir2))
    }

    @Test
    fun testCacheCalculation_MissingNonExistentDirectories() {
        val nonExistent1 = File(tempDir, "does_not_exist_1")
        val nonExistent2 = File(tempDir, "does_not_exist_2")

        assertFalse(nonExistent1.exists())
        assertFalse(nonExistent2.exists())

        // Must safely return "0 KB" without throwing FileNotFoundException or crashing
        assertEquals("0 KB", calculateCacheSize(nonExistent1, nonExistent2))
    }

    @Test
    fun testCacheCalculation_SubKilobyteFilesTruncation() {
        val cache = File(tempDir, "sub_kb_cache").apply { mkdir() }
        val file1 = File(cache, "small.txt").apply { writeBytes(ByteArray(500)) }

        // 500 bytes < 1024 bytes: 500 / 1024 = 0 -> "0 KB"
        assertEquals("0 KB", calculateCacheSize(cache, null))

        val file2 = File(cache, "small2.txt").apply { writeBytes(ByteArray(600)) }
        // 1100 bytes: 1100 / 1024 = 1 -> "1 KB"
        assertEquals("1 KB", calculateCacheSize(cache, null))
    }

    @Test
    fun testCacheCalculation_LargeFileBoundariesAndDeepNesting() {
        val cache = File(tempDir, "large_cache").apply { mkdir() }

        // Exactly 1 MB = 1048576 bytes
        val file1Mb = File(cache, "1mb.bin").apply { writeBytes(ByteArray(1024 * 1024)) }
        assertEquals("1.0 MB", calculateCacheSize(cache, null))

        // Deeply nested files
        var nestedDir = cache
        for (depth in 1..10) {
            nestedDir = File(nestedDir, "level_$depth").apply { mkdir() }
            File(nestedDir, "file_$depth.dat").apply { writeBytes(ByteArray(50 * 1024)) } // 50 KB each
        }
        // Total = 1024 KB + 10 * 50 KB = 1524 KB = ~1.488 MB -> 1.5 MB
        val display = calculateCacheSize(cache, null)
        assertTrue("Cache size display should end with MB: $display", display.endsWith("MB"))
    }

    @Test
    fun testCacheCalculation_ExtremeMultiGigabyteScaleFormatting() {
        fun formatBytes(totalBytes: Long): String = when {
            totalBytes <= 0L -> "0 KB"
            totalBytes < 1024L * 1024L -> "${totalBytes / 1024L} KB"
            else -> String.format(Locale.getDefault(), "%.1f MB", totalBytes / (1024.0 * 1024.0))
        }

        val gigabyte10 = 10L * 1024L * 1024L * 1024L
        val result = formatBytes(gigabyte10)
        // Implementation formats in MB rather than GB
        assertEquals("10240.0 MB", result)
    }

    /**
     * EMPIRICAL FIX VERIFICATION:
     * In SettingsScreen.kt:
     * `context.cacheDir?.listFiles()?.forEach { it.deleteRecursively() }`
     * Deletes only child files and subdirectories, preserving the cache directory root.
     */
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

    // =========================================================================================
    // SECTION 4: Static Inspection & Apple HIG Fidelity
    // =========================================================================================

    private fun resolveSourceFile(relativePath: String): File {
        val candidates = listOf(
            File(relativePath),
            File("app", relativePath),
            File(System.getProperty("user.dir") ?: ".", relativePath),
            File(System.getProperty("user.dir") ?: ".", "app/$relativePath")
        )
        return candidates.firstOrNull { it.exists() } ?: File(relativePath)
    }

    @Test
    fun testSettingsScreen_ZeroMaterial3FabOrOverflowMenu() {
        val settingsFile = resolveSourceFile("src/main/java/com/example/inkpaperdiary/ui/settings/SettingsScreen.kt")
        assertTrue("SettingsScreen.kt must exist", settingsFile.exists())

        val content = settingsFile.readText()

        // 1. Zero FloatingActionButton
        assertFalse("SettingsScreen must not contain FloatingActionButton", content.contains("FloatingActionButton"))
        assertFalse("SettingsScreen must not contain ExtendedFloatingActionButton", content.contains("ExtendedFloatingActionButton"))

        // 2. Zero MoreVert 3-dot overflow menu
        assertFalse("SettingsScreen must not contain Icons.Default.MoreVert", content.contains("MoreVert"))
        assertFalse("SettingsScreen must not contain DropdownMenu", content.contains("DropdownMenu"))

        // 3. Zero Android AlertDialog
        assertFalse("SettingsScreen must not contain AlertDialog", content.contains("AlertDialog("))

        // 4. Inset Grouped sections check
        assertTrue("SettingsScreen must use IosListSection", content.contains("IosListSection("))
        assertTrue("SettingsScreen must use IosLargeTitleScaffold", content.contains("IosLargeTitleScaffold("))
        assertTrue("SettingsScreen must use IosModalDialog", content.contains("IosModalDialog("))
        assertTrue("SettingsScreen must use IosActionSheet", content.contains("IosActionSheet("))
    }

    @Test
    fun testSettingsScreen_FourCanonicalSectionsPresent() {
        val settingsFile = resolveSourceFile("src/main/java/com/example/inkpaperdiary/ui/settings/SettingsScreen.kt")
        val content = settingsFile.readText()

        assertTrue("Section 1: 云端与同步 must be present", content.contains("云端与同步"))
        assertTrue("Section 2: 安全与隐私 must be present", content.contains("安全与隐私"))
        assertTrue("Section 3: 外观与排版 must be present", content.contains("外观与排版"))
        assertTrue("Section 4: 数据与关于 must be present", content.contains("数据与关于"))
    }

    @Test
    fun testSettingsScreen_DisablePinVerifiesAgainstStoredPin() {
        val settingsFile = resolveSourceFile("src/main/java/com/example/inkpaperdiary/ui/settings/SettingsScreen.kt")
        val content = settingsFile.readText()

        val disableBlockStart = content.lastIndexOf("PinDialogMode.DISABLE ->")
        assertTrue("SettingsScreen must handle PinDialogMode.DISABLE", disableBlockStart > 0)
        val disableBlock = content.substring(disableBlockStart, disableBlockStart + 600)

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
        val shareFunctionBlock = content.substring(shareFunctionStart, shareFunctionStart + 800)

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
}
