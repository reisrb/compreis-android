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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rafaelreis.compreis.CompreisApp
import com.rafaelreis.compreis.R
import com.rafaelreis.compreis.data.db.AppDatabase
import com.rafaelreis.compreis.data.db.ShoppingListEntity
import com.rafaelreis.compreis.ui.theme.Green
import com.rafaelreis.compreis.ui.theme.Orange
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ListsViewModel(private val db: AppDatabase) : ViewModel() {
    val lists = db.shoppingListDao().getAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createList(name: String, marketDate: Long?, marketName: String?) = viewModelScope.launch {
        db.shoppingListDao().insert(ShoppingListEntity(name = name, marketDate = marketDate, marketName = marketName?.takeIf { it.isNotBlank() }))
    }

    fun deleteList(list: ShoppingListEntity) = viewModelScope.launch {
        db.shoppingListDao().delete(list)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListsScreen(app: CompreisApp, onListTap: (Long) -> Unit) {
    val vm: ListsViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ListsViewModel(app.db) as T
    })
    val lists by vm.lists.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    val active = lists.filter { !it.finalized }
    val finalized = lists.filter { it.finalized }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }, containerColor = Green) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.new_list_title), tint = Color.White)
            }
        }
    ) { padding ->
        if (lists.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(64.dp), tint = Green.copy(alpha = 0.4f))
                    Text(stringResource(R.string.lists_empty_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.lists_empty_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (active.isNotEmpty()) {
                    item { Text(stringResource(R.string.lists_section_active), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 4.dp)) }
                    items(active, key = { it.id }) { list ->
                        ListCard(list = list, onClick = { onListTap(list.id) }, onDelete = { vm.deleteList(list) })
                    }
                }
                if (finalized.isNotEmpty()) {
                    item { Text(stringResource(R.string.lists_section_done), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) }
                    items(finalized, key = { it.id }) { list ->
                        ListCard(list = list, onClick = { onListTap(list.id) }, onDelete = { vm.deleteList(list) })
                    }
                }
            }
        }
    }

    if (showDialog) NewListDialog(onDismiss = { showDialog = false }, onCreate = { name, date, marketName -> vm.createList(name, date, marketName); showDialog = false })
}

@Composable
private fun ListCard(list: ShoppingListEntity, onClick: () -> Unit, onDelete: () -> Unit) {
    val fmt = SimpleDateFormat("dd/MM · HH:mm", Locale.getDefault())
    val iconColor = when {
        list.finalized -> MaterialTheme.colorScheme.onSurfaceVariant
        list.inProgress -> Orange
        else -> Green
    }
    val bgColor = when {
        list.finalized -> MaterialTheme.colorScheme.surfaceVariant
        list.inProgress -> Orange.copy(alpha = 0.15f)
        else -> Green.copy(alpha = 0.15f)
    }
    val icon = if (list.finalized) Icons.Default.CheckCircle else Icons.Default.ShoppingCart

    Card(Modifier.fillMaxWidth().clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = MaterialTheme.shapes.large, color = bgColor, modifier = Modifier.size(42.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(list.name, fontWeight = FontWeight.SemiBold)
                val sub = buildList {
                    list.marketName?.let { add(it) }
                    list.marketDate?.let { add(fmt.format(Date(it))) }
                }
                if (sub.isNotEmpty()) Text(sub.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.list_delete_desc), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewListDialog(onDismiss: () -> Unit, onCreate: (String, Long?, String?) -> Unit) {
    var name by remember { mutableStateOf("") }
    var marketName by remember { mutableStateOf("") }
    var useDate by remember { mutableStateOf(false) }
    var marketDate by remember { mutableStateOf(System.currentTimeMillis()) }
    val defaultName = stringResource(R.string.list_default_name)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.new_list_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.new_list_name_label)) }, placeholder = { Text(stringResource(R.string.new_list_name_hint)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = marketName, onValueChange = { marketName = it }, label = { Text(stringResource(R.string.new_list_market_label)) }, placeholder = { Text(stringResource(R.string.new_list_market_hint)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.new_list_set_date), Modifier.weight(1f))
                Switch(checked = useDate, onCheckedChange = { useDate = it }, colors = SwitchDefaults.colors(checkedThumbColor = Green, checkedTrackColor = Green.copy(alpha = 0.3f)))
            }
            Button(onClick = { onCreate(name.ifBlank { defaultName }, if (useDate) marketDate else null, marketName) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Green)) {
                Text(stringResource(R.string.new_list_create_btn))
            }
        }
    }
}
