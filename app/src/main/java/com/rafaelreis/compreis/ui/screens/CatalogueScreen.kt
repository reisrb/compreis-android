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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rafaelreis.compreis.CompreisApp
import com.rafaelreis.compreis.R
import com.rafaelreis.compreis.data.db.AppDatabase
import com.rafaelreis.compreis.data.db.MarketPriceEntity
import com.rafaelreis.compreis.data.db.ProductHistoryEntity
import com.rafaelreis.compreis.ui.theme.Green
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CatalogueViewModel(private val db: AppDatabase) : ViewModel() {
    val products = db.productHistoryDao().getAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val marketPrices = db.marketPriceDao().getAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteProduct(product: ProductHistoryEntity) = viewModelScope.launch {
        db.productHistoryDao().delete(product)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogueScreen(app: CompreisApp) {
    val vm: CatalogueViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T = CatalogueViewModel(app.db) as T
    })
    val products by vm.products.collectAsState()
    val marketPrices by vm.marketPrices.collectAsState()
    var query by remember { mutableStateOf("") }

    val filtered = if (query.isBlank()) products else products.filter { it.name.contains(query, ignoreCase = true) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.catalogue_title), fontWeight = FontWeight.Bold) })
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.catalogue_search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )
            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.List, null, Modifier.size(64.dp), tint = Green.copy(alpha = 0.4f))
                        Text(stringResource(R.string.catalogue_empty_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.catalogue_empty_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filtered, key = { it.name }) { product ->
                        val prices = marketPrices.filter { it.productName == product.name }.sortedBy { it.price }
                        ProductCard(product = product, marketPrices = prices)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductCard(product: ProductHistoryEntity, marketPrices: List<MarketPriceEntity>) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { if (marketPrices.isNotEmpty()) expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = MaterialTheme.shapes.large, color = Green.copy(alpha = 0.15f), modifier = Modifier.size(42.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.ShoppingCart, null, tint = Green, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(product.name, fontWeight = FontWeight.SemiBold)
                    Text("${product.price.brl()}/${product.unit}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (marketPrices.isNotEmpty()) {
                    Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (expanded && marketPrices.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                marketPrices.forEachIndexed { index, mp ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (index == 0) {
                            Icon(Icons.Default.Star, null, tint = Green, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                        } else {
                            Spacer(Modifier.width(18.dp))
                        }
                        Text(mp.marketName, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        Text(mp.price.brl(), fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal, color = if (index == 0) Green else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (marketPrices.size >= 2) {
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.catalogue_cheapest) + ": ${marketPrices.first().marketName}", style = MaterialTheme.typography.labelSmall, color = Green)
                }
            }
        }
    }
}
