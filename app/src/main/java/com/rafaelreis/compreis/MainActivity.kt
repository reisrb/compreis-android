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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.rafaelreis.compreis.ui.screens.CatalogueScreen
import com.rafaelreis.compreis.ui.screens.ItemsScreen
import com.rafaelreis.compreis.ui.screens.ListsScreen
import com.rafaelreis.compreis.ui.screens.ProfileScreen
import com.rafaelreis.compreis.ui.screens.ReportScreen
import com.rafaelreis.compreis.ui.screens.TemplatesScreen
import com.rafaelreis.compreis.ui.theme.CompreisTheme
import androidx.compose.ui.res.stringResource

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
    object Profile : Tab("profile", R.string.tab_profile, Icons.Default.Person)
}

private val tabs = listOf(Tab.Lists, Tab.Catalogue, Tab.Report, Tab.Profile)

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
                    ListsScreen(
                        app = app,
                        onListTap = { id -> navController.navigate("itens/$id") },
                        onTemplates = { navController.navigate("templates") }
                    )
                }
                composable(
                    route = "itens/{listaId}",
                    arguments = listOf(navArgument("listaId") { type = NavType.LongType })
                ) { backStack ->
                    val listaId = backStack.arguments!!.getLong("listaId")
                    ItemsScreen(app = app, listaId = listaId, onBack = { navController.popBackStack() })
                }
                composable("templates") {
                    TemplatesScreen(
                        app = app,
                        onBack = { navController.popBackStack() },
                        onTemplateAbrir = { id -> navController.navigate("itens/$id") }
                    )
                }
                composable(Tab.Catalogue.route) {
                    CatalogueScreen(app = app)
                }
                composable(Tab.Report.route) {
                    ReportScreen(app = app)
                }
                composable(Tab.Profile.route) {
                    ProfileScreen(app = app)
                }
            }
        }
    }
}
