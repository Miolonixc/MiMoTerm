package com.mimoterm.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.mimoterm.ui.screens.home.HomeScreen
import com.mimoterm.ui.screens.terminal.TerminalScreen
import com.mimoterm.ui.screens.ai.AiChatScreen
import com.mimoterm.ui.screens.files.FileManagerScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = modifier
    ) {
        composable("home") {
            HomeScreen(
                onNavigateToTerminal = { navController.navigate("terminal") },
                onNavigateToAi = { navController.navigate("ai_chat") },
                onNavigateToFiles = { navController.navigate("files") }
            )
        }
        composable("terminal") {
            TerminalScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable("ai_chat") {
            AiChatScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable("files") {
            FileManagerScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
