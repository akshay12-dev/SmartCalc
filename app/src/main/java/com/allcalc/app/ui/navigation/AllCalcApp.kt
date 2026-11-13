package com.allcalc.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.allcalc.app.ui.calculator.CalculatorScreen
import com.allcalc.app.ui.common.FeedbackSettings
import com.allcalc.app.ui.common.LocalFeedbackSettings
import com.allcalc.app.ui.converter.ConverterScreen
import com.allcalc.app.ui.currency.CurrencyScreen
import com.allcalc.app.ui.history.HistoryScreen
import com.allcalc.app.ui.home.HomeScreen
import com.allcalc.app.ui.scanner.ScanScreen
import com.allcalc.app.ui.settings.SettingsScreen
import com.allcalc.app.ui.settings.SettingsViewModel
import com.allcalc.app.ui.specialized.SpecializedHubScreen
import com.allcalc.app.ui.specialized.SpecializedRunnerScreen
import com.allcalc.app.utils.AppViewModelProvider

object Routes {
    const val HOME = "home"
    const val CALC = "calc"
    const val CONVERTER = "converter"
    const val SCAN = "scan"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val SPECIALIZED_HUB = "specialized_hub"
    fun specializedDetail(id: String) = "specialized/$id"
    const val SPECIALIZED_DETAIL = "specialized/{id}"

    fun currency(from: String, to: String, value: String) = "currency/$from/$to/$value"
    const val CURRENCY = "currency/{from}/{to}/{value}"
}

private data class BottomItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val bottomItems = listOf(
    BottomItem(Routes.CALC, "Calculator", Icons.Filled.Calculate),
    BottomItem(Routes.SCAN, "Scan & Solve", Icons.Filled.CameraAlt),
    BottomItem(Routes.CONVERTER, "Convert", Icons.Filled.SwapHoriz),
    BottomItem(Routes.HISTORY, "History", Icons.Filled.History),
    BottomItem(Routes.SETTINGS, "Settings", Icons.Filled.Settings)
)

private val bottomRoutes = bottomItems.map { it.route }.toSet() + Routes.HOME

@Composable
fun AllCalcApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomRoutes

    val onHomeOrBack: () -> Unit = {
        if (navController.previousBackStackEntry != null) {
            navController.popBackStack()
        } else {
            navController.navigate(Routes.HOME) {
                launchSingleTop = true
            }
        }
    }

    val settingsViewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider)
    val hapticsEnabled by settingsViewModel.haptics.collectAsStateWithLifecycle()
    val soundEnabled by settingsViewModel.sound.collectAsStateWithLifecycle()

    CompositionLocalProvider(
        LocalFeedbackSettings provides FeedbackSettings(haptics = hapticsEnabled, sound = soundEnabled)
    ) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            NavHost(
                navController = navController,
                startDestination = Routes.CALC,
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp)
            ) {
                composable(Routes.HOME) {
                    HomeScreen(
                        onNavigate = { route -> navController.navigate(route) },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Routes.CALC) {
                    CalculatorScreen(onHome = onHomeOrBack)
                }
                composable(Routes.SCAN) {
                    ScanScreen(
                        onHome = onHomeOrBack,
                        onOpenCalculator = {
                            navController.navigate(Routes.CALC) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
                composable(Routes.CONVERTER) {
                    ConverterScreen(
                        onHome = onHomeOrBack,
                        onOpenCurrency = {
                            navController.navigate(Routes.currency("USD", "INR", "1"))
                        }
                    )
                }
                composable(Routes.HISTORY) {
                    HistoryScreen(
                        onHome = onHomeOrBack,
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onOpenCurrency = { from, to, value ->
                            navController.navigate(Routes.currency(from, to, value))
                        }
                    )
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen(onHome = onHomeOrBack)
                }
                composable(
                    route = Routes.CURRENCY,
                    arguments = listOf(
                        navArgument("from") { type = NavType.StringType; defaultValue = "" },
                        navArgument("to") { type = NavType.StringType; defaultValue = "" },
                        navArgument("value") { type = NavType.StringType; defaultValue = "" }
                    )
                ) { entry ->
                    val from = entry.arguments?.getString("from").orEmpty()
                    val to = entry.arguments?.getString("to").orEmpty()
                    val value = entry.arguments?.getString("value").orEmpty()
                    CurrencyScreen(
                        initialFrom = from,
                        initialTo = to,
                        initialValue = value,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Routes.SPECIALIZED_HUB) {
                    SpecializedHubScreen(
                        onSelectCalculator = { id ->
                            navController.navigate(Routes.specializedDetail(id))
                        },
                        onBack = onHomeOrBack
                    )
                }
                composable(
                    route = Routes.SPECIALIZED_DETAIL,
                    arguments = listOf(
                        navArgument("id") { type = NavType.StringType; defaultValue = "" }
                    )
                ) { entry ->
                    val calcId = entry.arguments?.getString("id").orEmpty()
                    SpecializedRunnerScreen(
                        calcId = calcId,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
    }
}