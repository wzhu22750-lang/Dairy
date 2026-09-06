package com.example.inkpaperdiary.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.activity.compose.BackHandler
import com.example.inkpaperdiary.core.security.AppLockManager
import com.example.inkpaperdiary.core.sync.SyncManager
import com.example.inkpaperdiary.data.repository.DiaryRepository
import com.example.inkpaperdiary.data.repository.SettingsRepository
import com.example.inkpaperdiary.data.repository.MediaRepository
import com.example.inkpaperdiary.ui.calendar.CalendarScreen
import com.example.inkpaperdiary.ui.calendar.CalendarViewModel
import com.example.inkpaperdiary.ui.editor.EditorScreen
import com.example.inkpaperdiary.ui.editor.EditorViewModel
import com.example.inkpaperdiary.ui.lock.LockScreen
import com.example.inkpaperdiary.ui.lock.LockViewModel
import com.example.inkpaperdiary.ui.onthisday.OnThisDayScreen
import com.example.inkpaperdiary.ui.onthisday.OnThisDayViewModel
import com.example.inkpaperdiary.ui.search.SearchScreen
import com.example.inkpaperdiary.ui.search.SearchViewModel
import com.example.inkpaperdiary.ui.settings.SettingsScreen
import com.example.inkpaperdiary.ui.settings.SettingsViewModel
import com.example.inkpaperdiary.ui.stats.StatsScreen
import com.example.inkpaperdiary.ui.stats.StatsViewModel
import com.example.inkpaperdiary.ui.timeline.TimelineScreen
import com.example.inkpaperdiary.ui.timeline.TimelineViewModel
import com.example.inkpaperdiary.ui.trash.TrashScreen
import com.example.inkpaperdiary.ui.trash.TrashViewModel

sealed interface AppDestination {
    data object Timeline : AppDestination
    data class Editor(val diaryId: String?, val entryDate: Long? = null) : AppDestination
    data object Calendar : AppDestination
    data object OnThisDay : AppDestination
    data object Search : AppDestination
    data object Stats : AppDestination
    data object Settings : AppDestination
    data object Trash : AppDestination
    data object Lock : AppDestination
}

@Composable
fun AppNavigation(
    diaryRepository: DiaryRepository,
    settingsRepository: SettingsRepository,
    mediaRepository: MediaRepository,
    syncManager: SyncManager,
    isLockEnabled: Boolean
) {
    val backStack = remember {
        mutableStateListOf<AppDestination>(AppDestination.Timeline)
    }

    val isAppLocked by AppLockManager.isLocked.collectAsState()
    val currentDestination = backStack.lastOrNull() ?: AppDestination.Timeline

    // 系统返回键：子页面 pop 回上级（编辑页内部自行处理保存后返回，锁屏状态下由 LockScreen 单独处理）
    BackHandler(
        enabled = backStack.size > 1 && currentDestination !is AppDestination.Editor && !(isLockEnabled && isAppLocked)
    ) {
        backStack.removeLast()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (isLockEnabled && isAppLocked) {
            val lockViewModel = remember { LockViewModel(settingsRepository) }
            LockScreen(
                viewModel = lockViewModel,
                onUnlocked = {
                    AppLockManager.unlock()
                }
            )
        } else {
            AnimatedContent(
                targetState = currentDestination,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "ScreenTransition"
            ) { destination ->
                when (destination) {
                    is AppDestination.Lock -> {
                        // 兼容处理：若栈内存在旧 Lock 则移除
                        LaunchedEffect(Unit) {
                            if (backStack.size > 1) {
                                backStack.remove(AppDestination.Lock)
                            } else {
                                backStack.clear()
                                backStack.add(AppDestination.Timeline)
                            }
                        }
                    }
                is AppDestination.Timeline -> {
                    val timelineViewModel = remember { TimelineViewModel(diaryRepository) }
                    TimelineScreen(
                        viewModel = timelineViewModel,
                        onNavigateToEditor = { id -> backStack.add(AppDestination.Editor(id)) },
                        onNavigateToSearch = { backStack.add(AppDestination.Search) },
                        onNavigateToCalendar = { backStack.add(AppDestination.Calendar) },
                        onNavigateToOnThisDay = { backStack.add(AppDestination.OnThisDay) },
                        onNavigateToStats = { backStack.add(AppDestination.Stats) },
                        onNavigateToSettings = { backStack.add(AppDestination.Settings) }
                    )
                }
                is AppDestination.Editor -> {
                    val editorViewModel = remember(destination.diaryId, destination.entryDate) {
                        EditorViewModel(diaryRepository, mediaRepository, destination.diaryId, destination.entryDate)
                    }
                    EditorScreen(
                        viewModel = editorViewModel,
                        onNavigateBack = {
                            if (backStack.size > 1) backStack.removeLast()
                        }
                    )
                }
                is AppDestination.Calendar -> {
                    val calendarViewModel = remember { CalendarViewModel(diaryRepository) }
                    CalendarScreen(
                        viewModel = calendarViewModel,
                        onNavigateBack = {
                            if (backStack.size > 1) backStack.removeLast()
                        },
                        onNavigateToEditor = { id, entryDate -> backStack.add(AppDestination.Editor(id, entryDate)) }
                    )
                }
                is AppDestination.OnThisDay -> {
                    val onThisDayViewModel = remember { OnThisDayViewModel(diaryRepository) }
                    OnThisDayScreen(
                        viewModel = onThisDayViewModel,
                        onNavigateBack = {
                            if (backStack.size > 1) backStack.removeLast()
                        },
                        onNavigateToEditor = { id -> backStack.add(AppDestination.Editor(id)) }
                    )
                }
                is AppDestination.Search -> {
                    val searchViewModel = remember { SearchViewModel(diaryRepository) }
                    SearchScreen(
                        viewModel = searchViewModel,
                        onNavigateBack = {
                            if (backStack.size > 1) backStack.removeLast()
                        },
                        onNavigateToEditor = { id -> backStack.add(AppDestination.Editor(id)) }
                    )
                }
                is AppDestination.Stats -> {
                    val statsViewModel = remember { StatsViewModel(diaryRepository) }
                    StatsScreen(
                        viewModel = statsViewModel,
                        onNavigateBack = {
                            if (backStack.size > 1) backStack.removeLast()
                        }
                    )
                }
                is AppDestination.Settings -> {
                    val settingsViewModel = remember {
                        SettingsViewModel(settingsRepository, diaryRepository, syncManager)
                    }
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onNavigateBack = {
                            if (backStack.size > 1) backStack.removeLast()
                        },
                        onNavigateToTrash = { backStack.add(AppDestination.Trash) }
                    )
                }
                is AppDestination.Trash -> {
                    val trashViewModel = remember { TrashViewModel(diaryRepository) }
                    TrashScreen(
                        viewModel = trashViewModel,
                        onNavigateBack = {
                            if (backStack.size > 1) backStack.removeLast()
                        }
                    )
                }
            }
        }
    }
}
}
