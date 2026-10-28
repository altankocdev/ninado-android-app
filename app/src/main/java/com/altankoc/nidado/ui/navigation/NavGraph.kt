package com.altankoc.nidado.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.altankoc.nidado.ui.archive.ArchiveScreen
import com.altankoc.nidado.ui.calendar.CalendarScreen
import com.altankoc.nidado.ui.courses.CoursesScreen
import com.altankoc.nidado.ui.courses.CreateEditCourseScreen
import com.altankoc.nidado.ui.createlist.CreateListScreen
import com.altankoc.nidado.ui.createlist.CreateListViewModel
import com.altankoc.nidado.ui.home.HomeScreen
import com.altankoc.nidado.ui.listdetail.ListDetailScreen
import com.altankoc.nidado.ui.profile.ProfileScreen
import com.altankoc.nidado.ui.splash.SplashScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    widgetStartRoute: String? = null
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavRoutes = listOf(
        Screen.Home.route,
        Screen.Calendar.route,
        Screen.Courses.route,
        Screen.Profile.route
        // CreateList ve Archive burada yok — bu ekranlardayken navbar tamamen kaybolur
    )
    val showBottomNav = currentRoute in bottomNavRoutes

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(navController = navController)

                // Widget'tan "create_list" yönlendirmesiyle açıldıysa, normal
                // splash akışından sonra otomatik olarak CreateList'e geçer.
                LaunchedEffect(widgetStartRoute) {
                    if (widgetStartRoute != null) {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                        navController.navigate(widgetStartRoute)
                    }
                }
            }
            composable(Screen.Home.route) {
                HomeScreen(navController = navController)
            }
            composable(Screen.Calendar.route) {
                CalendarScreen(navController = navController)
            }
            composable(
                route = Screen.CreateList.route,
                arguments = listOf(navArgument("date") {
                    type = NavType.LongType
                    defaultValue = -1L
                })
            ) { backStackEntry ->
                val dateArg = backStackEntry.arguments?.getLong("date") ?: -1L
                val viewModel: CreateListViewModel = hiltViewModel()

                LaunchedEffect(dateArg) {
                    if (dateArg != -1L) {
                        viewModel.setPreselectedDate(dateArg)
                    }
                }

                CreateListScreen(navController = navController, viewModel = viewModel)
            }
            composable(
                route = Screen.ListDetail.route,
                arguments = listOf(navArgument("listId") { type = NavType.IntType })
            ) {
                ListDetailScreen(navController = navController)
            }
            composable(Screen.Courses.route) {
                CoursesScreen(navController = navController)
            }
            composable(Screen.CreateCourse.route) {
                CreateEditCourseScreen(navController = navController)
            }
            composable(
                route = Screen.EditCourse.route,
                arguments = listOf(navArgument("courseId") { type = NavType.IntType })
            ) { backStackEntry ->
                val courseId = backStackEntry.arguments?.getInt("courseId") ?: return@composable
                CreateEditCourseScreen(
                    courseId = courseId,
                    navController = navController
                )
            }
            composable(Screen.Profile.route) {
                ProfileScreen(navController = navController)
            }
            composable(Screen.Archive.route) {
                ArchiveScreen(navController = navController)
            }
        }

        AnimatedVisibility(
            visible = showBottomNav,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it },
            exit = slideOutVertically { it }
        ) {
            BottomNavBar(navController = navController)
        }
    }
}