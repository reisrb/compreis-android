package com.rafaelreis.compreis.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rafaelreis.compreis.CompreisApp
import com.rafaelreis.compreis.R
import com.rafaelreis.compreis.data.db.AppDatabase
import com.rafaelreis.compreis.data.db.MarketEntity
import com.rafaelreis.compreis.data.db.MarketPriceEntity
import com.rafaelreis.compreis.ui.theme.Green
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MarketsViewModel(private val db: AppDatabase) : ViewModel() {
    val markets = db.marketDao().getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val prices = db.marketPriceDao().getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addMarket(name: String) = viewModelScope.launch {
        db.marketDao().insert(MarketEntity(name = name))
    }

    fun deleteMarket(market: MarketEntity) = viewModelScope.launch {
        db.marketPriceDao().deleteByMarket(market.name)
        db.marketDao().delete(market)
    }

    fun deletePrice(price: MarketPriceEntity) = viewModelScope.launch {
        db.marketPriceDao().delete(price)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketsScreen(app: CompreisApp, onBack: () -> Unit) {
    val vm: MarketsViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T = MarketsViewModel(app.db) as T
    })
    val markets by vm.markets.collectAsState()
    val prices by vm.prices.collectAsState()
    var showNew by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<MarketEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.markets_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.items_back_desc)) }
                },
                actions = {
                    IconButton(onClick = { showNew = true }) { Icon(Icons.Default.Add, contentDescription = null, tint = Green) }
                }
            )
        }
    ) { padding ->
        if (markets.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.LocationOff, null, Modifier.size(64.dp), tint = Green.copy(alpha = 0.4f))
                    Text(stringResource(R.string.markets_empty_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.markets_empty_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(markets, key = { it.id }) { market ->
                    val count = prices.count { it.marketName == market.name }
                    MarketCard(market = market, productCount = count, onTap = { detail = market }, onDelete = { vm.deleteMarket(market) })
                }
            }
        }
    }

    if (showNew) NewMarketSheet(existingNames = markets.map { it.name }, onDismiss = { showNew = false }, onCreate = { name -> vm.addMarket(name); showNew = false })
    detail?.let { market ->
        MarketDetailSheet(
            market = market,
            prices = prices.filter { it.marketName == market.name },
            onDismiss = { detail = null },
            onDeletePrice = { vm.deletePrice(it) }
        )
    }
}

@Composable
private fun MarketCard(market: MarketEntity, productCount: Int, onTap: () -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onTap() }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = MaterialTheme.shapes.large, color = Green.copy(alpha = 0.15f), modifier = Modifier.size(42.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Place, contentDescription = null, tint = Green, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(market.name, fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.markets_product_count, productCount), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.list_delete_desc), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MarketDetailSheet(
    market: MarketEntity,
    prices: List<MarketPriceEntity>,
    onDismiss: () -> Unit,
    onDeletePrice: (MarketPriceEntity) -> Unit
) {
    var search by remember { mutableStateOf("") }
    val filtered = if (search.isBlank()) prices
        else prices.filter { it.productName.contains(search, ignoreCase = true) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(market.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text(stringResource(R.string.markets_search_product)) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            if (filtered.isEmpty()) {
                Text(stringResource(R.string.markets_no_prices), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.heightIn(max = 400.dp)) {
                    items(filtered.sortedBy { it.productName }, key = { it.id }) { price ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(price.productName, fontWeight = FontWeight.SemiBold)
                                Text("/ ${price.unit}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(price.price.brl(), fontWeight = FontWeight.Bold, color = Green)
                            IconButton(onClick = { onDeletePrice(price) }) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewMarketSheet(existingNames: List<String>, onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    val trimmed = name.trim()
    val duplicate = existingNames.any { it.equals(trimmed, ignoreCase = true) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.markets_new_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.markets_name_label)) },
                isError = duplicate,
                supportingText = { if (duplicate) Text(stringResource(R.string.markets_duplicate)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Button(
                onClick = { onCreate(trimmed) },
                modifier = Modifier.fillMaxWidth(),
                enabled = trimmed.isNotBlank() && !duplicate,
                colors = ButtonDefaults.buttonColors(containerColor = Green)
            ) {
                Text(stringResource(R.string.markets_save_btn))
            }
        }
    }
}
