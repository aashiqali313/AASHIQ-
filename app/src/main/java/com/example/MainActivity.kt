package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import com.example.ui.screens.*
import com.example.ui.theme.AashiqTheme
import com.example.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Transparent, immersive edge-to-edge bleeds
        enableEdgeToEdge()

        setContent {
            val viewModel: AppViewModel = viewModel()
            val settings by viewModel.settingsState.collectAsState()
            
            AashiqTheme(
                darkTheme = settings.darkTheme,
                amoledBlack = settings.amoledBlack
            ) {
                val navController = rememberNavController()
                
                NavHost(
                    navController = navController, 
                    startDestination = "splash",
                    enterTransition = {
                        slideInHorizontally(initialOffsetX = { 350 }, animationSpec = tween(220)) + fadeIn(animationSpec = tween(220))
                    },
                    exitTransition = {
                        slideOutHorizontally(targetOffsetX = { -350 }, animationSpec = tween(220)) + fadeOut(animationSpec = tween(220))
                    },
                    popEnterTransition = {
                        slideInHorizontally(initialOffsetX = { -350 }, animationSpec = tween(220)) + fadeIn(animationSpec = tween(220))
                    },
                    popExitTransition = {
                        slideOutHorizontally(targetOffsetX = { 350 }, animationSpec = tween(220)) + fadeOut(animationSpec = tween(220))
                    }
                ) {
                    composable("splash") {
                        SplashScreen {
                            navController.navigate("home") {
                                popUpTo("splash") { inclusive = true }
                            }
                        }
                    }
                    
                    composable("home") {
                        HomeScreen(
                            viewModel = viewModel,
                            onNavigateToSearch = { navController.navigate("search") },
                            onNavigateToSettings = { navController.navigate("settings") },
                            onNavigateToCourseDetail = { id -> navController.navigate("course_detail/$id") },
                            onNavigateToPlayer = { id -> navController.navigate("player/$id") },
                            onNavigateToCertificatesVault = { navController.navigate("certificates_vault") }
                        )
                    }

                    composable("certificates_vault") {
                        CertificatesVaultScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    
                    composable("settings") {
                        SettingsScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    
                    composable("search") {
                        SearchScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToCourseDetail = { id -> navController.navigate("course_detail/$id") },
                            onNavigateToPlayer = { id -> navController.navigate("player/$id") }
                        )
                    }
                    
                    composable(
                        route = "course_detail/{courseId}",
                        arguments = listOf(navArgument("courseId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val courseId = backStackEntry.arguments?.getString("courseId") ?: ""
                        CourseDetailScreen(
                            viewModel = viewModel,
                            courseId = courseId,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToPlayer = { id -> navController.navigate("player/$id") }
                        )
                    }
                    
                    composable(
                        route = "player/{lessonId}",
                        arguments = listOf(navArgument("lessonId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val lessonId = backStackEntry.arguments?.getString("lessonId") ?: ""
                        PlayerScreen(
                            viewModel = viewModel,
                            lessonId = lessonId,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
