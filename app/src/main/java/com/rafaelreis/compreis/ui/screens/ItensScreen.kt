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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rafaelreis.compreis.CompreisApp
import com.rafaelreis.compreis.data.db.AppDatabase
import com.rafaelreis.compreis.data.db.ItemEntity
import com.rafaelreis.compreis.data.db.ProductHistoryEntity
import com.rafaelreis.compreis.data.db.ShoppingListEntity
import com.rafaelreis.compreis.ui.theme.Green
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

fun Double.brl(): String = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(this)

class ItensViewModel(private val db: AppDatabase, val listaId: Long) : ViewModel() {
    val itens = db.itemDao().getByList(listaId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val lista = db.shoppingListDao().getAll().map { it.find { l -> l.id == listaId } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun addItem(nome: String, preco: Double, unidade: String, quantidade: Double) = viewModelScope.launch {
        db.itemDao().insert(ItemEntity(listaId = listaId, nome = nome, preco = preco, unidade = unidade, quantidade = quantidade))
        db.productHistoryDao().upsert(ProductHistoryEntity(nome = nome, preco = preco, unidade = unidade))
    }

    fun updateItem(item: ItemEntity, nome: String, preco: Double, unidade: String, quantidade: Double) = viewModelScope.launch {
        db.itemDao().update(item.copy(nome = nome, preco = preco, unidade = unidade, quantidade = quantidade))
        db.productHistoryDao().upsert(ProductHistoryEntity(nome = nome, preco = preco, unidade = unidade))
    }

    fun deleteItem(item: ItemEntity) = viewModelScope.launch { db.itemDao().delete(item) }

    fun finalizar(copiar: Boolean) = viewModelScope.launch {
        val l = lista.value ?: return@launch
        db.shoppingListDao().update(l.copy(finalizada = true, finalizadaEm = System.currentTimeMillis()))
        if (copiar) {
            val novaId = db.shoppingListDao().insert(ShoppingListEntity(nome = l.nome))
            itens.value.forEach { db.itemDao().insert(it.copy(id = 0, listaId = novaId)) }
        }
    }

    suspend fun buscarSugestoes(query: String) = if (query.length >= 2) db.productHistoryDao().search(query) else emptyList()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItensScreen(app: CompreisApp, listaId: Long, onBack: () -> Unit) {
    val vm: ItensViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ItensViewModel(app.db, listaId) as T
    })
    val itens by vm.itens.collectAsState()
    val lista by vm.lista.collectAsState()
    val total = itens.sumOf { it.total }
    var showAdd by remember { mutableStateOf(false) }
    var editando by remember { mutableStateOf<ItemEntity?>(null) }
    var showFinalizar by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(lista?.nome ?: "", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Voltar") } },
                actions = {
                    if (itens.isNotEmpty() && lista?.finalizada == false) {
                        TextButton(onClick = { showFinalizar = true }) { Text("Finalizar", color = Green, fontWeight = FontWeight.SemiBold) }
                    }
                }
            )
        },
        floatingActionButton = {
            if (lista?.finalizada == false) {
                FloatingActionButton(onClick = { showAdd = true }, containerColor = Green) {
                    Icon(Icons.Default.Add, contentDescription = "Adicionar", tint = Color.White)
                }
            }
        },
        bottomBar = {
            if (itens.isNotEmpty()) {
                Surface(tonalElevation = 8.dp) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("${itens.size} ${if (itens.size == 1) "item" else "itens"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Total estimado", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                        Text(total.brl(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Green)
                    }
                }
            }
        }
    ) { padding ->
        if (itens.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.ShoppingCart, null, Modifier.size(64.dp), tint = Green.copy(alpha = 0.4f))
                    Text("Lista vazia", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("Toque em + para adicionar produtos", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(itens, key = { it.id }) { item ->
                    ItemCard(item = item, onTap = { if (lista?.finalizada == false) editando = item }, onDelete = { vm.deleteItem(item) })
                }
            }
        }
    }

    if (showAdd) AdicionarItemSheet(vm = vm, item = null, onDismiss = { showAdd = false }, onSave = { nome, preco, unidade, qtd -> vm.addItem(nome, preco, unidade, qtd); showAdd = false })
    editando?.let { item -> AdicionarItemSheet(vm = vm, item = item, onDismiss = { editando = null }, onSave = { nome, preco, unidade, qtd -> vm.updateItem(item, nome, preco, unidade, qtd); editando = null }) }
    if (showFinalizar) FinalizarSheet(lista = lista, total = total, onDismiss = { showFinalizar = false }, onConfirm = { copiar -> vm.finalizar(copiar); showFinalizar = false; onBack() })
}

@Composable
private fun ItemCard(item: ItemEntity, onTap: () -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth(), onClick = onTap, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = MaterialTheme.shapes.large, color = Green.copy(alpha = 0.15f), modifier = Modifier.size(42.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.ShoppingCart, null, tint = Green, modifier = Modifier.size(20.dp)) }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.nome, fontWeight = FontWeight.SemiBold)
                val qtdLabel = if (item.unidade == "kg") "%,.3f kg".format(item.quantidade) else "${item.quantidade.toInt()} un"
                Text("${item.preco.brl()}/${item.unidade} · $qtdLabel", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(item.total.brl(), fontWeight = FontWeight.Bold, color = Green)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdicionarItemSheet(vm: ItensViewModel, item: ItemEntity?, onDismiss: () -> Unit, onSave: (String, Double, String, Double) -> Unit) {
    var nome by remember { mutableStateOf(item?.nome ?: "") }
    var precoText by remember { mutableStateOf(item?.preco?.let { "%.2f".format(it).replace(".", ",") } ?: "") }
    var unidade by remember { mutableStateOf(item?.unidade ?: "un") }
    var quantidadeInt by remember { mutableIntStateOf(item?.quantidade?.toInt() ?: 1) }
    var pesoGramas by remember { mutableIntStateOf(item?.takeIf { it.unidade == "kg" }?.let { (it.quantidade * 1000).toInt() } ?: 0) }
    var pesoDisplay by remember { mutableStateOf(item?.takeIf { it.unidade == "kg" }?.let { "%d,%03d".format((it.quantidade * 1000).toInt() / 1000, (it.quantidade * 1000).toInt() % 1000) } ?: "0,000") }
    var sugestoes by remember { mutableStateOf<List<ProductHistoryEntity>>(emptyList()) }
    val scope = rememberCoroutineScope()

    val precoDouble = precoText.replace(",", ".").toDoubleOrNull() ?: 0.0
    val qtdDouble = if (unidade == "kg") pesoGramas / 1000.0 else quantidadeInt.toDouble()
    val isValid = nome.isNotBlank() && precoDouble > 0 && (unidade == "un" || pesoGramas > 0)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (item == null) "Novo item" else "Editar item", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            OutlinedTextField(value = nome, onValueChange = { nome = it; scope.launch { sugestoes = vm.buscarSugestoes(it) } }, label = { Text("Nome do produto") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            if (sugestoes.isNotEmpty()) {
                sugestoes.forEach { s ->
                    ListItem(headlineContent = { Text(s.nome) }, supportingContent = { Text("${s.preco.brl()}/${s.unidade}") }, trailingContent = { Icon(Icons.Default.NorthWest, null, tint = Green) }, modifier = Modifier.clickable { nome = s.nome; precoText = "%.2f".format(s.preco).replace(".", ","); unidade = s.unidade; sugestoes = emptyList() })
                }
                HorizontalDivider()
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = precoText, onValueChange = { precoText = it }, label = { Text("Preço") }, prefix = { Text("R$") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                Column(Modifier.weight(1f)) {
                    Text("Unidade", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("un", "kg").forEach { u ->
                            FilterChip(selected = unidade == u, onClick = { unidade = u }, label = { Text(if (u == "un") "Por unidade" else "Por kg") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Green.copy(alpha = 0.2f), selectedLabelColor = Green))
                        }
                    }
                }
            }

            if (unidade == "kg") {
                OutlinedTextField(
                    value = pesoDisplay,
                    onValueChange = { newVal ->
                        val digits = newVal.filter { it.isDigit() }.take(7)
                        val n = digits.toIntOrNull() ?: 0
                        pesoGramas = n
                        val formatted = "%d,%03d".format(n / 1000, n % 1000)
                        if (pesoDisplay != formatted) pesoDisplay = formatted
                    },
                    label = { Text("Peso — ${pesoGramas}g") },
                    suffix = { Text("kg") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Quantidade", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = { if (quantidadeInt > 1) quantidadeInt-- }, enabled = quantidadeInt > 1) { Icon(Icons.Default.RemoveCircle, null, tint = if (quantidadeInt > 1) Green else MaterialTheme.colorScheme.onSurfaceVariant) }
                    Text("$quantidadeInt", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = { quantidadeInt++ }) { Icon(Icons.Default.AddCircle, null, tint = Green) }
                }
            }

            if (isValid) {
                Surface(color = Green.copy(alpha = 0.1f), shape = MaterialTheme.shapes.medium) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total do item", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text((precoDouble * qtdDouble).brl(), fontWeight = FontWeight.Bold, color = Green)
                    }
                }
            }

            Button(onClick = { onSave(nome.trim(), precoDouble, unidade, qtdDouble) }, modifier = Modifier.fillMaxWidth(), enabled = isValid, colors = ButtonDefaults.buttonColors(containerColor = Green)) {
                Text("Salvar")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FinalizarSheet(lista: ShoppingListEntity?, total: Double, onDismiss: () -> Unit, onConfirm: (Boolean) -> Unit) {
    var copiar by remember { mutableStateOf(true) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Finalizar compra", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total da compra"); Text(total.brl(), fontWeight = FontWeight.Bold, color = Green)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Copiar itens para próxima lista", fontWeight = FontWeight.Medium)
                    Text("Mesmos produtos com preços salvos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = copiar, onCheckedChange = { copiar = it }, colors = SwitchDefaults.colors(checkedThumbColor = Green, checkedTrackColor = Green.copy(alpha = 0.3f)))
            }
            Button(onClick = { onConfirm(copiar) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Green)) {
                Text("Confirmar")
            }
        }
    }
}
