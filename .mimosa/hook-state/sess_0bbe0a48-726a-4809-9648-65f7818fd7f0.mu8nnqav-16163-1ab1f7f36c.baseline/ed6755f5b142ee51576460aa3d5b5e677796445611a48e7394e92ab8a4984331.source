package com.example.inkpaperdiary.ui.navigation

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
