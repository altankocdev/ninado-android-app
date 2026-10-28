package com.altankoc.nidado.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    data object Home : BottomNavItem(Screen.Home.route, Icons.Rounded.Home, "Ana Sayfa")
    data object Calendar : BottomNavItem(Screen.Calendar.route, Icons.Rounded.CalendarMonth, "Takvim")
    data object CreateList : BottomNavItem(Screen.CreateList.route, Icons.Rounded.AddCircle, "Oluştur")
    data object Courses : BottomNavItem(Screen.Courses.route, Icons.Rounded.MenuBook, "Dersler")
    data object Profile : BottomNavItem(Screen.Profile.route, Icons.Rounded.Person, "Profil")
}