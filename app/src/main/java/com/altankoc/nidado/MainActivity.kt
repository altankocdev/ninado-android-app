package com.altankoc.nidado

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.altankoc.nidado.domain.model.enums.ThemePreference
import com.altankoc.nidado.ui.navigation.NavGraph
import com.altankoc.nidado.ui.navigation.Screen
import com.altankoc.nidado.ui.theme.NidaDoTheme
import com.altankoc.nidado.widget.WIDGET_NAVIGATE_KEY
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        // Widget'tan gelen yönlendirme: "create_list" ise CreateList'e,
        // diğer her durumda (veya widget dışından açılmışsa) Splash'tan
        // normal akış devam eder.
        val widgetNavigate = intent?.getStringExtra(WIDGET_NAVIGATE_KEY.name)

        val startRoute = when (widgetNavigate) {
            "create_list" -> Screen.CreateList.createRoute()
            "profile" -> Screen.Profile.route
            else -> null
        }

        setContent {
            val user by viewModel.user.collectAsStateWithLifecycle()

            val darkTheme = when (user?.themePreference) {
                ThemePreference.LIGHT -> false
                ThemePreference.DARK -> true
                ThemePreference.SYSTEM, null -> isSystemInDarkTheme()
            }

            NidaDoTheme(darkTheme = darkTheme) {
                val navController = rememberNavController()
                NavGraph(navController = navController, widgetStartRoute = startRoute)
            }
        }
    }
}