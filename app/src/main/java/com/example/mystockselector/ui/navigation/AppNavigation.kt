package com.example.mystockselector.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.mystockselector.R
import com.example.mystockselector.ui.import_.ImportScreen
import com.example.mystockselector.ui.portfolio.PortfolioScreen
import com.example.mystockselector.ui.screener.ScreenerScreen
import com.example.mystockselector.ui.sync.SyncScreen

private data class TopLevelDestination(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
)

private val topLevelDestinations = listOf(
    TopLevelDestination(NavRoutes.IMPORT, R.string.nav_import, Icons.Default.FileUpload),
    TopLevelDestination(NavRoutes.SYNC, R.string.nav_sync, Icons.Default.CloudSync),
    TopLevelDestination(NavRoutes.SCREENER, R.string.nav_screener, Icons.Default.FilterList),
    TopLevelDestination(NavRoutes.PORTFOLIO, R.string.nav_portfolio, Icons.Default.AccountBalance),
)

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                topLevelDestinations.forEach { destination ->
                    NavigationBarItem(
                        icon = { Icon(destination.icon, contentDescription = null) },
                        label = { Text(stringResource(destination.labelRes)) },
                        selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.IMPORT,
        ) {
            composable(NavRoutes.IMPORT) {
                ImportScreen(contentPadding = innerPadding)
            }
            composable(NavRoutes.SYNC) {
                SyncScreen(contentPadding = innerPadding)
            }
            composable(NavRoutes.SCREENER) {
                ScreenerScreen(contentPadding = innerPadding)
            }
            composable(NavRoutes.PORTFOLIO) {
                PortfolioScreen(contentPadding = innerPadding)
            }
        }
    }
}
