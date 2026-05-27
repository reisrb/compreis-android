package com.rafaelreis.compreis

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.rafaelreis.compreis.ui.screens.CatalogueScreen
import com.rafaelreis.compreis.ui.screens.ItensScreen
import com.rafaelreis.compreis.ui.screens.ListasScreen
import com.rafaelreis.compreis.ui.screens.RelatorioScreen
import com.rafaelreis.compreis.ui.theme.CompreisTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as CompreisApp
        setContent {
            CompreisTheme {
                CompreisNav(app)
            }
        }
    }
}

private sealed class Tab(val route: String, val labelRes: Int, val icon: ImageVector) {
    object Lists : Tab("lists", R.string.tab_lists, Icons.Default.ShoppingCart)
    object Catalogue : Tab("catalogue", R.string.tab_catalogue, Icons.Default.List)
    object Report : Tab("report", R.string.tab_report, Icons.Default.BarChart)
}

private val tabs = listOf(Tab.Lists, Tab.Catalogue, Tab.Report)

@Composable
private fun CompreisNav(app: CompreisApp) {
    val navController = rememberNavController()
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route

    val showBottomBar = currentRoute != null && tabs.any { currentRoute.startsWith(it.route) }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute?.startsWith(tab.route) == true,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(Tab.Lists.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = stringResource(tab.labelRes)) },
                            label = { Text(stringResource(tab.labelRes)) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            NavHost(navController = navController, startDestination = Tab.Lists.route) {
                composable(Tab.Lists.route) {
                    ListasScreen(app = app, onListTap = { id ->
                        navController.navigate("itens/$id")
                    })
                }
                composable(
                    route = "itens/{listaId}",
                    arguments = listOf(navArgument("listaId") { type = NavType.LongType })
                ) { backStack ->
                    val listaId = backStack.arguments!!.getLong("listaId")
                    ItensScreen(app = app, listaId = listaId, onBack = { navController.popBackStack() })
                }
                composable(Tab.Catalogue.route) {
                    CatalogueScreen(app = app)
                }
                composable(Tab.Report.route) {
                    RelatorioScreen(app = app)
                }
            }
        }
    }
}
