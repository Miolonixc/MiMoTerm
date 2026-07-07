package com.mimoterm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.mimoterm.ui.components.BottomNavBar
import com.mimoterm.ui.navigation.AppNavigation
import com.mimoterm.ui.theme.MiMoTermTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MiMoTermTheme {
                val navController = rememberNavController()
                val navBackStackEntry = navController.currentBackStackEntry
                val currentRoute = navBackStackEntry?.destination?.route ?: "home"

                Surface(modifier = Modifier.fillMaxSize()) {
                    androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxSize()) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier.weight(1f)
                        ) {
                            AppNavigation(navController = navController)
                        }
                        BottomNavBar(
                            currentRoute = currentRoute,
                            onNavigate = { route ->
                                navController.navigate(route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
