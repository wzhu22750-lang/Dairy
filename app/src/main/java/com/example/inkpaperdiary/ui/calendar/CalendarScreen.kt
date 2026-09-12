package com.example.inkpaperdiary.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inkpaperdiary.core.designsystem.AppleMaterials
import com.example.inkpaperdiary.core.designsystem.CapsuleShape
import com.example.inkpaperdiary.core.designsystem.SansFontFamily
import com.example.inkpaperdiary.core.designsystem.components.MoodIcon
import com.example.inkpaperdiary.core.designsystem.components.PaperCard
import com.example.inkpaperdiary.core.designsystem.components.StampBadge
import com.example.inkpaperdiary.core.designsystem.interaction.SuppressMaterialRipples
import com.example.inkpaperdiary.core.designsystem.interaction.iosClick
import com.example.inkpaperdiary.core.designsystem.interaction.iosIconClick
import com.example.inkpaperdiary.core.designsystem.scaffold.IosLargeTitleDefaults
import com.example.inkpaperdiary.core.designsystem.scaffold.IosNavBackButton
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToEditor: (String?, Long?) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = isSystemInDarkTheme()
    val monthTitleFormatter = remember { SimpleDateFormat("yyyy年 M月", Locale.CHINESE) }
    val dayTitleFormatter = remember { SimpleDateFormat("M月d日 EEEE", Locale.CHINESE) }

    val monthTitle = remember(uiState.currentMonth) { monthTitleFormatter.format(uiState.currentMonth.time) }
    val selectedDayTitle = remember(uiState.selectedDate) { dayTitleFormatter.format(uiState.selectedDate.time) }

    // 计算当月日历网格
    val calendarMonth = uiState.currentMonth
    val daysInMonth = calendarMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = remember(calendarMonth) {
        val temp = calendarMonth.clone() as Calendar
        temp.set(Calendar.DAY_OF_MONTH, 1)
        val day = temp.get(Calendar.DAY_OF_WEEK) // 1=Sunday, 2=Monday, ...
        (day + 5) % 7 // 转换为 0=Monday ... 6=Sunday
    }

    val selectedDayNum = uiState.selectedDate.get(Calendar.DAY_OF_MONTH)
    val isSameMonth = uiState.selectedDate.get(Calendar.YEAR) == uiState.currentMonth.get(Calendar.YEAR) &&
            uiState.selectedDate.get(Calendar.MONTH) == uiState.currentMonth.get(Calendar.MONTH)

    val bottomPadding = if (onNavigateBack != null) 24.dp else 96.dp

    SuppressMaterialRipples {
        Scaffold(
            topBar = {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppleMaterials.barBackgroundColor(isDark)),
                    color = AppleMaterials.barBackgroundColor(isDark),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.statusBars)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IosLargeTitleDefaults.TopBarHeight)
                                .padding(horizontal = 8.dp)
                        ) {
                            if (onNavigateBack != null) {
                                IosNavBackButton(
                                    onNavigateBack = onNavigateBack,
                                    label = "返回",
                                    modifier = Modifier.align(Alignment.CenterStart)
                                )
                            }
                            Text(
                                text = "日历",
                                fontSize = 17.sp,
                                fontFamily = SansFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = (-0.4).sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = AppleMaterials.separatorColor(isDark)
                        )
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = bottomPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 月份导航条
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .iosIconClick { viewModel.previousMonth() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "上个月",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = monthTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = SansFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .iosIconClick { viewModel.nextMonth() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "下个月",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // 星期表头
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    listOf("一", "二", "三", "四", "五", "六", "日").forEach { dayName ->
                        Text(
                            text = dayName,
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = SansFontFamily,
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(36.dp)
                        )
                    }
                }

                // 日历网格 (iOS 圆角微气泡，标准 0.97f 按压缩放)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // 空白占位
                    items(firstDayOfWeek) {
                        Spacer(modifier = Modifier.size(36.dp))
                    }

                    // 当月各天
                    items(daysInMonth) { index ->
                        val dayNum = index + 1
                        val isSelected = isSameMonth && selectedDayNum == dayNum
                        val diariesOnThisDay = uiState.diaryDaysMap[dayNum] ?: emptyList()
                        val hasDiary = diariesOnThisDay.isNotEmpty()

                        val cellShape = CircleShape
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(cellShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                )
                                .iosClick { viewModel.selectDate(dayNum) },
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
                                    fontWeight = if (isSelected) FontWeight.Bold else if (hasDiary) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                                if (hasDiary) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 2.dp)
                                            .size(4.dp)
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                                CircleShape
                                            )
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = AppleMaterials.separatorColor(isDark), thickness = 0.5.dp)

                // 所选日期的日记列表区域
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedDayTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = SansFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .iosIconClick { onNavigateToEditor(null, uiState.selectedDate.timeInMillis) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "记录这一天",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                if (uiState.selectedDayDiaries.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "该日暂无记录",
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = SansFontFamily,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            // iOS Capsule Button (Material 3 Button purged)
                            Surface(
                                modifier = Modifier
                                    .height(44.dp)
                                    .clip(CapsuleShape)
                                    .iosClick { onNavigateToEditor(null, uiState.selectedDate.timeInMillis) },
                                shape = CapsuleShape,
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "写这天的日记",
                                        fontFamily = SansFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.selectedDayDiaries) { diary ->
                            PaperCard(
                                onClick = { onNavigateToEditor(diary.id, null) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        StampBadge(
                                            text = diary.mood.displayName,
                                            icon = { MoodIcon(mood = diary.mood, modifier = Modifier.size(12.dp)) },
                                            tintColor = diary.mood.tintColor
                                        )
                                        Text(
                                            text = "${diary.wordCount} 字",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = SansFontFamily,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    if (diary.title.isNotBlank()) {
                                        Text(
                                            text = diary.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontFamily = SansFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Text(
                                        text = diary.previewText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontFamily = SansFontFamily,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
