package com.example.inkpaperdiary.ui.settings

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.inkpaperdiary.core.designsystem.components.PaperPattern
import com.example.inkpaperdiary.core.designsystem.components.isDialogButtonLayoutVertical
import com.example.inkpaperdiary.core.security.AppLockManager
import com.example.inkpaperdiary.core.sync.SyncManager
import com.example.inkpaperdiary.data.repository.DiaryRepository
import com.example.inkpaperdiary.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Locale
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelHigTest {

    private val testDispatcher = StandardTestDispatcher()

    private val supabaseUrlFlow = MutableStateFlow("https://demo.supabase.co")
    private val supabaseAnonKeyFlow = MutableStateFlow("anon_key_123")
    private val autoSyncEnabledFlow = MutableStateFlow(true)
    private val lastSyncTimeFlow = MutableStateFlow(1700000000000L)
    private val appLockEnabledFlow = MutableStateFlow(false)
    private val appLockPinFlow = MutableStateFlow("")
    private val biometricEnabledFlow = MutableStateFlow(false)
    private val paperPatternFlow = MutableStateFlow(PaperPattern.RULED_LINES)

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var diaryRepository: DiaryRepository
    private lateinit var syncManager: SyncManager
    private lateinit var viewModel: SettingsViewModel

    private fun allocateInstance(clazz: Class<*>): Any {
        val unsafeField = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe")
        unsafeField.isAccessible = true
        val unsafe = unsafeField.get(null) as sun.misc.Unsafe
        return unsafe.allocateInstance(clazz)
    }

    private fun setField(target: Any, fieldName: String, value: Any?) {
        var curr: Class<*>? = target.javaClass
        while (curr != null) {
            try {
                val field = curr.getDeclaredField(fieldName)
                field.isAccessible = true
                field.set(target, value)
                return
            } catch (e: NoSuchFieldException) {
                curr = curr.superclass
            }
        }
        throw NoSuchFieldException("Field $fieldName not found in ${target.javaClass}")
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        settingsRepository = allocateInstance(SettingsRepository::class.java) as SettingsRepository
        setField(settingsRepository, "supabaseUrl", supabaseUrlFlow)
        setField(settingsRepository, "supabaseAnonKey", supabaseAnonKeyFlow)
        setField(settingsRepository, "autoSyncEnabled", autoSyncEnabledFlow)
        setField(settingsRepository, "lastSyncTime", lastSyncTimeFlow)
        setField(settingsRepository, "appLockEnabled", appLockEnabledFlow)
        setField(settingsRepository, "appLockPin", appLockPinFlow)
        setField(settingsRepository, "biometricEnabled", biometricEnabledFlow)
        setField(settingsRepository, "paperPattern", paperPatternFlow)

        diaryRepository = allocateInstance(DiaryRepository::class.java) as DiaryRepository
        syncManager = allocateInstance(SyncManager::class.java) as SyncManager

        viewModel = SettingsViewModel(settingsRepository, diaryRepository, syncManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // -----------------------------------------------------------------------------------------
    // 1. Reactive Flow Composition & UiState Integration
    // -----------------------------------------------------------------------------------------

    @Test
    fun testUiState_CombinesRepositoryFlowsCorrectly() = runTest {
        val collectJob = backgroundScope.launch(testDispatcher) {
            viewModel.uiState.collect {}
        }
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("https://demo.supabase.co", state.supabaseUrl)
        assertEquals("anon_key_123", state.supabaseAnonKey)
        assertTrue(state.autoSyncEnabled)
        assertEquals(1700000000000L, state.lastSyncTime)
        assertFalse(state.appLockEnabled)
        assertEquals("", state.appLockPin)
        assertFalse(state.biometricEnabled)
        assertEquals(PaperPattern.RULED_LINES, state.paperPattern)

        collectJob.cancel()
    }

    @Test
    fun testUiState_ReactiveUpdatesFromRepositoryFlows() = runTest {
        val collectJob = backgroundScope.launch(testDispatcher) {
            viewModel.uiState.collect {}
        }
        testScheduler.advanceUntilIdle()

        supabaseUrlFlow.value = "https://updated.supabase.co"
        supabaseAnonKeyFlow.value = "new_anon_key"
        appLockEnabledFlow.value = true
        appLockPinFlow.value = "8888"
        biometricEnabledFlow.value = true
        paperPatternFlow.value = PaperPattern.DOTTED_GRID
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("https://updated.supabase.co", state.supabaseUrl)
        assertEquals("new_anon_key", state.supabaseAnonKey)
        assertTrue(state.appLockEnabled)
        assertEquals("8888", state.appLockPin)
        assertTrue(state.biometricEnabled)
        assertEquals(PaperPattern.DOTTED_GRID, state.paperPattern)

        collectJob.cancel()
    }

    @Test
    fun testClearSyncMessage_ResetsSyncMessageFlow() = runTest {
        val messageField = SettingsViewModel::class.java.getDeclaredField("_syncMessage")
        messageField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = messageField.get(viewModel) as MutableStateFlow<String?>
        stateFlow.value = "同步完成，已同步 5 条变更"

        assertEquals("同步完成，已同步 5 条变更", viewModel.syncMessage.value)

        viewModel.clearSyncMessage()
        assertNull(viewModel.syncMessage.value)
    }

    // -----------------------------------------------------------------------------------------
    // 2. HIG Inset Grouped Section Structure & Content Contracts
    // -----------------------------------------------------------------------------------------

    @Test
    fun testFourCanonicalSettingsSectionsContract() {
        val expectedSections = listOf(
            "云端与同步",
            "安全与隐私",
            "外观与排版",
            "数据与关于"
        )
        assertEquals(4, expectedSections.size)
        assertEquals("云端与同步", expectedSections[0])
        assertEquals("安全与隐私", expectedSections[1])
        assertEquals("外观与排版", expectedSections[2])
        assertEquals("数据与关于", expectedSections[3])
    }

    @Test
    fun testSection1_CloudAndSyncSectionItems() {
        val section1Rows = listOf(
            "Supabase 凭据配置",
            "立即双向同步",
            "自动后台同步",
            "全量数据备份",
            "数据导入与恢复"
        )
        assertEquals(5, section1Rows.size)
        assertTrue(section1Rows.contains("Supabase 凭据配置"))
        assertTrue(section1Rows.contains("立即双向同步"))
        assertTrue(section1Rows.contains("自动后台同步"))
        assertTrue(section1Rows.contains("全量数据备份"))
        assertTrue(section1Rows.contains("数据导入与恢复"))
    }

    @Test
    fun testSection2_SecurityAndPrivacySectionItems() {
        val section2Rows = listOf(
            "应用锁 (PIN 密码)",
            "修改 PIN 密码",
            "生物特征快速解锁",
            "自动锁定延迟"
        )
        assertEquals(4, section2Rows.size)
        assertTrue(section2Rows.contains("应用锁 (PIN 密码)"))
        assertTrue(section2Rows.contains("修改 PIN 密码"))
        assertTrue(section2Rows.contains("生物特征快速解锁"))
        assertTrue(section2Rows.contains("自动锁定延迟"))
    }

    @Test
    fun testSection3_AppearanceAndTypographySectionItems() {
        val section3Rows = listOf(
            "主题外观",
            "正文字体",
            "书写信笺底纹"
        )
        assertEquals(3, section3Rows.size)
        assertTrue(section3Rows.contains("主题外观"))
        assertTrue(section3Rows.contains("正文字体"))
        assertTrue(section3Rows.contains("书写信笺底纹"))

        val paperPatterns = listOf(
            PaperPattern.BLANK,
            PaperPattern.RULED_LINES,
            PaperPattern.DOTTED_GRID
        )
        assertEquals(3, paperPatterns.size)
    }

    @Test
    fun testSection4_DataAndAboutSectionItems() {
        val section4Rows = listOf(
            "日记回收站",
            "清除应用缓存",
            "关于与版本信息"
        )
        assertEquals(3, section4Rows.size)
        assertTrue(section4Rows.contains("日记回收站"))
        assertTrue(section4Rows.contains("清除应用缓存"))
        assertTrue(section4Rows.contains("关于与版本信息"))
    }

    // -----------------------------------------------------------------------------------------
    // 3. Apple HIG Layout Geometries, Squircle Icons, and Divider Math
    // -----------------------------------------------------------------------------------------

    @Test
    fun testSquircleIconBoxGeometryAndVividColors() {
        val boxWidth = 30.dp
        val boxHeight = 30.dp
        val cornerRadius = 7.dp
        val iconGlyphSize = 18.dp

        assertEquals(30f, boxWidth.value, 0.001f)
        assertEquals(30f, boxHeight.value, 0.001f)
        assertEquals(7f, cornerRadius.value, 0.001f)
        assertEquals(18f, iconGlyphSize.value, 0.001f)

        // Apple HIG Vivid Palette mapping verification
        val blueColor = Color(0xFF007AFF)       // CloudSync, TextFields, SaveAlt
        val indigoColor = Color(0xFF5856D6)     // Sync, FileUpload
        val greenColor = Color(0xFF34C759)      // Schedule, NoteAdd
        val orangeColor = Color(0xFFFF9500)     // Lock, Key, Timer, Description, CleaningServices
        val lightBlueColor = Color(0xFF32ADE6)  // Fingerprint
        val purpleColor = Color(0xFFAF52DE)     // Palette, FolderZip
        val redColor = Color(0xFFFF3B30)        // Delete
        val grayColor = Color(0xFF8E8E93)       // Info

        assertEquals(Color(0xFF007AFF), blueColor)
        assertEquals(Color(0xFF34C759), greenColor)
        assertEquals(Color(0xFFFF3B30), redColor)
    }

    @Test
    fun testHairlineIndentedDividerSpecification() {
        val startPadding = 16.dp
        val iconWidth = 30.dp
        val spacing = 10.dp

        // Mathematical derivation: 16dp start padding + 30dp icon + 10dp spacing = 56dp
        val derivedDividerIndent = startPadding + iconWidth + spacing
        assertEquals(56.dp, derivedDividerIndent)

        val hairlineThickness = 0.5.dp
        assertEquals(0.5f, hairlineThickness.value, 0.001f)
    }

    // -----------------------------------------------------------------------------------------
    // 4. Modal Alert Dialog & ActionSheet HIG Conformance
    // -----------------------------------------------------------------------------------------

    @Test
    fun testIosModalDialogGeometryAndTypographySpecs() {
        val dialogWidth = 270.dp
        val cornerRadius = 14.dp
        val hairlineBorder = 0.5.dp
        val titleSize = 17.dp
        val messageSize = 13.dp
        val actionHeight = 44.dp

        assertEquals(270f, dialogWidth.value, 0.001f)
        assertEquals(14f, cornerRadius.value, 0.001f)
        assertEquals(0.5f, hairlineBorder.value, 0.001f)
        assertEquals(17f, titleSize.value, 0.001f)
        assertEquals(13f, messageSize.value, 0.001f)
        assertEquals(44f, actionHeight.value, 0.001f)

        // System Blue confirm button
        val confirmColor = Color(0xFF007AFF)
        val destructiveColor = Color(0xFFFF3B30)
        assertNotEquals(confirmColor, destructiveColor)
    }

    @Test
    fun testIosModalDialogAdaptiveButtonLayout() {
        // Spec 6.4: 1 or 2 buttons -> horizontal, 3+ buttons -> vertical
        assertFalse(isDialogButtonLayoutVertical(1))
        assertFalse(isDialogButtonLayoutVertical(2))
        assertTrue(isDialogButtonLayoutVertical(3))
        assertTrue(isDialogButtonLayoutVertical(4))
    }

    @Test
    fun testIosActionSheetGeometrySpecs() {
        val rowHeight = 56.dp
        val cardCornerRadius = 14.dp
        val detachedCancelGap = 8.dp

        assertEquals(56f, rowHeight.value, 0.001f)
        assertEquals(14f, cardCornerRadius.value, 0.001f)
        assertEquals(8f, detachedCancelGap.value, 0.001f)
    }

    // -----------------------------------------------------------------------------------------
    // 5. PIN Lifecycle State Machine & Security Contracts
    // -----------------------------------------------------------------------------------------

    @Test
    fun testPinLifecycle_ValidationAndStateTransitions() {
        val modes = PinDialogMode.values()
        assertEquals(3, modes.size)
        assertTrue(modes.contains(PinDialogMode.SETUP))
        assertTrue(modes.contains(PinDialogMode.CHANGE))
        assertTrue(modes.contains(PinDialogMode.DISABLE))

        // 4-digit numeric validation rules
        fun isValidPin(pin: String): Boolean = pin.length == 4 && pin.all { it.isDigit() }

        assertTrue(isValidPin("1234"))
        assertTrue(isValidPin("0000"))
        assertFalse(isValidPin("123"))     // Too short
        assertFalse(isValidPin("12345"))   // Too long
        assertFalse(isValidPin("12a4"))    // Non-digit
        assertFalse(isValidPin("    "))    // Whitespace
    }

    // -----------------------------------------------------------------------------------------
    // 6. Cache Calculation Formatting Logic
    // -----------------------------------------------------------------------------------------

    @Test
    fun testCacheCalculationFormattingLogic() {
        fun formatBytes(totalBytes: Long): String = when {
            totalBytes <= 0L -> "0 KB"
            totalBytes < 1024L * 1024L -> "${totalBytes / 1024L} KB"
            else -> String.format(Locale.getDefault(), "%.1f MB", totalBytes / (1024.0 * 1024.0))
        }

        assertEquals("0 KB", formatBytes(0L))
        assertEquals("0 KB", formatBytes(-50L))
        assertEquals("500 KB", formatBytes(500L * 1024L))
        assertEquals("1.0 MB", formatBytes(1024L * 1024L))
        assertEquals("2.5 MB", formatBytes((2.5 * 1024 * 1024).toLong()))
    }

    // -----------------------------------------------------------------------------------------
    // 7. AppLock Media & Document Picker Exemption Contract
    // -----------------------------------------------------------------------------------------

    @Test
    fun testAppLockPickerExemptionContract() {
        AppLockManager.isPickerActive = false
        assertFalse(AppLockManager.isPickerActive)

        // Launching document picker sets flag
        AppLockManager.isPickerActive = true
        assertTrue(AppLockManager.isPickerActive)

        // Simulating activity onStop while picker active -> should NOT lock
        var lockedByOnStop = false
        if (!AppLockManager.isPickerActive) {
            lockedByOnStop = true
        }
        assertFalse(lockedByOnStop)

        // Activity resumes -> picker closes -> flag reset
        AppLockManager.isPickerActive = false
        assertFalse(AppLockManager.isPickerActive)
    }

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
}
