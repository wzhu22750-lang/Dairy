# Handoff Report: IosDateTimePickerSheet Design & EditorScreen Integration Specification

## 1. Observation

### 1.1 Existing Date and Time Picker Implementation
- **File**: `app/src/main/java/com/example/inkpaperdiary/ui/editor/EditorScreen.kt`
- **Imports (Lines 3-4)**:
  ```kotlin
  import android.app.DatePickerDialog
  import android.app.TimePickerDialog
  ```
- **Trigger Logic (Lines 84-112)**:
  ```kotlin
  val calendar = remember(uiState.entryDate) {
      Calendar.getInstance().apply { timeInMillis = uiState.entryDate }
  }

  fun pickDateTime() {
      DatePickerDialog(
          context,
          { _, year, month, dayOfMonth ->
              calendar.set(Calendar.YEAR, year)
              calendar.set(Calendar.MONTH, month)
              calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
              TimePickerDialog(
                  context,
                  { _, hourOfDay, minute ->
                      calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                      calendar.set(Calendar.MINUTE, minute)
                      viewModel.updateEntryDate(calendar.timeInMillis)
                  },
                  calendar.get(Calendar.HOUR_OF_DAY),
                  calendar.get(Calendar.MINUTE),
                  true
              ).show()
          },
          calendar.get(Calendar.YEAR),
          calendar.get(Calendar.MONTH),
          calendar.get(Calendar.DAY_OF_MONTH)
      ).show()
  }
  ```
- **TopAppBar Trigger (Lines 159-184)**:
  ```kotlin
  Surface(
      modifier = Modifier.iosClick { pickDateTime() },
      shape = CapsuleShape,
      border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
      color = MaterialTheme.colorScheme.surfaceVariant
  ) { ... }
  ```
- **TopAppBar Navigation & Actions (Lines 186-229)**:
  Currently, `navigationIcon` uses `Icons.AutoMirrored.Filled.ArrowBack` instead of the iOS text button "取消", while trailing actions include `PushPin` and "完成".
- **Missing File**:
  `app/src/main/java/com/example/inkpaperdiary/core/designsystem/components/IosDatePicker.kt` does not exist in `core/designsystem/components/`.

### 1.2 Existing Design System Components & Primitives
- **`IosActionSheet.kt` (`core/designsystem/components/IosActionSheet.kt:71-76`)**:
  ```kotlin
  ModalBottomSheet(
      onDismissRequest = onDismissRequest,
      containerColor = Color.Transparent,
      dragHandle = null,
      scrimColor = Color.Black.copy(alpha = 0.4f)
  )
  ```
- **`AppleMaterial.kt` (`core/designsystem/AppleMaterial.kt:63-65, 99-119`)**:
  - `AppleMaterials.backgroundColor(MaterialThickness.ULTRA_THICK)`: `0xFA121214` (dark) / `0xFDFFFFFF` (light).
  - `AppleMaterials.glassBorder(width = 0.5.dp)`: 0.5dp vertical gradient hairline border.
  - `AppleMaterials.separatorColor(isDark)`: `0x2EFFFFFF` (dark) / `0x1F000000` (light).
- **`IosSegmentedControl.kt` (`core/designsystem/components/IosSegmentedControl.kt:43-74`)**:
  32dp height sliding pill segmented control with spring dynamics (`Spring.DampingRatioNoBouncy`).
- **`IosTouchPhysics.kt` (`core/designsystem/interaction/IosTouchPhysics.kt:147-189`)**:
  `Modifier.iosClick` and `Modifier.iosIconClick` with tactile haptics (`TextHandleMove`), spring scale-down, and ripple elimination.

### 1.3 Test Suite Contracts & Boundary Constraints
- **`R3ScreenLayoutFeatureTest.kt:190-198` (Feature 12: Editor Navigation)**:
  ```kotlin
  @Test
  fun testF12_EditorNavigationBarActions() {
      val leadingAction = "取消"
      val trailingAction = "完成"
      val centerAction = "DATE_TIME_PILL"
      assertEquals("取消", leadingAction)
      assertEquals("完成", trailingAction)
      assertNotNull(centerAction)
  }
  ```
- **`R3ScreenLayoutFeatureTest.kt:238-285` (Feature 13: iOS Date/Time Picker Sheet)**:
  ```kotlin
  @Test
  fun testF13_DateTimePickerModalStructure() {
      val headerActions = listOf("取消", "选择时间", "完成")
      assertEquals(3, headerActions.size)
      assertEquals("取消", headerActions[0])
      assertEquals("选择时间", headerActions[1])
      assertEquals("完成", headerActions[2])
  }

  @Test
  fun testF13_DateClampingToPositiveEpoch() {
      val epochNow = System.currentTimeMillis()
      val inputEpoch = -1000L
      val clampedEpoch = if (inputEpoch < 0) epochNow else inputEpoch
      assertTrue(clampedEpoch > 0)
  }

  @Test
  fun testF13_DateSelectionTimestampIntegrity() {
      val testDate = 1700000000000L
      var selectedDate = testDate
      val newDate = 1700086400000L // 1 day later
      selectedDate = newDate
      assertEquals(1700086400000L, selectedDate)
      assertEquals(86400000L, selectedDate - testDate)
  }

  @Test
  fun testF13_DatePickerDismissContract() {
      var isVisible = true
      fun onDismiss() { isVisible = false }
      onDismiss()
      assertEquals(false, isVisible)
  }

  @Test
  fun testF13_DatePickerConfirmContract() {
      var committedTimestamp = 0L
      fun onConfirm(timestamp: Long) { committedTimestamp = timestamp }
      onConfirm(1720000000000L)
      assertEquals(1720000000000L, committedTimestamp)
  }
  ```
- **`R3BoundaryEdgeCasesTest.kt:80-95` (Edge cases: Leap year & Epoch 0)**:
  - `testB3_DatePickerLeapYearFeb29`: Leap year `2024-02-29` handling.
  - `testB3_DatePickerUnixEpochZero`: Unix Epoch `0L` (`1970-01-01`) formatting.
- **`SettingsScreenAndModalSheetsEmpiricalChallengeTest.kt:335-337`**:
  Strict AST purge rule checking zero occurrence of `DatePickerDialog` and `TimePickerDialog`.

---

## 2. Logic Chain

1. **Elimination of Android Dialog Idioms**:
   - Observations 1.1 and 1.3 establish that `DatePickerDialog` and `TimePickerDialog` are legacy Android platform dialogs that break Apple HIG uniformity and fail the static AST purge audits.
   - Therefore, `DatePickerDialog` and `TimePickerDialog` must be completely excised from `EditorScreen.kt`.

2. **Modal Sheet Container & HIG Styling**:
   - Per Observation 1.2, Compose M3's `ModalBottomSheet` with `containerColor = Color.Transparent`, `dragHandle = null`, and `scrimColor = Color.Black.copy(alpha = 0.4f)` provides the required container.
   - Enclosing the sheet in a `Surface` with `RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)`, `AppleMaterials.backgroundColor(MaterialThickness.ULTRA_THICK)`, and `AppleMaterials.glassBorder(0.5.dp)` fulfills the visual HIG requirement for frosted translucent materials and hairline borders.

3. **Header Contract & Structure**:
   - Per Observation 1.3 (`testF13_DateTimePickerModalStructure`), the header row must strictly offer `["取消", "选择时间", "完成"]`:
     - Leading text action: "取消" (Cancel) calling `onDismissRequest()`.
     - Center title: "选择时间" (17sp SemiBold).
     - Trailing text action: "完成" (Done) emitting the selected timestamp to `onDateTimeSelected(timestamp)` and calling `onDismissRequest()`.
     - 0.5dp hairline divider below the header using `AppleMaterials.separatorColor()`.

4. **Authentic Two-Tier Selection (Date & Time)**:
   - To deliver both month calendar navigation and hours/minutes selection within a compact modal bottom sheet without cramped clutter:
     - Use `IosSegmentedControl` (`["日期", "时间"]`) below the header.
     - Add a real-time live readout banner (e.g. `2026年9月6日 星期日 20:25`) in a capsule shape so the user receives continuous feedback.
     - **Date View (Tab 0)**: 7-column month grid (`LazyVerticalGrid(GridCells.Fixed(7))`), `<` and `>` month paging, "今天" (Today) quick-jump button, and circular selection highlights. Leap year Feb 29 and Epoch 0L are correctly calculated via `Calendar.getActualMaximum(Calendar.DAY_OF_MONTH)`.
     - **Time View (Tab 1)**: Authentic 2-column Cupertino Wheel Picker (Hours 00..23, Minutes 00..59) utilizing `LazyColumn` with `rememberSnapFlingBehavior`, central translucent highlight box (40dp height, 8dp corner radius), top/bottom 3D drum gradient masks, and haptic feedback (`TextHandleMove`).
     - Quick time preset pills ("现在", "整点", "+15分", "+30分") ensure rapid adjustments.

5. **EditorScreen Integration**:
   - In `EditorScreen.kt`:
     - Maintain state: `var showDateTimePickerSheet by remember { mutableStateOf(false) }`.
     - DateTime capsule pill in `TopAppBar` triggers `showDateTimePickerSheet = true`.
     - Update `TopAppBar` leading action to "取消" and trailing action to "完成" per `testF12_EditorNavigationBarActions`.
     - Render `IosDateTimePickerSheet` at the bottom of the screen bound to `uiState.entryDate` and `viewModel.updateEntryDate(timestamp)`.

---

## 3. Caveats

- **No Caveats**: All designsystem primitives (`AppleMaterials`, `IosSegmentedControl`, `iosClick`, `iosIconClick`) are already implemented, tested, and available in the workspace.
- **Backwards Compatibility**: Both `IosDateTimePickerSheet` and `IosDatePickerSheet` aliases are provided in `proposed_IosDateTimePickerSheet.kt` to prevent naming mismatch across Milestones.
- **External AppLock Interception**: Unlike `PickVisualMedia` which launches an external activity and sets `AppLockManager.isPickerActive = true`, `IosDateTimePickerSheet` is an in-process Composable modal sheet, so no lifecycle flags on `AppLockManager` are required.

---

## 4. Conclusion

The specification and blueprints for `IosDateTimePickerSheet.kt` and its integration with `EditorScreen.kt` are finalized and ready for the implementer agent:
1. **Component Blueprint**: Stored at `.agents/explorer_m5_2/proposed_IosDateTimePickerSheet.kt`.
   - Target destination: `app/src/main/java/com/example/inkpaperdiary/core/designsystem/components/IosDateTimePickerSheet.kt`.
2. **EditorScreen Integration Patch**: Stored at `.agents/explorer_m5_2/proposed_EditorScreen.patch`.
   - Target file: `app/src/main/java/com/example/inkpaperdiary/ui/editor/EditorScreen.kt`.
   - Replaces all usages of `DatePickerDialog` and `TimePickerDialog` with `IosDateTimePickerSheet`.
   - Aligns `TopAppBar` actions with the F12 contract (`取消`, `DATE_TIME_PILL`, `完成`).

---

## 5. Verification Method

### 5.1 Compilation & Test Verification
Run the following commands in project root:
```bash
# 1. Verify compilation and existing test suite passes
./gradlew test

# 2. Verify debug compilation
./gradlew assembleDebug

# 3. Verify specific feature and boundary test classes
./gradlew testDebugUnitTest --tests "com.example.inkpaperdiary.tier1_features.R3ScreenLayoutFeatureTest"
./gradlew testDebugUnitTest --tests "com.example.inkpaperdiary.tier2_boundaries.R3BoundaryEdgeCasesTest"
./gradlew testDebugUnitTest --tests "com.example.inkpaperdiary.tier3_combinations.CrossFeaturePairwiseTest"
```

### 5.2 Static AST Audit Verification
Ensure that after applying the patch, `EditorScreen.kt` contains zero references to `DatePickerDialog` or `TimePickerDialog`:
```bash
grep -n "DatePickerDialog" app/src/main/java/com/example/inkpaperdiary/ui/editor/EditorScreen.kt
grep -n "TimePickerDialog" app/src/main/java/com/example/inkpaperdiary/ui/editor/EditorScreen.kt
# (Both must return 0 results)
```

### 5.3 Invalidation Conditions
- Any introduction of third-party external UI libraries.
- Header actions not matching `["取消", "选择时间", "完成"]`.
- Negative timestamps not clamped or Epoch 0L crashing the calendar formatter.
- Failure of `./gradlew test` or `./gradlew assembleDebug`.
