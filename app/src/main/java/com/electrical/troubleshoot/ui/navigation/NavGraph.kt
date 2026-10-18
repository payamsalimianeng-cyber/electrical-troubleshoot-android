package com.electrical.troubleshoot.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.electrical.troubleshoot.data.remote.TokenManager
import com.electrical.troubleshoot.ui.screens.auth.LoginScreen
import com.electrical.troubleshoot.ui.screens.rooms.RoomsScreen
import com.electrical.troubleshoot.ui.screens.mechanisms.MechanismsScreen
import com.electrical.troubleshoot.ui.screens.troubleshooting.TroubleshootingScreen
import com.electrical.troubleshoot.ui.screens.experience.ExperiencesScreen
import com.electrical.troubleshoot.ui.screens.pdf.UploadPdfScreen

object Routes {
    const val LOGIN = "login"
    const val ROOMS = "rooms"
    const val MECHANISMS = "mechanisms/{roomId}/{roomName}"
    const val TROUBLESHOOT = "troubleshoot/{sessionId}"
    const val EXPERIENCES = "experiences"
    const val START_SESSION = "start_session/{roomId}/{mechanismId}/{mechanismName}"
    const val UPLOAD_PDF = "upload_pdf"
}

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val token by TokenManager.getTokenFlow(context).collectAsState(initial = null)

    val startDestination = if (token.isNullOrBlank()) Routes.LOGIN else Routes.ROOMS

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.ROOMS) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.ROOMS) {
            RoomsScreen(
                onRoomClick = { roomId, roomName ->
                    navController.navigate("mechanisms/$roomId/$roomName")
                },
                onExperiencesClick = {
                    navController.navigate(Routes.EXPERIENCES)
                },
                onUploadPdfClick = {
                    navController.navigate(Routes.UPLOAD_PDF)
                },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.UPLOAD_PDF) {
            UploadPdfScreen(
                onBack = { navController.popBackStack() },
                onCreated = {
                    navController.navigate(Routes.ROOMS) {
                        popUpTo(Routes.ROOMS) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.MECHANISMS,
            arguments = listOf(
                navArgument("roomId") { type = NavType.StringType },
                navArgument("roomName") { type = NavType.StringType }
            )
        ) { backStack ->
            val roomId = backStack.arguments?.getString("roomId") ?: return@composable
            val roomName = backStack.arguments?.getString("roomName") ?: ""
            MechanismsScreen(
                roomId = roomId,
                roomName = roomName,
                onBack = { navController.popBackStack() },
                onStartTroubleshoot = { mechanismId, mechanismName ->
                    navController.navigate("start_session/$roomId/$mechanismId/$mechanismName")
                }
            )
        }

        composable(
            route = Routes.START_SESSION,
            arguments = listOf(
                navArgument("roomId") { type = NavType.StringType },
                navArgument("mechanismId") { type = NavType.StringType },
                navArgument("mechanismName") { type = NavType.StringType }
            )
        ) { backStack ->
            val roomId = backStack.arguments?.getString("roomId") ?: return@composable
            val mechanismId = backStack.arguments?.getString("mechanismId") ?: return@composable
            val mechanismName = backStack.arguments?.getString("mechanismName") ?: ""
            TroubleshootingScreen(
                sessionId = null,
                roomId = roomId,
                mechanismId = mechanismId,
                mechanismName = mechanismName,
                onBack = { navController.popBackStack() },
                onSessionCreated = { newSessionId ->
                    navController.navigate("troubleshoot/$newSessionId") {
                        popUpTo(Routes.ROOMS)
                    }
                }
            )
        }

        composable(
            route = Routes.TROUBLESHOOT,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStack ->
            val sessionId = backStack.arguments?.getString("sessionId") ?: return@composable
            TroubleshootingScreen(
                sessionId = sessionId,
                roomId = null,
                mechanismId = null,
                mechanismName = null,
                onBack = { navController.popBackStack() },
                onSessionCreated = {}
            )
        }

        composable(Routes.EXPERIENCES) {
            ExperiencesScreen(onBack = { navController.popBackStack() })
        }
    }
}
