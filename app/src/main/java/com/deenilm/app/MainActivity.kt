package com.deenilm.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.*
import com.deenilm.app.ui.screens.*
import com.deenilm.app.ui.strings.uiString
import com.deenilm.app.ui.theme.DeenIlmTheme

private data class Tab(val route: String, val labelKey: String, val icon: ImageVector)

private val TABS = listOf(
    Tab("home", "home", Icons.Filled.Home),
    Tab("quran", "quran", Icons.Filled.MenuBook),
    Tab("hadith", "hadith", Icons.Filled.LibraryBooks),
    Tab("books", "books", Icons.Filled.MenuBook),
    Tab("dua", "dua", Icons.Filled.Favorite),
    Tab("tasbih", "tasbih", Icons.Filled.AccessTime),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DeenIlmTheme {
                val navController = rememberNavController()
                val backStack by navController.currentBackStackEntryAsState()
                val currentRoute = backStack?.destination?.route
                val showBar = TABS.any { it.route == currentRoute }
                Scaffold(
                    bottomBar = {
                        if (showBar) {
                            NavigationBar {
                                TABS.forEach { tab ->
                                    val tabLabel = uiString(tab.labelKey)
                                    NavigationBarItem(
                                        selected = currentRoute == tab.route,
                                        onClick = {
                                            navController.navigate(tab.route) {
                                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = { Icon(tab.icon, contentDescription = tabLabel) },
                                        label = { Text(tabLabel) }
                                    )
                                }
                            }
                        }
                    }
                ) { padding ->
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.padding(padding)
                    ) {
                        composable("home") { HomeScreen(navController) }
                        composable("quran") { QuranListScreen(navController) }
                        composable(
                            "quran/surah/{surahId}",
                            arguments = listOf(navArgument("surahId") { type = NavType.IntType })
                        ) { e ->
                            SurahReaderScreen(e.arguments?.getInt("surahId") ?: 1, navController)
                        }
                        composable("hadith") { HadithBooksScreen(navController) }
                        composable("hadith/book/{bookSlug}") { e ->
                            HadithListScreen(
                                e.arguments?.getString("bookSlug") ?: "bukhari",
                                navController
                            )
                        }
                        composable(
                            "hadith/book/{bookSlug}/hadith/{number}",
                            arguments = listOf(navArgument("number") { type = NavType.IntType })
                        ) { e ->
                            HadithDetailScreen(
                                e.arguments?.getString("bookSlug") ?: "bukhari",
                                e.arguments?.getInt("number") ?: 1
                            )
                        }
                        composable("dua") { DuaScreen() }
                        composable("tasbih") { TasbihScreen() }
                        composable("prayer") { PrayerScreen() }
                        composable("qibla") { QiblaScreen() }
                        composable("books") { BooksScreen(navController) }
                        composable(
                            "books/read/{index}",
                            arguments = listOf(navArgument("index") { type = NavType.IntType })
                        ) { e -> BookReaderScreen(e.arguments?.getInt("index") ?: 0, navController) }
                        composable("settings") { SettingsScreen() }
                    }
                }
            }
        }
    }
}
