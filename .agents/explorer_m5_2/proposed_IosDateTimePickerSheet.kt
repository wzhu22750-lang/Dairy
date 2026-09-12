package com.example.inkpaperdiary.core.designsystem.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inkpaperdiary.core.designsystem.AppleMaterials
import com.example.inkpaperdiary.core.designsystem.CapsuleShape
import com.example.inkpaperdiary.core.designsystem.MaterialThickness
import com.example.inkpaperdiary.core.designsystem.PaperColors
import com.example.inkpaperdiary.core.designsystem.SansFontFamily
import com.example.inkpaperdiary.core.designsystem.interaction.iosClick
import com.example.inkpaperdiary.core.designsystem.interaction.iosIconClick
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Apple HIG Modal Date and Time Picker Sheet (UIDatePicker sheet presentation)
 *
 * Distinct iOS HIG characteristics:
 * 1. Modal bottom sheet with 16dp rounded top corners, ultra-thick frosted translucent background,
 *    and 0.5dp specular hairline border (AppleMaterials.glassBorder).
 * 2. 50dp navigation header with "取消" (Cancel) on left, "选择时间" title in center,
 *    and "完成" (Done) text action on right, separated by a 0.5dp hairline divider.
 * 3. Real-time selection readout badge and iOS Segmented Control (["日期", "时间"]) for tabbed switching.
 * 4. Authentic month calendar grid with previous/next navigation, "今天" (Today) quick-jump,
 *    and leap year (Feb 29) + Unix Epoch 0 compliance.
 * 5. Authentic Cupertino 2-column Wheel Picker (Hour 00..23, Minute 00..59) with snapping,
 *    central rounded highlight bar, 3D drum cylinder gradient masks, and haptic feedback.
 * 6. Clean callback contract: onDateTimeSelected(timestamp: Long) and onDismissRequest().
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IosDateTimePickerSheet(
    visible: Boolean,
    initialTimestamp: Long = System.currentTimeMillis(),
    title: String = "选择时间",
    cancelText: String = "取消",
    confirmText: String = "完成",
    onDateTimeSelected: (Long) -> Unit,
    onDismissRequest: () -> Unit
) {
    if (!visible) return

    // Clamp negative epoch timestamps to current time; preserve epoch 0L (1970-01-01)
    val validInitialTimestamp = if (initialTimestamp < 0L) System.currentTimeMillis() else initialTimestamp

    // Internal staged calendar state holding changes until "完成" is confirmed
    var stagedCalendar by remember(validInitialTimestamp) {
        mutableStateOf(
            Calendar.getInstance().apply { timeInMillis = validInitialTimestamp }
        )
    }

    // Display month for calendar navigation (separate from selected date to allow browsing)
    var displayMonthCalendar by remember(validInitialTimestamp) {
        mutableStateOf(
            (stagedCalendar.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
        )
    }

    // Tab index: 0 = "日期", 1 = "时间"
    var selectedTab by remember { mutableIntStateOf(0) }

    val isDark = isSystemInDarkTheme()
    val sheetBgColor = AppleMaterials.backgroundColor(MaterialThickness.ULTRA_THICK)
    val dividerColor = AppleMaterials.separatorColor(isDark)

    // Formatter for live readout banner
    val fullReadoutFormatter = remember {
        SimpleDateFormat("yyyy年M月d日 EEEE  HH:mm", Locale.CHINESE)
    }
    val liveReadoutText = remember(stagedCalendar.timeInMillis) {
        fullReadoutFormatter.format(Date(stagedCalendar.timeInMillis))
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = Color.Transparent,
        dragHandle = null,
        scrimColor = Color.Black.copy(alpha = 0.4f)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            color = sheetBgColor,
            border = AppleMaterials.glassBorder(width = 0.5.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                // 1. Navigation Header Bar (50dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: "取消" (Cancel)
                    Box(
                        modifier = Modifier
                            .iosClick { onDismissRequest() }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = cancelText,
                            fontSize = 16.sp,
                            fontFamily = SansFontFamily,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Center: "选择时间" (Title)
                    Text(
                        text = title,
                        fontSize = 17.sp,
                        fontFamily = SansFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    // Right: "完成" (Done)
                    Box(
                        modifier = Modifier
                            .iosClick {
                                onDateTimeSelected(stagedCalendar.timeInMillis)
                                onDismissRequest()
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Text(
                            text = confirmText,
                            fontSize = 16.sp,
                            fontFamily = SansFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // 0.5dp Hairline Divider
                HorizontalDivider(thickness = 0.5.dp, color = dividerColor)

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Real-Time Date & Time Readout Capsule
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CapsuleShape,
                        color = AppleMaterials.backgroundColor(MaterialThickness.THIN),
                        border = BorderStroke(0.5.dp, dividerColor)
                    ) {
                        Text(
                            text = liveReadoutText,
                            fontSize = 13.sp,
                            fontFamily = SansFontFamily,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Segmented Control Switcher (["日期", "时间"])
                IosSegmentedControl(
                    items = listOf("日期", "时间"),
                    selectedIndex = selectedTab,
                    onSelectedIndexChange = { selectedTab = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 4. Tab Content: Date Grid or Cupertino Time Wheel
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "DatePickerTabTransition"
                ) { tab ->
                    if (tab == 0) {
                        IosCalendarPickerView(
                            stagedCalendar = stagedCalendar,
                            displayMonthCalendar = displayMonthCalendar,
                            onDisplayMonthChanged = { displayMonthCalendar = it },
                            onDateSelected = { year, month, day ->
                                val updated = stagedCalendar.clone() as Calendar
                                updated.set(Calendar.YEAR, year)
                                updated.set(Calendar.MONTH, month)
                                updated.set(Calendar.DAY_OF_MONTH, day)
                                stagedCalendar = updated
                            }
                        )
                    } else {
                        IosTimeWheelPickerView(
                            stagedCalendar = stagedCalendar,
                            onTimeChanged = { hour, minute ->
                                val updated = stagedCalendar.clone() as Calendar
                                updated.set(Calendar.HOUR_OF_DAY, hour)
                                updated.set(Calendar.MINUTE, minute)
                                stagedCalendar = updated
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Calendar Month Grid View for Date Selection
 */
@Composable
private fun IosCalendarPickerView(
    stagedCalendar: Calendar,
    displayMonthCalendar: Calendar,
    onDisplayMonthChanged: (Calendar) -> Unit,
    onDateSelected: (year: Int, month: Int, day: Int) -> Unit
) {
    val monthTitleFormatter = remember { SimpleDateFormat("yyyy年 M月", Locale.CHINESE) }
    val monthTitle = remember(displayMonthCalendar.timeInMillis) {
        monthTitleFormatter.format(displayMonthCalendar.time)
    }

    val displayYear = displayMonthCalendar.get(Calendar.YEAR)
    val displayMonth = displayMonthCalendar.get(Calendar.MONTH)

    val selectedYear = stagedCalendar.get(Calendar.YEAR)
    val selectedMonth = stagedCalendar.get(Calendar.MONTH)
    val selectedDay = stagedCalendar.get(Calendar.DAY_OF_MONTH)

    val todayCalendar = remember { Calendar.getInstance() }
    val isTodayMonth = todayCalendar.get(Calendar.YEAR) == displayYear &&
            todayCalendar.get(Calendar.MONTH) == displayMonth
    val todayDay = todayCalendar.get(Calendar.DAY_OF_MONTH)

    val daysInMonth = displayMonthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = remember(displayMonthCalendar.timeInMillis) {
        val temp = displayMonthCalendar.clone() as Calendar
        temp.set(Calendar.DAY_OF_MONTH, 1)
        val day = temp.get(Calendar.DAY_OF_WEEK) // 1=Sunday, 2=Monday, ...
        (day + 5) % 7 // Convert to 0=Monday ... 6=Sunday
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Month Navigation Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous Month
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .iosIconClick {
                        val nextMonth = displayMonthCalendar.clone() as Calendar
                        nextMonth.add(Calendar.MONTH, -1)
                        onDisplayMonthChanged(nextMonth)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "上一月",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Month Label & "今天" Jump Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = monthTitle,
                    fontSize = 16.sp,
                    fontFamily = SansFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    modifier = Modifier.iosClick {
                        val now = Calendar.getInstance()
                        onDisplayMonthChanged((now.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) })
                        onDateSelected(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH))
                    },
                    shape = CapsuleShape,
                    color = AppleMaterials.backgroundColor(MaterialThickness.THIN),
                    border = BorderStroke(0.5.dp, AppleMaterials.separatorColor(isSystemInDarkTheme()))
                ) {
                    Text(
                        text = "今天",
                        fontSize = 11.sp,
                        fontFamily = SansFontFamily,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Next Month
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .iosIconClick {
                        val nextMonth = displayMonthCalendar.clone() as Calendar
                        nextMonth.add(Calendar.MONTH, 1)
                        onDisplayMonthChanged(nextMonth)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "下一月",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Weekday Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach { dayName ->
                Text(
                    text = dayName,
                    fontSize = 12.sp,
                    fontFamily = SansFontFamily,
                    fontWeight = FontWeight.Medium,
                    color = PaperColors.MonoGray500,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(36.dp)
                )
            }
        }

        // Calendar Month Grid (7 columns)
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            userScrollEnabled = false
        ) {
            // Empty placeholder cells before the 1st
            items(firstDayOfWeek) {
                Spacer(modifier = Modifier.size(36.dp))
            }

            // Days of the month
            items(daysInMonth) { index ->
                val dayNum = index + 1
                val isSelected = displayYear == selectedYear &&
                        displayMonth == selectedMonth &&
                        dayNum == selectedDay
                val isToday = isTodayMonth && dayNum == todayDay

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                        )
                        .iosClick(pressedScale = 0.90f) {
                            onDateSelected(displayYear, displayMonth, dayNum)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "$dayNum",
                            fontSize = 14.sp,
                            fontFamily = SansFontFamily,
                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                            color = when {
                                isSelected -> Color.White
                                isToday -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                        if (isToday && !isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(3.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Authentic Cupertino 2-Column Wheel Picker for Hours & Minutes
 */
@Composable
private fun IosTimeWheelPickerView(
    stagedCalendar: Calendar,
    onTimeChanged: (hour: Int, minute: Int) -> Unit
) {
    val initialHour = stagedCalendar.get(Calendar.HOUR_OF_DAY)
    val initialMinute = stagedCalendar.get(Calendar.MINUTE)

    var currentHour by remember(stagedCalendar.timeInMillis) { mutableIntStateOf(initialHour) }
    var currentMinute by remember(stagedCalendar.timeInMillis) { mutableIntStateOf(initialMinute) }

    val hours = remember { (0..23).map { String.format("%02d", it) } }
    val minutes = remember { (0..59).map { String.format("%02d", it) } }

    val isDark = isSystemInDarkTheme()
    val dividerColor = AppleMaterials.separatorColor(isDark)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Cupertino Wheel Container (200dp fixed height, 5 visible rows of 40dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            // Central Highlight Bar for Active Row
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(40.dp),
                shape = RoundedCornerShape(8.dp),
                color = AppleMaterials.backgroundColor(MaterialThickness.THIN),
                border = BorderStroke(0.5.dp, dividerColor)
            ) {}

            // Two Columns: [Hours] : [Minutes]
            Row(
                modifier = Modifier.fillMaxWidth(0.80f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hour Wheel Column
                IosWheelScrollColumn(
                    items = hours,
                    selectedIndex = currentHour,
                    onItemSelected = { hour ->
                        currentHour = hour
                        onTimeChanged(hour, currentMinute)
                    },
                    modifier = Modifier.weight(1f)
                )

                // Colon Separator
                Text(
                    text = ":",
                    fontSize = 24.sp,
                    fontFamily = SansFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                // Minute Wheel Column
                IosWheelScrollColumn(
                    items = minutes,
                    selectedIndex = currentMinute,
                    onItemSelected = { minute ->
                        currentMinute = minute
                        onTimeChanged(currentHour, minute)
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // Top Gradient Fade Mask (3D drum cylinder lens effect)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                AppleMaterials.backgroundColor(MaterialThickness.ULTRA_THICK),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Bottom Gradient Fade Mask (3D drum cylinder lens effect)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                AppleMaterials.backgroundColor(MaterialThickness.ULTRA_THICK)
                            )
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Preset Pills ("现在", "整点", "+15分", "+30分")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            QuickTimePresetButton(text = "现在") {
                val now = Calendar.getInstance()
                val h = now.get(Calendar.HOUR_OF_DAY)
                val m = now.get(Calendar.MINUTE)
                currentHour = h
                currentMinute = m
                onTimeChanged(h, m)
            }
            QuickTimePresetButton(text = "整点") {
                currentMinute = 0
                onTimeChanged(currentHour, 0)
            }
            QuickTimePresetButton(text = "+15分") {
                val updated = stagedCalendar.clone() as Calendar
                updated.add(Calendar.MINUTE, 15)
                val h = updated.get(Calendar.HOUR_OF_DAY)
                val m = updated.get(Calendar.MINUTE)
                currentHour = h
                currentMinute = m
                onTimeChanged(h, m)
            }
            QuickTimePresetButton(text = "+30分") {
                val updated = stagedCalendar.clone() as Calendar
                updated.add(Calendar.MINUTE, 30)
                val h = updated.get(Calendar.HOUR_OF_DAY)
                val m = updated.get(Calendar.MINUTE)
                currentHour = h
                currentMinute = m
                onTimeChanged(h, m)
            }
        }
    }
}

/**
 * Single Column Wheel with snap-to-item behavior and Apple tactile feedback
 */
@Composable
private fun IosWheelScrollColumn(
    items: List<String>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 40.dp
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = selectedIndex.coerceIn(0, items.lastIndex)
    )
    val coroutineScope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    // Detect settle position when scrolling stops
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val centerIndex = listState.firstVisibleItemIndex.coerceIn(0, items.lastIndex)
            if (centerIndex != selectedIndex) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onItemSelected(centerIndex)
            }
        }
    }

    // Programmatically scroll when selectedIndex changes externally
    LaunchedEffect(selectedIndex) {
        if (listState.firstVisibleItemIndex != selectedIndex && !listState.isScrollInProgress) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    LazyColumn(
        state = listState,
        flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
        modifier = modifier.height(itemHeight * 5),
        contentPadding = PaddingValues(vertical = itemHeight * 2), // 2 rows of padding top and bottom
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(items.size) { index ->
            val isCurrent = index == listState.firstVisibleItemIndex
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .iosClick(pressedScale = 0.96f) {
                        coroutineScope.launch {
                            listState.animateScrollToItem(index)
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onItemSelected(index)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = items[index],
                    fontSize = if (isCurrent) 22.sp else 17.sp,
                    fontFamily = SansFontFamily,
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isCurrent) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        PaperColors.MonoGray500.copy(alpha = 0.6f)
                    }
                )
            }
        }
    }
}

/**
 * Compact iOS Quick Time Preset Pill Button
 */
@Composable
private fun QuickTimePresetButton(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.iosClick(onClick = onClick),
        shape = CapsuleShape,
        color = AppleMaterials.backgroundColor(MaterialThickness.THIN),
        border = BorderStroke(0.5.dp, AppleMaterials.separatorColor(isSystemInDarkTheme()))
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontFamily = SansFontFamily,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
        )
    }
}

/**
 * Alias for backward compatibility with earlier design mentions of IosDatePicker
 */
@Composable
fun IosDatePickerSheet(
    visible: Boolean,
    initialTimestamp: Long = System.currentTimeMillis(),
    title: String = "选择时间",
    cancelText: String = "取消",
    confirmText: String = "完成",
    onDateTimeSelected: (Long) -> Unit,
    onDismissRequest: () -> Unit
) {
    IosDateTimePickerSheet(
        visible = visible,
        initialTimestamp = initialTimestamp,
        title = title,
        cancelText = cancelText,
        confirmText = confirmText,
        onDateTimeSelected = onDateTimeSelected,
        onDismissRequest = onDismissRequest
    )
}
