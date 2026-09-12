package com.example.inkpaperdiary.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
    data class Editor(val diaryId: String?, val entryDate: Long? = null) : AppDestination
    data object Search : AppDestination
    data object Stats : AppDestination
    data object Trash : AppDestination
}

/**
 * 2-Tier Apple HIG Navigation Architecture:
 * 1. Root Level: 4-tab bottom navigation (Journal, Calendar, Memories, Settings) via IosTabBar
 * 2. Modal/Detail Level: Pushed modal screens (Editor, Search, Stats, Trash) with native back navigation
 */
@Composable
fun AppNavigation(
    diaryRepository: DiaryRepository,
    settingsRepository: SettingsRepository,
    mediaRepository: MediaRepository,
    syncManager: SyncManager,
    isLockEnabled: Boolean
) {
    var selectedTab by remember { mutableStateOf(IosTab.JOURNAL) }
    val modalStack = remember { mutableStateListOf<AppDestination>() }

    val isAppLocked by AppLockManager.isLocked.collectAsState()
    val currentModal = modalStack.lastOrNull()

    // 预热并持久化根 Tab 的 ViewModel 实例
    val timelineViewModel = remember { TimelineViewModel(diaryRepository) }
    val calendarViewModel = remember { CalendarViewModel(diaryRepository) }
    val onThisDayViewModel = remember { OnThisDayViewModel(diaryRepository) }
    val settingsViewModel = remember { SettingsViewModel(settingsRepository, diaryRepository, syncManager) }

    // 系统返回键分层拦截：
    // 1. 如果有弹层/编辑页，先回退弹层（编辑页内部自行保存）
    // 2. 如果停留在非 Journal Tab，按返回键优先切回 Journal 主日记 Tab
    // 3. 在主日记 Tab 且无弹层时，不拦截返回键（退出应用）
    BackHandler(
        enabled = !(isLockEnabled && isAppLocked) && (modalStack.isNotEmpty() || selectedTab != IosTab.JOURNAL)
    ) {
        if (modalStack.isNotEmpty()) {
            if (currentModal !is AppDestination.Editor) {
                modalStack.removeAt(modalStack.size - 1)
            }
        } else if (selectedTab != IosTab.JOURNAL) {
            selectedTab = IosTab.JOURNAL
        }
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
            Box(modifier = Modifier.fillMaxSize()) {
                if (currentModal != null) {
                    // Modal 页面展示 (全屏覆盖，暂时隐藏底部 TabBar)
                    AnimatedContent(
                        targetState = currentModal,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "ModalTransition"
                    ) { destination ->
                        when (destination) {
                            is AppDestination.Editor -> {
                                val editorViewModel = remember(destination.diaryId, destination.entryDate) {
                                    EditorViewModel(diaryRepository, mediaRepository, destination.diaryId, destination.entryDate)
                                }
                                EditorScreen(
                                    viewModel = editorViewModel,
                                    onNavigateBack = {
                                        if (modalStack.isNotEmpty()) modalStack.removeAt(modalStack.size - 1)
                                    }
                                )
                            }
                            is AppDestination.Search -> {
                                val searchViewModel = remember { SearchViewModel(diaryRepository) }
                                SearchScreen(
                                    viewModel = searchViewModel,
                                    onNavigateBack = {
                                        if (modalStack.isNotEmpty()) modalStack.removeAt(modalStack.size - 1)
                                    },
                                    onNavigateToEditor = { id ->
                                        modalStack.add(AppDestination.Editor(id))
                                    }
                                )
                            }
                            is AppDestination.Stats -> {
                                val statsViewModel = remember { StatsViewModel(diaryRepository) }
                                StatsScreen(
                                    viewModel = statsViewModel,
                                    onNavigateBack = {
                                        if (modalStack.isNotEmpty()) modalStack.removeAt(modalStack.size - 1)
                                    }
                                )
                            }
                            is AppDestination.Trash -> {
                                val trashViewModel = remember { TrashViewModel(diaryRepository) }
                                TrashScreen(
                                    viewModel = trashViewModel,
                                    onNavigateBack = {
                                        if (modalStack.isNotEmpty()) modalStack.removeAt(modalStack.size - 1)
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // 4 栏式根视图架构 (持久化底部 IosTabBar)
                    Box(modifier = Modifier.fillMaxSize()) {
                        AnimatedContent(
                            targetState = selectedTab,
                            transitionSpec = {
                                fadeIn() togetherWith fadeOut()
                            },
                            label = "TabTransition"
                        ) { tab ->
                            when (tab) {
                                IosTab.JOURNAL -> {
                                    TimelineScreen(
                                        viewModel = timelineViewModel,
                                        onNavigateToEditor = { id -> modalStack.add(AppDestination.Editor(id)) },
                                        onNavigateToSearch = { modalStack.add(AppDestination.Search) },
                                        onNavigateToCalendar = { selectedTab = IosTab.CALENDAR },
                                        onNavigateToOnThisDay = { selectedTab = IosTab.MEMORIES },
                                        onNavigateToStats = { modalStack.add(AppDestination.Stats) },
                                        onNavigateToSettings = { selectedTab = IosTab.SETTINGS }
                                    )
                                }
                                IosTab.CALENDAR -> {
                                    CalendarScreen(
                                        viewModel = calendarViewModel,
                                        onNavigateBack = null,
                                        onNavigateToEditor = { id, entryDate ->
                                            modalStack.add(AppDestination.Editor(id, entryDate))
                                        }
                                    )
                                }
                                IosTab.MEMORIES -> {
                                    OnThisDayScreen(
                                        viewModel = onThisDayViewModel,
                                        onNavigateBack = null,
                                        onNavigateToEditor = { id ->
                                            modalStack.add(AppDestination.Editor(id))
                                        }
                                    )
                                }
                                IosTab.SETTINGS -> {
                                    SettingsScreen(
                                        viewModel = settingsViewModel,
                                        onNavigateBack = null,
                                        onNavigateToTrash = { modalStack.add(AppDestination.Trash) }
                                    )
                                }
                            }
                        }

                        // 底部悬浮毛玻璃 TabBar
                        IosTabBar(
                            selectedTab = selectedTab,
                            onTabSelected = { selectedTab = it },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }
    }
}
