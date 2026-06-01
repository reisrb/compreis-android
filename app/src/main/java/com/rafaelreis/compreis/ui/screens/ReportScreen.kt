package com.rafaelreis.compreis.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rafaelreis.compreis.CompreisApp
import com.rafaelreis.compreis.R
import com.rafaelreis.compreis.data.db.AppDatabase
import com.rafaelreis.compreis.data.db.ItemEntity
import com.rafaelreis.compreis.data.db.MarketPriceEntity
import com.rafaelreis.compreis.data.db.ShoppingListEntity
import com.rafaelreis.compreis.ui.theme.Green
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

data class MonthSummary(val label: String, val total: Double, val trips: Int)
data class MarketSpending(val marketName: String, val total: Double, val trips: Int)
data class BasketEntry(val marketName: String, val basketTotal: Double)

class ReportViewModel(private val db: AppDatabase) : ViewModel() {
    val lists = db.shoppingListDao().getAll()
        .map { it.filter { l -> l.finalized } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val items = db.itemDao().getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val marketPrices = db.marketPriceDao().getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

private fun calculateLast7Days(lists: List<ShoppingListEntity>, items: List<ItemEntity>): Double {
    val threshold = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
    val recentIds = lists.filter { (it.finalizedAt ?: 0) >= threshold }.map { it.id }.toSet()
    return items.filter { it.listId in recentIds }.sumOf { it.total }
}

private fun calculateMonthlyAvg(byMonth: List<MonthSummary>): Double =
    if (byMonth.isEmpty()) 0.0 else byMonth.sumOf { it.total } / byMonth.size

private fun calculateAvgPerTrip(lists: List<ShoppingListEntity>, items: List<ItemEntity>): Double {
    if (lists.isEmpty()) return 0.0
    return lists.map { l -> items.filter { it.listId == l.id }.sumOf { it.total } }.average()
}

private fun groupByMonth(lists: List<ShoppingListEntity>, items: List<ItemEntity>): List<MonthSummary> {
    val fmt = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    return lists
        .groupBy { fmt.format(Date(it.finalizedAt ?: it.id)) }
        .map { (label, group) ->
            val ids = group.map { it.id }.toSet()
            MonthSummary(
                label = label.replaceFirstChar { it.uppercase() },
                total = items.filter { it.listId in ids }.sumOf { it.total },
                trips = group.size
            )
        }
        .sortedByDescending { it.label }
}

private fun computeSpendingByMarket(lists: List<ShoppingListEntity>, items: List<ItemEntity>): List<MarketSpending> {
    return lists.filter { it.marketName != null }
        .groupBy { it.marketName!! }
        .map { (market, group) ->
            val ids = group.map { it.id }.toSet()
            val total = group.sumOf { l -> l.totalPaid ?: items.filter { it.listId == l.id }.sumOf { it.total } }
            MarketSpending(marketName = market, total = total, trips = group.size)
        }
        .sortedByDescending { it.total }
}

private fun computeBasketComparison(marketPrices: List<MarketPriceEntity>): List<BasketEntry> {
    val multiMarketProducts = marketPrices.groupBy { it.productName }.filter { it.value.map { p -> p.marketName }.distinct().size >= 2 }.keys
    if (multiMarketProducts.isEmpty()) return emptyList()
    val filtered = marketPrices.filter { it.productName in multiMarketProducts }
    return filtered.groupBy { it.marketName }
        .map { (market, prices) ->
            BasketEntry(marketName = market, basketTotal = prices.sumOf { it.price })
        }
        .sortedBy { it.basketTotal }
}

private val mockLists = listOf(
    ShoppingListEntity(id = 1, name = "Semana 1", finalized = true, finalizedAt = System.currentTimeMillis() - 2L * 24 * 3600 * 1000, marketName = "Carrefour"),
    ShoppingListEntity(id = 2, name = "Churrasco", finalized = true, finalizedAt = System.currentTimeMillis() - 10L * 24 * 3600 * 1000, marketName = "Extra"),
    ShoppingListEntity(id = 3, name = "Semana 2", finalized = true, finalizedAt = System.currentTimeMillis() - 35L * 24 * 3600 * 1000, marketName = "Carrefour"),
    ShoppingListEntity(id = 4, name = "Mês passado", finalized = true, finalizedAt = System.currentTimeMillis() - 65L * 24 * 3600 * 1000, marketName = "Extra"),
    ShoppingListEntity(id = 5, name = "Há 3 meses", finalized = true, finalizedAt = System.currentTimeMillis() - 95L * 24 * 3600 * 1000),
)
private val mockItems = listOf(
    ItemEntity(listId = 1, name = "Frango", price = 12.90, unit = "kg", quantity = 2.0),
    ItemEntity(listId = 1, name = "Arroz", price = 5.49, unit = "un", quantity = 2.0),
    ItemEntity(listId = 1, name = "Feijão", price = 8.90, unit = "un", quantity = 1.0),
    ItemEntity(listId = 2, name = "Picanha", price = 89.90, unit = "kg", quantity = 1.5),
    ItemEntity(listId = 2, name = "Carvão", price = 24.90, unit = "un", quantity = 2.0),
    ItemEntity(listId = 2, name = "Cerveja", price = 4.99, unit = "un", quantity = 12.0),
    ItemEntity(listId = 3, name = "Macarrão", price = 3.29, unit = "un", quantity = 3.0),
    ItemEntity(listId = 3, name = "Molho", price = 6.49, unit = "un", quantity = 2.0),
    ItemEntity(listId = 3, name = "Carne moída", price = 28.90, unit = "kg", quantity = 0.8),
    ItemEntity(listId = 4, name = "Leite", price = 4.89, unit = "un", quantity = 6.0),
    ItemEntity(listId = 4, name = "Pão", price = 8.90, unit = "un", quantity = 2.0),
    ItemEntity(listId = 5, name = "Banana", price = 3.99, unit = "kg", quantity = 1.5),
    ItemEntity(listId = 5, name = "Laranja", price = 5.99, unit = "kg", quantity = 2.0),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(app: CompreisApp) {
    val vm: ReportViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ReportViewModel(app.db) as T
    })
    val lists by vm.lists.collectAsState()
    val items by vm.items.collectAsState()
    val marketPrices by vm.marketPrices.collectAsState()
    var showExamples by remember { mutableStateOf(false) }

    val last7 = calculateLast7Days(lists, items)
    val byMonth = groupByMonth(lists, items)
    val monthlyAvg = calculateMonthlyAvg(byMonth)
    val avgPerTrip = calculateAvgPerTrip(lists, items)
    val spendingByMarket = computeSpendingByMarket(lists, items)
    val basketComparison = computeBasketComparison(marketPrices)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.report_title), fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = { showExamples = true }) {
                        Text(stringResource(R.string.report_examples_btn), color = Green, fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        }
    ) { padding ->
        if (lists.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.BarChart, null, Modifier.size(64.dp), tint = Green.copy(alpha = 0.4f))
                    Text(stringResource(R.string.report_empty_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.report_empty_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { OverviewCards(last7 = last7, monthlyAvg = monthlyAvg, avgPerTrip = avgPerTrip) }
                if (spendingByMarket.isNotEmpty()) {
                    item { Text(stringResource(R.string.report_by_market), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp)) }
                    items(spendingByMarket) { ms -> MarketSpendingCard(spending = ms) }
                }
                if (basketComparison.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(R.string.report_basket_comparison), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                            Text(stringResource(R.string.report_basket_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    items(basketComparison.withIndex().toList()) { (index, entry) -> BasketCard(entry = entry, isCheapest = index == 0) }
                }
                if (byMonth.isNotEmpty()) {
                    item { Text(stringResource(R.string.report_by_month), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp)) }
                    items(byMonth) { month -> MonthCard(month = month) }
                }
            }
        }
    }

    if (showExamples) ExamplesSheet(onDismiss = { showExamples = false })
}

@Composable
private fun OverviewCards(last7: Double, monthlyAvg: Double, avgPerTrip: Double) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MetricCard(title = stringResource(R.string.report_last7days), value = last7.brl(), icon = Icons.Default.DateRange)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard(title = stringResource(R.string.report_monthly_avg), value = monthlyAvg.brl(), icon = Icons.Default.DateRange, modifier = Modifier.weight(1f))
            MetricCard(title = stringResource(R.string.report_per_trip_avg), value = avgPerTrip.brl(), icon = Icons.Default.ShoppingCart, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier.fillMaxWidth()) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = MaterialTheme.shapes.large, color = Green.copy(alpha = 0.15f), modifier = Modifier.size(42.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = Green, modifier = Modifier.size(20.dp)) }
            }
            Column {
                Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Green)
            }
        }
    }
}

@Composable
private fun MonthCard(month: MonthSummary) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(month.label, fontWeight = FontWeight.SemiBold)
                Text(pluralStringResource(R.plurals.report_trips, month.trips, month.trips), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(month.total.brl(), fontWeight = FontWeight.Bold, color = Green)
        }
    }
}

@Composable
private fun MarketSpendingCard(spending: MarketSpending) {
    val avg = if (spending.trips > 0) spending.total / spending.trips else 0.0
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = MaterialTheme.shapes.large, color = Green.copy(alpha = 0.15f), modifier = Modifier.size(42.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Store, null, tint = Green, modifier = Modifier.size(20.dp)) }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(spending.marketName, fontWeight = FontWeight.SemiBold)
                Text("${spending.trips} ${stringResource(R.string.report_trips_label)} · ${stringResource(R.string.report_avg_per_trip)} ${avg.brl()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(spending.total.brl(), fontWeight = FontWeight.Bold, color = Green)
        }
    }
}

@Composable
private fun BasketCard(entry: BasketEntry, isCheapest: Boolean) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (isCheapest) Green.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (isCheapest) Icon(Icons.Default.Star, null, tint = Green, modifier = Modifier.size(16.dp))
                    Text(entry.marketName, fontWeight = FontWeight.SemiBold)
                }
                if (isCheapest) Text(stringResource(R.string.report_cheapest), style = MaterialTheme.typography.bodySmall, color = Green)
            }
            Text(entry.basketTotal.brl(), fontWeight = FontWeight.Bold, color = if (isCheapest) Green else MaterialTheme.colorScheme.onSurface)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExamplesSheet(onDismiss: () -> Unit) {
    val last7 = calculateLast7Days(mockLists, mockItems)
    val byMonth = groupByMonth(mockLists, mockItems)
    val monthlyAvg = calculateMonthlyAvg(byMonth)
    val avgPerTrip = calculateAvgPerTrip(mockLists, mockItems)
    val spendingByMarket = computeSpendingByMarket(mockLists, mockItems)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.report_examples_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.report_examples_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
            }
            OverviewCards(last7 = last7, monthlyAvg = monthlyAvg, avgPerTrip = avgPerTrip)
            if (spendingByMarket.isNotEmpty()) {
                Text(stringResource(R.string.report_by_market), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                spendingByMarket.forEach { ms -> MarketSpendingCard(spending = ms) }
            }
            Text(stringResource(R.string.report_by_month), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            byMonth.forEach { month -> MonthCard(month = month) }
        }
    }
}
