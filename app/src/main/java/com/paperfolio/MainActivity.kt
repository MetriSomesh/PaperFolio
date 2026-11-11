package com.paperfolio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.paperfolio.ui.home.HomeScreen
import com.paperfolio.ui.home.HomeViewModel
import com.paperfolio.ui.pnl.PnlScreen
import com.paperfolio.ui.pnl.PnlViewModel
import com.paperfolio.ui.portfolio.PortfolioDetailScreen
import com.paperfolio.ui.portfolio.PortfolioDetailViewModel
import com.paperfolio.ui.search.StockSearchScreen
import com.paperfolio.ui.search.StockSearchViewModel
import com.paperfolio.ui.settings.SettingsScreen
import com.paperfolio.ui.settings.SettingsViewModel
import com.paperfolio.ui.stockdetail.StockDetailScreen
import com.paperfolio.ui.stockdetail.StockDetailViewModel
import com.paperfolio.ui.theme.BackgroundDark
import com.paperfolio.ui.theme.BrandPrimary
import com.paperfolio.ui.theme.PaperFolioTheme
import com.paperfolio.ui.theme.SurfaceDark
import com.paperfolio.ui.theme.TextPrimary
import com.paperfolio.ui.theme.TextSecondary

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Home : Screen("home", "Portfolios", Icons.Default.PieChart)
    object Search : Screen("search", "Search", Icons.Default.Search)
    object Pnl : Screen("pnl", "P&L", Icons.AutoMirrored.Filled.TrendingUp)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)

    object PortfolioDetail : Screen("portfolio/{portfolioId}", "Portfolio") {
        fun createRoute(id: Long) = "portfolio/$id"
    }

    object StockDetail : Screen("stock/{yahooSymbol}", "Stock") {
        fun createRoute(symbol: String) = "stock/$symbol"
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as PaperFolioApp

        try {
            setContent {
                PaperFolioTheme {
                    MainAppNavigation(app = app)
                }
            }
        } catch (e: Throwable) {
            android.util.Log.e("MainActivity", "Error during UI setup", e)
            setContent {
                androidx.compose.material3.Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BackgroundDark
                ) {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                    ) {
                        Text("PaperFolio Recovery", color = TextPrimary, fontSize = 20.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(e.localizedMessage ?: e.toString(), color = TextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        androidx.compose.material3.Button(onClick = {
                            applicationContext.deleteDatabase("paperfolio_v2.db")
                            recreate()
                        }) {
                            Text("Reset & Retry")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainAppNavigation(app: PaperFolioApp) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        Screen.Home,
        Screen.Search,
        Screen.Pnl,
        Screen.Settings
    )

    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = SurfaceDark,
                    tonalElevation = 8.dp
                ) {
                    bottomNavItems.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                screen.icon?.let {
                                    Icon(it, contentDescription = screen.title)
                                }
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    fontSize = 11.sp
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = BrandPrimary,
                                selectedTextColor = BrandPrimary,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = SurfaceDark
                            )
                        )
                    }
                }
            }
        },
        containerColor = BackgroundDark
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                val homeViewModel = viewModel<HomeViewModel>(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            return HomeViewModel(app.portfolioRepository) as T
                        }
                    }
                )
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                    onNavigateToPortfolio = { portfolioId ->
                        navController.navigate(Screen.PortfolioDetail.createRoute(portfolioId))
                    }
                )
            }

            composable(Screen.Search.route) {
                val searchViewModel = viewModel<StockSearchViewModel>(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            return StockSearchViewModel(app.stockRepository) as T
                        }
                    }
                )
                StockSearchScreen(
                    viewModel = searchViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onStockSelected = { symbol ->
                        navController.navigate(Screen.StockDetail.createRoute(symbol))
                    }
                )
            }

            composable(
                route = Screen.PortfolioDetail.route,
                arguments = listOf(navArgument("portfolioId") { type = NavType.LongType })
            ) { backStackEntry ->
                val portfolioId = backStackEntry.arguments?.getLong("portfolioId") ?: 0L
                val detailViewModel = viewModel<PortfolioDetailViewModel>(
                    key = "portfolio_$portfolioId",
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            return PortfolioDetailViewModel(
                                portfolioId = portfolioId,
                                portfolioRepository = app.portfolioRepository,
                                tradeRepository = app.tradeRepository,
                                snapshotDao = app.database.portfolioSnapshotDao()
                            ) as T
                        }
                    }
                )
                PortfolioDetailScreen(
                    viewModel = detailViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                    onNavigateToStockDetail = { symbol ->
                        navController.navigate(Screen.StockDetail.createRoute(symbol))
                    }
                )
            }

            composable(
                route = Screen.StockDetail.route,
                arguments = listOf(navArgument("yahooSymbol") { type = NavType.StringType })
            ) { backStackEntry ->
                val symbol = backStackEntry.arguments?.getString("yahooSymbol") ?: ""
                val stockDetailViewModel = viewModel<StockDetailViewModel>(
                    key = "stock_$symbol",
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            return StockDetailViewModel(
                                yahooSymbol = symbol,
                                stockRepository = app.stockRepository,
                                portfolioRepository = app.portfolioRepository,
                                tradeRepository = app.tradeRepository
                            ) as T
                        }
                    }
                )
                StockDetailScreen(
                    viewModel = stockDetailViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Pnl.route) {
                val pnlViewModel = viewModel<PnlViewModel>(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            return PnlViewModel(
                                tradeDao = app.database.tradeDao(),
                                portfolioRepository = app.portfolioRepository,
                                tradeRepository = app.tradeRepository
                            ) as T
                        }
                    }
                )
                PnlScreen(
                    viewModel = pnlViewModel,
                    onNavigateToPortfolio = { portfolioId ->
                        navController.navigate(Screen.PortfolioDetail.createRoute(portfolioId))
                    }
                )
            }

            composable(Screen.Settings.route) {
                val settingsViewModel = viewModel<SettingsViewModel>(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            return SettingsViewModel(
                                stockMasterDao = app.database.stockMasterDao(),
                                stockRepository = app.stockRepository
                            ) as T
                        }
                    }
                )
                SettingsScreen(viewModel = settingsViewModel)
            }
        }
    }
}
