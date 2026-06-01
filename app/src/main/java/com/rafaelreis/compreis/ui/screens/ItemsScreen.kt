package com.rafaelreis.compreis.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rafaelreis.compreis.CompreisApp
import com.rafaelreis.compreis.R
import com.rafaelreis.compreis.data.db.AppDatabase
import com.rafaelreis.compreis.data.db.ItemEntity
import com.rafaelreis.compreis.data.db.MarketPriceEntity
import com.rafaelreis.compreis.data.db.ProductHistoryEntity
import com.rafaelreis.compreis.data.db.ShoppingListEntity
import com.rafaelreis.compreis.ui.theme.Green
import com.rafaelreis.compreis.ui.theme.Orange
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

fun Double.brl(): String = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(this)

class ItemsViewModel(private val db: AppDatabase, val listId: Long) : ViewModel() {
    val items = db.itemDao().getByList(listId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val list = db.shoppingListDao().getAll().map { it.find { l -> l.id == listId } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val marketPrices = db.marketPriceDao().getAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val activeLists = db.shoppingListDao().getAll()
        .map { it.filter { l -> !l.finalized && !l.isTemplate && l.id != listId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addItem(name: String, price: Double, unit: String, quantity: Double) = viewModelScope.launch {
        db.itemDao().insert(ItemEntity(listId = listId, name = name, price = price, unit = unit, quantity = quantity))
        db.productHistoryDao().upsert(ProductHistoryEntity(name = name, price = price, unit = unit))
    }

    fun updateItem(item: ItemEntity, name: String, price: Double, unit: String, quantity: Double) = viewModelScope.launch {
        db.itemDao().update(item.copy(name = name, price = price, unit = unit, quantity = quantity))
        db.productHistoryDao().upsert(ProductHistoryEntity(name = name, price = price, unit = unit))
    }

    fun deleteItem(item: ItemEntity) = viewModelScope.launch { db.itemDao().delete(item) }

    fun togglePicked(item: ItemEntity) = viewModelScope.launch {
        db.itemDao().togglePicked(item.id, !item.picked)
    }

    fun saveMarketPrice(productName: String, marketName: String, price: Double, unit: String) = viewModelScope.launch {
        db.marketPriceDao().upsert(MarketPriceEntity(productName = productName, marketName = marketName, price = price, unit = unit))
    }

    fun finalize(copy: Boolean) = viewModelScope.launch {
        val l = list.value ?: return@launch
        db.shoppingListDao().update(l.copy(finalized = true, finalizedAt = System.currentTimeMillis()))
        if (copy) {
            val newId = db.shoppingListDao().insert(ShoppingListEntity(name = l.name))
            items.value.forEach { db.itemDao().insert(it.copy(id = 0, listId = newId)) }
        }
    }

    fun moveItemToList(item: ItemEntity, targetListId: Long) = viewModelScope.launch {
        db.itemDao().insert(item.copy(id = 0, listId = targetListId, picked = false))
        db.itemDao().delete(item)
    }

    fun createNewListAndMove(item: ItemEntity, listName: String) = viewModelScope.launch {
        val newId = db.shoppingListDao().insert(ShoppingListEntity(name = listName))
        db.itemDao().insert(item.copy(id = 0, listId = newId, picked = false))
        db.itemDao().delete(item)
    }

    suspend fun searchSuggestions(query: String) = if (query.length >= 2) db.productHistoryDao().search(query) else emptyList()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemsScreen(app: CompreisApp, listaId: Long, onBack: () -> Unit) {
    val vm: ItemsViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ItemsViewModel(app.db, listaId) as T
    })
    val items by vm.items.collectAsState()
    val list by vm.list.collectAsState()
    val marketPrices by vm.marketPrices.collectAsState()
    val activeLists by vm.activeLists.collectAsState()
    val total = items.sumOf { it.total }
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ItemEntity?>(null) }
    var confirmingPrice by remember { mutableStateOf<ItemEntity?>(null) }
    var showFinalize by remember { mutableStateOf(false) }
    var movingItem by remember { mutableStateOf<Pair<ItemEntity, MarketPriceEntity>?>(null) }

    val fabColor = if (list?.inProgress == true) Orange else Green

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(list?.name ?: "", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, stringResource(R.string.items_back_desc)) } },
                actions = {
                    if (items.isNotEmpty() && list?.finalized == false) {
                        TextButton(onClick = { showFinalize = true }) { Text(stringResource(R.string.items_finalize_btn), color = fabColor, fontWeight = FontWeight.SemiBold) }
                    }
                }
            )
        },
        floatingActionButton = {
            if (list?.finalized == false) {
                FloatingActionButton(onClick = { showAdd = true }, containerColor = fabColor) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.items_add_desc), tint = Color.White)
                }
            }
        },
        bottomBar = {
            if (items.isNotEmpty()) {
                Surface(tonalElevation = 8.dp) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(pluralStringResource(R.plurals.items_count, items.size, items.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(stringResource(R.string.items_total_label), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                        Text(total.brl(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = fabColor)
                    }
                }
            }
        }
    ) { padding ->
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.ShoppingCart, null, Modifier.size(64.dp), tint = Green.copy(alpha = 0.4f))
                    Text(stringResource(R.string.items_empty_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.items_empty_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items, key = { it.id }) { item ->
                    val pricesForProduct = marketPrices.filter { it.productName == item.name }
                    val currentMarket = list?.marketName
                    val cheaperAlternative = if (currentMarket != null) {
                        pricesForProduct.filter { it.marketName != currentMarket && it.price < item.price }
                            .minByOrNull { it.price }
                    } else null
                    ItemCard(
                        item = item,
                        inProgress = list?.inProgress == true,
                        cheaperAt = cheaperAlternative,
                        onTap = {
                            if (list?.finalized == false) {
                                if (!item.picked) confirmingPrice = item else editing = item
                            }
                        },
                        onTogglePicked = { vm.togglePicked(item) },
                        onDelete = { vm.deleteItem(item) },
                        onChipClick = { cheaperAt -> movingItem = item to cheaperAt }
                    )
                }
            }
        }
    }

    if (showAdd) ItemSheet(vm = vm, item = null, onDismiss = { showAdd = false }, onSave = { name, price, unit, qty ->
        vm.addItem(name, price, unit, qty)
        list?.marketName?.let { vm.saveMarketPrice(name, it, price, unit) }
        showAdd = false
    })
    editing?.let { item ->
        ItemSheet(vm = vm, item = item, onDismiss = { editing = null }, onSave = { name, price, unit, qty ->
            vm.updateItem(item, name, price, unit, qty)
            list?.marketName?.let { vm.saveMarketPrice(name, it, price, unit) }
            editing = null
        })
    }
    confirmingPrice?.let { item ->
        val marketName = list?.marketName
        val lastPrice = marketPrices.find { it.productName == item.name && it.marketName == marketName }
        ConfirmPriceDialog(
            item = item,
            marketName = marketName,
            lastMarketPrice = lastPrice,
            onDismiss = { confirmingPrice = null },
            onConfirm = { price, qty ->
                vm.updateItem(item, item.name, price, item.unit, qty)
                marketName?.let { vm.saveMarketPrice(item.name, it, price, item.unit) }
                vm.togglePicked(item)
                confirmingPrice = null
            },
            onEdit = { confirmingPrice = null; editing = item }
        )
    }
    if (showFinalize) FinalizeSheet(list = list, total = total, onDismiss = { showFinalize = false }, onConfirm = { copy -> vm.finalize(copy); showFinalize = false; onBack() })
    movingItem?.let { (item, cheaperAt) ->
        CheapestMarketSheet(
            item = item,
            cheaperAt = cheaperAt,
            activeLists = activeLists,
            onDismiss = { movingItem = null },
            onMoveToList = { targetListId -> vm.moveItemToList(item, targetListId); movingItem = null },
            onCreateAndMove = { listName -> vm.createNewListAndMove(item, listName); movingItem = null }
        )
    }
}

@Composable
private fun ItemCard(
    item: ItemEntity,
    inProgress: Boolean,
    cheaperAt: MarketPriceEntity?,
    onTap: () -> Unit,
    onTogglePicked: () -> Unit,
    onDelete: () -> Unit,
    onChipClick: (MarketPriceEntity) -> Unit
) {
    val accentColor = if (inProgress) Orange else Green
    Card(Modifier.fillMaxWidth(), onClick = onTap, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = item.picked,
                onCheckedChange = { onTogglePicked() },
                colors = CheckboxDefaults.colors(checkedColor = accentColor)
            )
            Spacer(Modifier.width(4.dp))
            Column(Modifier.weight(1f)) {
                Text(item.name, fontWeight = FontWeight.SemiBold, color = if (item.picked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                val qtdLabel = if (item.unit == "kg") "%,.3f kg".format(item.quantity) else "${item.quantity.toInt()} un"
                Text("${item.price.brl()}/${item.unit} · $qtdLabel", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (cheaperAt != null) {
                    Spacer(Modifier.height(4.dp))
                    SuggestionChip(
                        onClick = { onChipClick(cheaperAt) },
                        label = { Text(stringResource(R.string.items_cheaper_at, cheaperAt.marketName) + " · ${cheaperAt.price.brl()}", style = MaterialTheme.typography.labelSmall) },
                        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Green.copy(alpha = 0.12f), labelColor = Green)
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(item.total.brl(), fontWeight = FontWeight.Bold, color = if (item.picked) MaterialTheme.colorScheme.onSurfaceVariant else accentColor)
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfirmPriceDialog(
    item: ItemEntity,
    marketName: String?,
    lastMarketPrice: MarketPriceEntity?,
    onDismiss: () -> Unit,
    onConfirm: (Double, Double) -> Unit,
    onEdit: () -> Unit
) {
    var priceText by remember { mutableStateOf("%.2f".format(item.price).replace(".", ",")) }
    var quantityInt by remember { mutableIntStateOf(item.quantity.toInt().coerceAtLeast(1)) }

    val priceDouble = priceText.replace(",", ".").toDoubleOrNull() ?: 0.0

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.confirm_price_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(item.name, style = MaterialTheme.typography.bodyLarge)

            if (lastMarketPrice != null && marketName != null) {
                Surface(color = Green.copy(alpha = 0.1f), shape = MaterialTheme.shapes.medium) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.confirm_price_last_here), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(lastMarketPrice.price.brl(), fontWeight = FontWeight.SemiBold, color = Green)
                    }
                }
            }

            OutlinedTextField(
                value = priceText,
                onValueChange = { priceText = it },
                label = { Text(stringResource(R.string.item_price_label)) },
                prefix = { Text("R$") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )

            if (item.unit != "kg") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(stringResource(R.string.item_quantity_label), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = { if (quantityInt > 1) quantityInt-- }, enabled = quantityInt > 1) { Icon(Icons.Default.RemoveCircle, null, tint = if (quantityInt > 1) Green else MaterialTheme.colorScheme.onSurfaceVariant) }
                    Text("$quantityInt", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = { quantityInt++ }) { Icon(Icons.Default.AddCircle, null, tint = Green) }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.item_sheet_edit))
                }
                Button(
                    onClick = { onConfirm(priceDouble, if (item.unit == "kg") item.quantity else quantityInt.toDouble()) },
                    modifier = Modifier.weight(1f),
                    enabled = priceDouble > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = Green)
                ) {
                    Text(stringResource(R.string.confirm_price_btn))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemSheet(vm: ItemsViewModel, item: ItemEntity?, onDismiss: () -> Unit, onSave: (String, Double, String, Double) -> Unit) {
    var name by remember { mutableStateOf(item?.name ?: "") }
    var priceText by remember { mutableStateOf(item?.price?.let { "%.2f".format(it).replace(".", ",") } ?: "") }
    var unit by remember { mutableStateOf(item?.unit ?: "un") }
    var quantityInt by remember { mutableIntStateOf(item?.quantity?.toInt() ?: 1) }
    var weightGrams by remember { mutableIntStateOf(item?.takeIf { it.unit == "kg" }?.let { (it.quantity * 1000).toInt() } ?: 0) }
    var weightDisplay by remember { mutableStateOf(item?.takeIf { it.unit == "kg" }?.let { "%d,%03d".format((it.quantity * 1000).toInt() / 1000, (it.quantity * 1000).toInt() % 1000) } ?: "0,000") }
    var suggestions by remember { mutableStateOf<List<ProductHistoryEntity>>(emptyList()) }
    val scope = rememberCoroutineScope()

    val priceDouble = priceText.replace(",", ".").toDoubleOrNull() ?: 0.0
    val qtyDouble = if (unit == "kg") weightGrams / 1000.0 else quantityInt.toDouble()
    val isValid = name.isNotBlank()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(if (item == null) R.string.item_sheet_new else R.string.item_sheet_edit), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            OutlinedTextField(value = name, onValueChange = { name = it; scope.launch { suggestions = vm.searchSuggestions(it) } }, label = { Text(stringResource(R.string.item_name_label)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            if (suggestions.isNotEmpty()) {
                suggestions.forEach { s ->
                    ListItem(headlineContent = { Text(s.name) }, supportingContent = { Text("${s.price.brl()}/${s.unit}") }, trailingContent = { Icon(Icons.Default.NorthWest, null, tint = Green) }, modifier = Modifier.clickable { name = s.name; priceText = "%.2f".format(s.price).replace(".", ","); unit = s.unit; suggestions = emptyList() })
                }
                HorizontalDivider()
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = priceText, onValueChange = { priceText = it }, label = { Text(stringResource(R.string.item_price_label)) }, prefix = { Text("R$") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.item_unit_label), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("un", "kg").forEach { u ->
                            FilterChip(selected = unit == u, onClick = { unit = u }, label = { Text(stringResource(if (u == "un") R.string.item_unit_each else R.string.item_unit_kg)) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Green.copy(alpha = 0.2f), selectedLabelColor = Green))
                        }
                    }
                }
            }

            if (unit == "kg") {
                OutlinedTextField(
                    value = weightDisplay,
                    onValueChange = { newVal ->
                        val digits = newVal.filter { it.isDigit() }.take(7)
                        val n = digits.toIntOrNull() ?: 0
                        weightGrams = n
                        val formatted = "%d,%03d".format(n / 1000, n % 1000)
                        if (weightDisplay != formatted) weightDisplay = formatted
                    },
                    label = { Text(stringResource(R.string.item_weight_label, weightGrams)) },
                    suffix = { Text("kg") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(stringResource(R.string.item_quantity_label), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = { if (quantityInt > 1) quantityInt-- }, enabled = quantityInt > 1) { Icon(Icons.Default.RemoveCircle, null, tint = if (quantityInt > 1) Green else MaterialTheme.colorScheme.onSurfaceVariant) }
                    Text("$quantityInt", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = { quantityInt++ }) { Icon(Icons.Default.AddCircle, null, tint = Green) }
                }
            }

            if (isValid) {
                Surface(color = Green.copy(alpha = 0.1f), shape = MaterialTheme.shapes.medium) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.item_total_label), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text((priceDouble * qtyDouble).brl(), fontWeight = FontWeight.Bold, color = Green)
                    }
                }
            }

            Button(onClick = { onSave(name.trim(), priceDouble, unit, qtyDouble) }, modifier = Modifier.fillMaxWidth(), enabled = isValid, colors = ButtonDefaults.buttonColors(containerColor = Green)) {
                Text(stringResource(R.string.item_save_btn))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FinalizeSheet(list: ShoppingListEntity?, total: Double, onDismiss: () -> Unit, onConfirm: (Boolean) -> Unit) {
    var copy by remember { mutableStateOf(true) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.finalize_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.finalize_total_label)); Text(total.brl(), fontWeight = FontWeight.Bold, color = Green)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.finalize_copy_title), fontWeight = FontWeight.Medium)
                    Text(stringResource(R.string.finalize_copy_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = copy, onCheckedChange = { copy = it }, colors = SwitchDefaults.colors(checkedThumbColor = Green, checkedTrackColor = Green.copy(alpha = 0.3f)))
            }
            Button(onClick = { onConfirm(copy) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Green)) {
                Text(stringResource(R.string.finalize_confirm_btn))
            }
        }
    }
}
