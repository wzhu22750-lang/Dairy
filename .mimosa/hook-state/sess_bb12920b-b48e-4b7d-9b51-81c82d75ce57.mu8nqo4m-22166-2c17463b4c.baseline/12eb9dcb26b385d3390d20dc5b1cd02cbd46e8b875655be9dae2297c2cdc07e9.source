package com.example.inkpaperdiary.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inkpaperdiary.core.designsystem.AppleMaterials
import com.example.inkpaperdiary.core.designsystem.CapsuleShape
import com.example.inkpaperdiary.core.designsystem.MaterialThickness
import com.example.inkpaperdiary.core.designsystem.PaperColors
import com.example.inkpaperdiary.core.designsystem.SansFontFamily
import com.example.inkpaperdiary.core.designsystem.components.MoodIcon
import com.example.inkpaperdiary.core.designsystem.components.PaperCard
import com.example.inkpaperdiary.core.designsystem.components.StampBadge
import com.example.inkpaperdiary.core.designsystem.components.TagChip
import com.example.inkpaperdiary.core.designsystem.components.WeatherIcon
import com.example.inkpaperdiary.core.designsystem.interaction.SuppressMaterialRipples
import com.example.inkpaperdiary.core.designsystem.interaction.iosClick
import com.example.inkpaperdiary.core.designsystem.interaction.iosIconClick
import com.example.inkpaperdiary.core.designsystem.scaffold.IosLargeTitleDefaults
import com.example.inkpaperdiary.core.designsystem.scaffold.IosNavTextButton
import com.example.inkpaperdiary.domain.model.Mood
import com.example.inkpaperdiary.domain.model.Weather
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEditor: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = isSystemInDarkTheme()

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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IosLargeTitleDefaults.TopBarHeight)
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 搜索输入胶囊 (iOS 经典 36dp 半透明圆角搜索框)
                            BasicTextField(
                                value = uiState.query,
                                onValueChange = { viewModel.updateQuery(it) },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontFamily = SansFontFamily,
                                    fontSize = 15.sp,
                                    lineHeight = 20.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                decorationBox = { innerTextField ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(36.dp)
                                            .background(
                                                color = if (isDark) Color(0x24FFFFFF) else Color(0x0D000000),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .border(
                                                width = 0.5.dp,
                                                color = AppleMaterials.separatorColor(isDark),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .padding(horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Search,
                                            contentDescription = null,
                                            tint = PaperColors.MonoGray500,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier.weight(1f),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            if (uiState.query.isEmpty()) {
                                                Text(
                                                    text = "搜索日记标题、内容、地点…",
                                                    fontFamily = SansFontFamily,
                                                    fontSize = 14.sp,
                                                    color = PaperColors.MonoGray400
                                                )
                                            }
                                            innerTextField()
                                        }
                                        if (uiState.query.isNotEmpty()) {
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(PaperColors.MonoGray400.copy(alpha = 0.3f))
                                                    .iosIconClick { viewModel.updateQuery("") },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "清除",
                                                    modifier = Modifier.size(12.dp),
                                                    tint = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            // 标准化 iOS "取消" 文字操作按钮
                            IosNavTextButton(
                                text = "取消",
                                onClick = onNavigateBack
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
            ) {
                // 多维筛选器行 (心情)
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(Mood.entries) { mood ->
                        val isSelected = uiState.selectedMood == mood
                        IosFilterPill(
                            text = mood.displayName,
                            isSelected = isSelected,
                            onClick = { viewModel.toggleMood(mood) },
                            icon = {
                                MoodIcon(
                                    mood = mood,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else mood.tintColor
                                )
                            }
                        )
                    }
                }

                // 多维筛选器行 (天气)
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(Weather.entries) { weather ->
                        val isSelected = uiState.selectedWeather == weather
                        IosFilterPill(
                            text = weather.displayName,
                            isSelected = isSelected,
                            onClick = { viewModel.toggleWeather(weather) },
                            icon = {
                                WeatherIcon(
                                    weather = weather,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else weather.tintColor
                                )
                            }
                        )
                    }
                }

                // 标签筛选
                if (uiState.allTags.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(uiState.allTags) { tag ->
                            TagChip(
                                text = tag.name,
                                isSelected = uiState.selectedTag?.id == tag.id,
                                onClick = { viewModel.toggleTag(tag) }
                            )
                        }
                    }
                }

                HorizontalDivider(
                    color = AppleMaterials.separatorColor(isDark),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(vertical = 6.dp)
                )

                // 搜索结果列表
                if (uiState.results.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (uiState.query.isBlank() && uiState.selectedMood == null && uiState.selectedWeather == null && uiState.selectedTag == null)
                                "输入标题、正文关键词或选择标签快速查找"
                            else
                                "暂无匹配日记",
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = SansFontFamily,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.results, key = { it.id }) { diary ->
                            val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
                            val dateStr = remember(diary.entryDate) { dateFormatter.format(Date(diary.entryDate)) }

                            PaperCard(
                                onClick = { onNavigateToEditor(diary.id) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = dateStr,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = SansFontFamily,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        StampBadge(
                                            text = diary.mood.displayName,
                                            icon = { MoodIcon(mood = diary.mood, modifier = Modifier.size(12.dp)) },
                                            tintColor = diary.mood.tintColor
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
                                        maxLines = 3
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

@Composable
private fun IosFilterPill(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    icon: (@Composable () -> Unit)? = null
) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary else AppleMaterials.backgroundColor(MaterialThickness.THIN)
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else AppleMaterials.separatorColor(isSystemInDarkTheme())

    Row(
        modifier = Modifier
            .clip(CapsuleShape)
            .background(backgroundColor)
            .border(0.5.dp, borderColor, CapsuleShape)
            .iosClick(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        icon?.invoke()
        Text(
            text = text,
            fontFamily = SansFontFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = contentColor
        )
    }
}
