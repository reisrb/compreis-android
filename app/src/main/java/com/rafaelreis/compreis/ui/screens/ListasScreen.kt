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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ListasViewModel(private val db: AppDatabase) : ViewModel() {
    val listas = db.shoppingListDao().getAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun criarLista(nome: String, dataMercado: Long?) = viewModelScope.launch {
        db.shoppingListDao().insert(ShoppingListEntity(nome = nome, dataMercado = dataMercado))
    }

    fun deletarLista(lista: ShoppingListEntity) = viewModelScope.launch {
        db.shoppingListDao().delete(lista)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListasScreen(app: CompreisApp, onListaTap: (Long) -> Unit) {
    val vm: ListasViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ListasViewModel(app.db) as T
    })
    val listas by vm.listas.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    val ativas = listas.filter { !it.finalizada }
    val finalizadas = listas.filter { it.finalizada }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }, containerColor = Green) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.new_list_title), tint = androidx.compose.ui.graphics.Color.White)
            }
        }
    ) { padding ->
        if (listas.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(64.dp), tint = Green.copy(alpha = 0.4f))
                    Text(stringResource(R.string.lists_empty_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.lists_empty_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (ativas.isNotEmpty()) {
                    item { Text(stringResource(R.string.lists_section_active), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 4.dp)) }
                    items(ativas, key = { it.id }) { lista ->
                        ListaCard(lista = lista, onClick = { onListaTap(lista.id) }, onDelete = { vm.deletarLista(lista) })
                    }
                }
                if (finalizadas.isNotEmpty()) {
                    item { Text(stringResource(R.string.lists_section_done), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) }
                    items(finalizadas, key = { it.id }) { lista ->
                        ListaCard(lista = lista, onClick = { onListaTap(lista.id) }, onDelete = { vm.deletarLista(lista) })
                    }
                }
            }
        }
    }

    if (showDialog) NovaListaDialog(onDismiss = { showDialog = false }, onCreate = { nome, data -> vm.criarLista(nome, data); showDialog = false })
}

@Composable
private fun ListaCard(lista: ShoppingListEntity, onClick: () -> Unit, onDelete: () -> Unit) {
    val fmt = SimpleDateFormat("dd/MM · HH:mm", Locale.getDefault())
    Card(Modifier.fillMaxWidth().clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = MaterialTheme.shapes.large, color = if (lista.finalizada) MaterialTheme.colorScheme.surfaceVariant else Green.copy(alpha = 0.15f), modifier = Modifier.size(42.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(if (lista.finalizada) Icons.Default.CheckCircle else Icons.Default.ShoppingCart, contentDescription = null, tint = if (lista.finalizada) MaterialTheme.colorScheme.onSurfaceVariant else Green, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(lista.nome, fontWeight = FontWeight.SemiBold)
                val sub = buildList {
                    lista.dataMercado?.let { add(fmt.format(Date(it))) }
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
private fun NovaListaDialog(onDismiss: () -> Unit, onCreate: (String, Long?) -> Unit) {
    var nome by remember { mutableStateOf("") }
    var usarData by remember { mutableStateOf(false) }
    var dataMercado by remember { mutableStateOf(System.currentTimeMillis()) }
    val defaultName = stringResource(R.string.list_default_name)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.new_list_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedTextField(value = nome, onValueChange = { nome = it }, label = { Text(stringResource(R.string.new_list_name_label)) }, placeholder = { Text(stringResource(R.string.new_list_name_hint)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.new_list_set_date), Modifier.weight(1f))
                Switch(checked = usarData, onCheckedChange = { usarData = it }, colors = SwitchDefaults.colors(checkedThumbColor = Green, checkedTrackColor = Green.copy(alpha = 0.3f)))
            }
            Button(onClick = { onCreate(nome.ifBlank { defaultName }, if (usarData) dataMercado else null) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Green)) {
                Text(stringResource(R.string.new_list_create_btn))
            }
        }
    }
}
