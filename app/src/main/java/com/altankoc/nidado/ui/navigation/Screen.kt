package com.altankoc.nidado.ui.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Onboarding : Screen("onboarding")
    data object Home : Screen("home")
    data object Calendar : Screen("calendar")
    data object CreateList : Screen("create_list?date={date}") {
        fun createRoute(date: Long? = null) =
            if (date != null) "create_list?date=$date" else "create_list?date=-1"
    }
    data object ListDetail : Screen("list_detail/{listId}") {
        fun createRoute(listId: Int) = "list_detail/$listId"
    }
    data object Courses : Screen("courses")
    data object CreateCourse : Screen("create_course")
    data object EditCourse : Screen("edit_course/{courseId}") {
        fun createRoute(courseId: Int) = "edit_course/$courseId"
    }
    data object Profile : Screen("profile")
    data object Archive : Screen("archive")
}