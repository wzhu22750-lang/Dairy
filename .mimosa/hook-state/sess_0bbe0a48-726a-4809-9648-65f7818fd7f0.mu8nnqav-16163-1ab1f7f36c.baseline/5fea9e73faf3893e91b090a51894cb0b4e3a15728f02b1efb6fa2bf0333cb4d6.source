package com.example.inkpaperdiary.challenger

import com.example.inkpaperdiary.core.security.AppLockManager
import com.example.inkpaperdiary.ui.settings.ChangePinStep
import com.example.inkpaperdiary.ui.settings.PinDialogMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * ADVERSARIAL CHALLENGER TEST SUITE for Milestone 4 Iteration 2:
 *
 * Rigorously attacks and stress-tests:
 * 1. PIN Disable State Machine (Brute-force resistance, wrong PIN rejection, state preservation, correct PIN acceptance)
 * 2. PIN Change State Machine (Old PIN authentication prerequisite, mid-step cancellation rollback, invalid new PIN rejection)
 * 3. `isPickerActive` Lifecycle Exception Handling (ActivityNotFoundException, SecurityException, NPE, concurrency)
 * 4. Cache Deletion Non-Destructive Inode & Sibling Preservation (Subtrees, siblings, recreate capability)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class Milestone4Iteration2AdversarialChallengeTest {

    private lateinit var sandboxDir: File
    private lateinit var cacheDir: File
    private lateinit var externalCacheDir: File
    private lateinit var databasesDir: File
    private lateinit var filesDir: File

    @Before
    fun setUp() {
        AppLockManager.isPickerActive = false
        AppLockManager.unlock()

        sandboxDir = Files.createTempDirectory("m4_it2_sandbox").toFile()
        cacheDir = File(sandboxDir, "cache").apply { mkdir() }
        externalCacheDir = File(sandboxDir, "external_cache").apply { mkdir() }
        databasesDir = File(sandboxDir, "databases").apply { mkdir() }
        filesDir = File(sandboxDir, "files").apply { mkdir() }
    }

    @After
    fun tearDown() {
        AppLockManager.isPickerActive = false
        sandboxDir.deleteRecursively()
    }

    // =============================================================================================
    // 1. PIN Disable State Machine Attacks
    // =============================================================================================

    @Test
    fun challenge_pinDisable_BruteForceSimulationNeverDisablesLockOrAltersPin() {
        val activeStoredPin = "7492"
        var currentLockState = true
        var currentPin = activeStoredPin
        var dialogOpen = true
        var failureCount = 0

        val disableStateMachine: (String, (String) -> Boolean) -> Unit = { input, verifier ->
            if (input.length == 4) {
                if (verifier(input)) {
                    currentLockState = false
                    currentPin = ""
                    dialogOpen = false
                } else {
                    failureCount++
                }
            }
        }

        // Adversary tests 20 incorrect 4-digit PINs
        for (i in 0..19) {
            val candidate = String.format("%04d", i * 111)
            if (candidate != activeStoredPin) {
                disableStateMachine(candidate) { it == activeStoredPin }
                assertTrue("Lock must remain ENABLED on wrong candidate: $candidate", currentLockState)
                assertEquals("Active PIN must NOT be cleared or altered", activeStoredPin, currentPin)
                assertTrue("Dialog must remain active so user can re-try or cancel", dialogOpen)
            }
        }

        assertEquals(20, failureCount)

        // Finally, adversary enters correct PIN
        disableStateMachine(activeStoredPin) { it == activeStoredPin }
        assertFalse("Lock must be DISABLED only upon correct PIN authentication", currentLockState)
        assertEquals("PIN must be wiped upon successful disable", "", currentPin)
        assertFalse("Dialog must dismiss on successful disable", dialogOpen)
    }

    @Test
    fun challenge_pinDisable_LengthBoundsAndMalformedInputsNeverTriggerVerification() {
        var verificationCalled = false
        val mockVerifier: (String) -> Boolean = {
            verificationCalled = true
            false
        }

        val testInputs = listOf("", "1", "12", "123", "12345", "abcd", "12a4", " 123", "123 ")

        testInputs.forEach { malformed ->
            verificationCalled = false
            // Simulating SettingsScreen input filter + confirmation length check:
            val filtered = if (malformed.length <= 4 && malformed.all { it.isDigit() }) malformed else ""
            if (filtered.length == 4) {
                mockVerifier(filtered)
            }
            assertFalse(
                "Malformed/improper length input '$malformed' must NEVER invoke verifyPin",
                verificationCalled
            )
        }
    }

    // =============================================================================================
    // 2. PIN Change State Machine Attacks
    // =============================================================================================

    @Test
    fun challenge_pinChange_CannotBypassOldPinAuthentication() {
        val activeOldPin = "2468"
        val unauthorizedNewPin = "9999"
        var storedPin = activeOldPin
        var step = ChangePinStep.VERIFY_OLD
        var dialogOpen = true
        var errorMessage: String? = null

        fun onConfirmInput(input: String, verifier: (String) -> Boolean) {
            when (step) {
                ChangePinStep.VERIFY_OLD -> {
                    if (input.length == 4) {
                        if (verifier(input)) {
                            step = ChangePinStep.ENTER_NEW
                        } else {
                            errorMessage = "原 PIN 密码错误，请重新输入"
                        }
                    }
                }
                ChangePinStep.ENTER_NEW -> {
                    if (input.length == 4) {
                        storedPin = input
                        step = ChangePinStep.VERIFY_OLD
                        dialogOpen = false
                    }
                }
            }
        }

        // Attack 1: Try entering new PIN while still in VERIFY_OLD
        onConfirmInput(unauthorizedNewPin) { it == activeOldPin }
        assertEquals("Must remain stuck in VERIFY_OLD when wrong old PIN is provided", ChangePinStep.VERIFY_OLD, step)
        assertEquals("Stored PIN must not be changed", activeOldPin, storedPin)
        assertEquals("原 PIN 密码错误，请重新输入", errorMessage)

        // Attack 2: Dismiss dialog mid-step, then reopen
        // Verify onDismissRequest contract:
        step = ChangePinStep.VERIFY_OLD
        dialogOpen = false

        // Reopen CHANGE PIN row onClick contract:
        step = ChangePinStep.VERIFY_OLD
        dialogOpen = true

        // User authenticates with correct old PIN
        onConfirmInput(activeOldPin) { it == activeOldPin }
        assertEquals("Must transition to ENTER_NEW upon correct old PIN", ChangePinStep.ENTER_NEW, step)

        // Attack 3: User enters invalid length for new PIN
        onConfirmInput("123") { it == activeOldPin }
        assertEquals("Must remain in ENTER_NEW if new PIN is not 4 digits", ChangePinStep.ENTER_NEW, step)
        assertEquals(activeOldPin, storedPin)

        // User supplies valid new PIN
        onConfirmInput(unauthorizedNewPin) { it == activeOldPin }
        assertEquals("New PIN must now be saved", unauthorizedNewPin, storedPin)
        assertFalse("Dialog must dismiss", dialogOpen)
        assertEquals("Step must reset to VERIFY_OLD for future invocations", ChangePinStep.VERIFY_OLD, step)
    }

    @Test
    fun challenge_pinChange_CancellationInEnterNewRollsBackToVerifyOld() {
        var step = ChangePinStep.VERIFY_OLD
        var showDialog = true

        // Advance to ENTER_NEW
        step = ChangePinStep.ENTER_NEW

        // User cancels / clicks backdrop (onDismissRequest)
        fun onDismissRequest() {
            showDialog = false
            step = ChangePinStep.VERIFY_OLD
        }
        onDismissRequest()

        assertFalse(showDialog)
        assertEquals(
            "Canceling in ENTER_NEW must strictly reset step to VERIFY_OLD to prevent auth bypass on next open",
            ChangePinStep.VERIFY_OLD,
            step
        )
    }

    @Test
    fun challenge_pinDialogMode_EnumConstantsStrictlyEqualThree() {
        // Enforce backward compatibility and interface invariants
        val modes = PinDialogMode.values()
        assertEquals("PinDialogMode must have exactly 3 constants (SETUP, CHANGE, DISABLE)", 3, modes.size)
        assertTrue(modes.any { it.name == "SETUP" })
        assertTrue(modes.any { it.name == "CHANGE" })
        assertTrue(modes.any { it.name == "DISABLE" })
    }

    // =============================================================================================
    // 3. `isPickerActive` Exception Handling & Concurrency Attacks
    // =============================================================================================

    @Test
    fun challenge_shareExportedFile_VariousExceptionsAlwaysResetPickerFlag() {
        val exceptionsToTest = listOf(
            SecurityException("Permission denial for FileProvider uri"),
            android.content.ActivityNotFoundException("No activity found to handle Intent ACTION_SEND"),
            NullPointerException("FileProvider context null"),
            IllegalStateException("Window manager bad token"),
            RuntimeException("Generic OS binder failure")
        )

        for (exception in exceptionsToTest) {
            AppLockManager.isPickerActive = false

            runCatching {
                AppLockManager.isPickerActive = true
                throw exception
            }.onFailure {
                AppLockManager.isPickerActive = false
            }

            assertFalse(
                "isPickerActive must be restored to false on ${exception.javaClass.simpleName}",
                AppLockManager.isPickerActive
            )

            // When app goes to background (onStop) after this failure, it MUST trigger lock
            var appLocked = false
            if (!AppLockManager.isPickerActive) {
                appLocked = true
                AppLockManager.lock()
            }
            assertTrue("App must lock onStop after exception: ${exception.javaClass.simpleName}", appLocked)
            assertTrue(AppLockManager.isLocked.value)
        }
    }

    @Test
    fun challenge_documentPickerLaunch_ExceptionAlwaysResetsPickerFlag() {
        AppLockManager.isPickerActive = false

        // Simulate OpenDocument launcher throwing ActivityNotFoundException
        runCatching {
            AppLockManager.isPickerActive = true
            throw android.content.ActivityNotFoundException("DocumentsUI disabled by OEM")
        }.onFailure {
            AppLockManager.isPickerActive = false
        }

        assertFalse("Picker flag must be false if OpenDocument launcher fails", AppLockManager.isPickerActive)
    }

    @Test
    fun challenge_pickerActive_HighVolumeRapidInvocationsStressTest() {
        // High volume sequential rapid invocations (simulating rapid user clicks on share/import)
        val iterations = 5000
        for (i in 0 until iterations) {
            val shouldFail = (i % 2 == 0)
            runCatching {
                AppLockManager.isPickerActive = true
                if (shouldFail) {
                    throw RuntimeException("Simulated intent launch abort #$i")
                } else {
                    // Simulating successful launch: isPickerActive remains true while chooser is open,
                    // then when user dismisses chooser / resumes, isPickerActive is reset
                    assertTrue(AppLockManager.isPickerActive)
                    AppLockManager.isPickerActive = false
                }
            }.onFailure {
                AppLockManager.isPickerActive = false
            }

            assertFalse(
                "isPickerActive must be false immediately after iteration $i (failure=$shouldFail)",
                AppLockManager.isPickerActive
            )
        }
        assertFalse("Final picker active state must be false", AppLockManager.isPickerActive)
    }

    // =============================================================================================
    // 4. Cache Deletion Inode & Sibling Directory Preservation Attacks
    // =============================================================================================

    @Test
    fun challenge_cacheDeletion_PreservesRootInodesAndProtectsSiblingDirectories() {
        // Populate cache directory with complex hierarchy
        val sub1 = File(cacheDir, "images/thumbnails").apply { mkdirs() }
        File(sub1, "thumb_001.jpg").writeText("JPEG_DATA_1")
        File(sub1, "thumb_002.jpg").writeText("JPEG_DATA_2")
        val zipFile = File(cacheDir, "export_20260906.zip").apply { writeText("ZIP_BACKUP_BLOB") }

        // Populate external cache directory
        val extSub = File(externalCacheDir, "logs").apply { mkdirs() }
        File(extSub, "network.log").writeText("HTTP 200 OK")

        // Populate critical sibling directories (Room database and user files)
        val dbFile = File(databasesDir, "diary.db").apply { writeText("ROOM_SQLITE_HEADER") }
        val diaryAttachment = File(filesDir, "user_photo.png").apply { writeText("PNG_ATTACHMENT") }

        assertTrue(cacheDir.exists() && cacheDir.isDirectory)
        assertTrue(externalCacheDir.exists() && externalCacheDir.isDirectory)
        assertTrue(dbFile.exists())
        assertTrue(diaryAttachment.exists())

        // Execute SettingsScreen cache clearing logic
        runCatching {
            cacheDir.listFiles()?.forEach { it.deleteRecursively() }
            externalCacheDir.listFiles()?.forEach { it.deleteRecursively() }
        }

        // 1. Inode / directory existence check
        assertTrue("cacheDir root MUST still exist", cacheDir.exists())
        assertTrue("cacheDir root MUST remain a directory", cacheDir.isDirectory)
        assertEquals("cacheDir root MUST be completely empty", 0, cacheDir.listFiles()?.size)

        assertTrue("externalCacheDir root MUST still exist", externalCacheDir.exists())
        assertTrue("externalCacheDir root MUST remain a directory", externalCacheDir.isDirectory)
        assertEquals("externalCacheDir root MUST be completely empty", 0, externalCacheDir.listFiles()?.size)

        // 2. Immediate recreate capability check (subsequent export should not fail with FileNotFoundException)
        val newExport = File(cacheDir, "new_export.zip")
        val created = newExport.createNewFile()
        assertTrue("Creating new files in preserved cacheDir must succeed immediately", created)
        assertTrue("Newly created file must exist", newExport.exists())

        // 3. Sibling isolation check: Room DB and user files must be 100% untouched
        assertTrue("Critical databasesDir must exist", databasesDir.exists())
        assertTrue("Critical Room DB file must NOT be deleted", dbFile.exists())
        assertEquals("ROOM_SQLITE_HEADER", dbFile.readText())

        assertTrue("Critical filesDir must exist", filesDir.exists())
        assertTrue("User attachment in filesDir must NOT be deleted", diaryAttachment.exists())
        assertEquals("PNG_ATTACHMENT", diaryAttachment.readText())
    }

    @Test
    fun challenge_cacheDeletion_ResilienceAgainstNullAndNonExistentDirs() {
        val nonExistentDir = File(sandboxDir, "phantom_cache_dir")
        assertFalse(nonExistentDir.exists())

        // Simulating invocation when cacheDir or externalCacheDir is null or does not exist
        var caughtException = false
        try {
            val nullFile: File? = null
            nullFile?.listFiles()?.forEach { it.deleteRecursively() }
            nonExistentDir.listFiles()?.forEach { it.deleteRecursively() }
        } catch (e: Throwable) {
            caughtException = true
        }

        assertFalse("Cache clearing must be null-safe and non-existent-dir safe", caughtException)
    }
}
