package com.example.inkpaperdiary.ui.navigation

/**
 * 路由名契约（Dairy 2.0）。
 *
 * 当前导航由 AppNavigation 的 Tab + 模态栈驱动；本清单保留为
 * 全应用的路由名注册表（测试断言、未来 Navigation3 迁移共用）。
 */
sealed class Screen(val route: String) {
    data object Timeline : Screen("timeline")
    data object Calendar : Screen("calendar")
    data object OnThisDay : Screen("on_this_day")
    data object Stats : Screen("stats")
    data object Search : Screen("search")
    data object Settings : Screen("settings")
    data object Trash : Screen("trash")
    data object Lock : Screen("lock")
    data object Editor : Screen("editor/{diaryId}") {
        fun createRoute(diaryId: String? = null): String {
            return if (diaryId != null) "editor/$diaryId" else "editor/new"
        }
    }
}
