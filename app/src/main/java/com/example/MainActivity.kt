package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ThemeMode
import com.example.ui.screens.*
import com.example.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            val settings by viewModel.settings.collectAsState()
            val themeMode = when (settings.theme) {
                "LIGHT" -> ThemeMode.LIGHT
                "AMOLED" -> ThemeMode.AMOLED
                else -> ThemeMode.DARK
            }

            MyApplicationTheme(themeMode = themeMode) {
                val navController = rememberNavController()
                
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Set up screen navigation routing path matching PRD specifications
                    NavHost(
                        navController = navController,
                        startDestination = "splash",
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // 1. SPLASH SCREEN (Glowing reveal)
                        composable("splash") {
                            SplashScreen(
                                onSplashFinished = {
                                    navController.navigate("home") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                }
                            )
                        }

                        //  HOME SCREEN (Hero Banner, catalog grids)
                        composable("home") {
                            HomeScreen(
                                viewModel = viewModel,
                                onNavigateToCourse = { courseId ->
                                    navController.navigate("course_detail/$courseId")
                                },
                                onNavigateToPlayer = { courseId, lessonId ->
                                    viewModel.selectLesson(lessonId)
                                    navController.navigate("player/$courseId/$lessonId")
                                },
                                onNavigateToBookmarks = {
                                    navController.navigate("bookmarks")
                                },
                                onNavigateToSettings = {
                                    navController.navigate("settings")
                                },
                                onNavigateToSearch = {
                                    navController.navigate("search")
                                }
                            )
                        }

                        // 3. COURSE DETAIL SCREEN (Accordion panels, statistics progress ring)
                        composable(
                            route = "course_detail/{courseId}",
                            arguments = listOf(navArgument("courseId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val courseId = backStackEntry.arguments?.getString("courseId") ?: ""
                            CourseDetailScreen(
                                courseId = courseId,
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToPlayer = { cid, lid ->
                                    viewModel.selectLesson(lid)
                                    navController.navigate("player/$cid/$lid")
                                }
                            )
                        }

                        // 4. PLAYER SCREEN (Cinema Media Player, Volume/Brightness gestures, markdown tabs)
                        composable(
                            route = "player/{courseId}/{lessonId}",
                            arguments = listOf(
                                navArgument("courseId") { type = NavType.StringType },
                                navArgument("lessonId") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val courseId = backStackEntry.arguments?.getString("courseId") ?: ""
                            val activeLessonId by viewModel.activeLessonId.collectAsState()
                            
                            // Dynamically react inside the player when switching lessons from queue
                            val lessonId = activeLessonId ?: backStackEntry.arguments?.getString("lessonId") ?: ""

                            PlayerScreen(
                                courseId = courseId,
                                lessonId = lessonId,
                                viewModel = viewModel,
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        // 5. SETTINGS SCREEN (Preferences theme/speed control)
                        composable("settings") {
                            SettingsScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        // 6. SEARCH SCREEN (Instant crawl results & recently used keywords)
                        composable("search") {
                            SearchScreen(
                                viewModel = viewModel,
                                onNavigateToCourse = { courseId ->
                                    navController.navigate("course_detail/$courseId")
                                },
                                onNavigateToPlayer = { courseId, lessonId ->
                                    viewModel.selectLesson(lessonId)
                                    navController.navigate("player/$courseId/$lessonId")
                                },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        // 7. BOOKMARKS SCREEN (Aggregated timestamps checkpoints list)
                        composable("bookmarks") {
                            BookmarksScreen(
                                viewModel = viewModel,
                                onNavigateToPlayer = { courseId, lessonId ->
                                    viewModel.selectLesson(lessonId)
                                    navController.navigate("player/$courseId/$lessonId")
                                },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
