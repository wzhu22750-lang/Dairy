package com.example.inkpaperdiary.ui.onthisday

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inkpaperdiary.core.designsystem.AppleMaterials
import com.example.inkpaperdiary.core.designsystem.CapsuleShape
import com.example.inkpaperdiary.core.designsystem.MaterialThickness
import com.example.inkpaperdiary.core.designsystem.SansFontFamily
import com.example.inkpaperdiary.core.designsystem.components.MoodIcon
import com.example.inkpaperdiary.core.designsystem.components.PaperCard
import com.example.inkpaperdiary.core.designsystem.components.StampBadge
import com.example.inkpaperdiary.core.designsystem.interaction.SuppressMaterialRipples
import com.example.inkpaperdiary.core.designsystem.scaffold.IosLargeTitleItem
import com.example.inkpaperdiary.core.designsystem.scaffold.IosLargeTitleScaffold
import com.example.inkpaperdiary.core.designsystem.scaffold.IosNavBackButton
import com.example.inkpaperdiary.core.designsystem.scaffold.rememberLazyListScrollOffset
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun OnThisDayScreen(
    viewModel: OnThisDayViewModel,
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToEditor: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val todayFormatter = remember { SimpleDateFormat("M月d日", Locale.CHINESE) }
    val todayStr = remember { todayFormatter.format(Date()) }

    val listState = rememberLazyListState()
    val scrollOffset = rememberLazyListScrollOffset(listState)

    SuppressMaterialRipples {
        IosLargeTitleScaffold(
            title = "那年今日",
            lazyListState = listState,
            navigationIcon = if (onNavigateBack != null) {
                {
                    IosNavBackButton(
                        onNavigateBack = onNavigateBack,
                        label = "返回"
                    )
                }
            } else null
        ) { innerPadding ->
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 3.dp
                    )
                }
            } else if (uiState.memories.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = innerPadding.calculateTopPadding() + 8.dp)
                        .padding(horizontal = 16.dp)
                ) {
                    IosLargeTitleItem(
                        title = "那年今日",
                        subtitle = todayStr,
                        scrollOffset = 0f
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = if (onNavigateBack != null) 32.dp else 96.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "今天暂无往年回忆",
                            fontSize = 17.sp,
                            fontFamily = SansFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "坚持每天写日记，未来的今天将重逢当下的自己",
                            fontSize = 13.sp,
                            fontFamily = SansFontFamily,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = innerPadding.calculateTopPadding(),
                        bottom = if (onNavigateBack != null) 32.dp else 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item(key = "large_title_header") {
                        IosLargeTitleItem(
                            title = "那年今日",
                            subtitle = todayStr,
                            scrollOffset = scrollOffset,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    items(uiState.memories, key = { it.diary.id }) { memory ->
                        val diary = memory.diary
                        val yearFormatter = remember { SimpleDateFormat("yyyy年M月d日", Locale.getDefault()) }
                        val formattedDate = remember(diary.entryDate) { yearFormatter.format(Date(diary.entryDate)) }

                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            PaperCard(
                                onClick = { onNavigateToEditor(diary.id) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = AppleMaterials.backgroundColor(MaterialThickness.THIN),
                                            shape = CapsuleShape,
                                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Icon(
                                                    Icons.Outlined.History,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(13.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = "${memory.yearsAgo} 年前的今天 · $formattedDate",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontFamily = SansFontFamily,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

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
                                        maxLines = 4,
                                        lineHeight = 22.sp
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
