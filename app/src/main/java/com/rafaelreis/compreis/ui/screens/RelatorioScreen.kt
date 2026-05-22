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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rafaelreis.compreis.CompreisApp
import com.rafaelreis.compreis.data.db.AppDatabase
import com.rafaelreis.compreis.data.db.ItemEntity
import com.rafaelreis.compreis.data.db.ShoppingListEntity
import com.rafaelreis.compreis.ui.theme.Green
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

data class MesResumo(val label: String, val total: Double, val compras: Int)

class RelatorioViewModel(private val db: AppDatabase) : ViewModel() {
    val listas = db.shoppingListDao().getAll()
        .map { it.filter { l -> l.finalizada } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val itens = db.itemDao().getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

private fun calcularUltimos7Dias(listas: List<ShoppingListEntity>, itens: List<ItemEntity>): Double {
    val limiar = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
    val idsRecentes = listas.filter { (it.finalizadaEm ?: 0) >= limiar }.map { it.id }.toSet()
    return itens.filter { it.listaId in idsRecentes }.sumOf { it.total }
}

private fun calcularMediaMensal(porMes: List<MesResumo>): Double =
    if (porMes.isEmpty()) 0.0 else porMes.sumOf { it.total } / porMes.size

private fun calcularMediaPorCompra(listas: List<ShoppingListEntity>, itens: List<ItemEntity>): Double {
    if (listas.isEmpty()) return 0.0
    return listas.map { l -> itens.filter { it.listaId == l.id }.sumOf { it.total } }.average()
}

private fun agruparPorMes(listas: List<ShoppingListEntity>, itens: List<ItemEntity>): List<MesResumo> {
    val fmt = SimpleDateFormat("MMMM yyyy", Locale("pt", "BR"))
    return listas
        .groupBy { fmt.format(Date(it.finalizadaEm ?: it.id)) }
        .map { (label, grupo) ->
            val ids = grupo.map { it.id }.toSet()
            MesResumo(
                label = label.replaceFirstChar { it.uppercase() },
                total = itens.filter { it.listaId in ids }.sumOf { it.total },
                compras = grupo.size
            )
        }
        .sortedByDescending { it.label }
}

private val mockListas = listOf(
    ShoppingListEntity(id = 1, nome = "Semana 1", finalizada = true, finalizadaEm = System.currentTimeMillis() - 2L * 24 * 3600 * 1000),
    ShoppingListEntity(id = 2, nome = "Churrasco", finalizada = true, finalizadaEm = System.currentTimeMillis() - 10L * 24 * 3600 * 1000),
    ShoppingListEntity(id = 3, nome = "Semana 2", finalizada = true, finalizadaEm = System.currentTimeMillis() - 35L * 24 * 3600 * 1000),
    ShoppingListEntity(id = 4, nome = "Mês passado", finalizada = true, finalizadaEm = System.currentTimeMillis() - 65L * 24 * 3600 * 1000),
    ShoppingListEntity(id = 5, nome = "Há 3 meses", finalizada = true, finalizadaEm = System.currentTimeMillis() - 95L * 24 * 3600 * 1000),
)
private val mockItens = listOf(
    ItemEntity(listaId = 1, nome = "Frango", preco = 12.90, unidade = "kg", quantidade = 2.0),
    ItemEntity(listaId = 1, nome = "Arroz", preco = 5.49, unidade = "un", quantidade = 2.0),
    ItemEntity(listaId = 1, nome = "Feijão", preco = 8.90, unidade = "un", quantidade = 1.0),
    ItemEntity(listaId = 2, nome = "Picanha", preco = 89.90, unidade = "kg", quantidade = 1.5),
    ItemEntity(listaId = 2, nome = "Carvão", preco = 24.90, unidade = "un", quantidade = 2.0),
    ItemEntity(listaId = 2, nome = "Cerveja", preco = 4.99, unidade = "un", quantidade = 12.0),
    ItemEntity(listaId = 3, nome = "Macarrão", preco = 3.29, unidade = "un", quantidade = 3.0),
    ItemEntity(listaId = 3, nome = "Molho", preco = 6.49, unidade = "un", quantidade = 2.0),
    ItemEntity(listaId = 3, nome = "Carne moída", preco = 28.90, unidade = "kg", quantidade = 0.8),
    ItemEntity(listaId = 4, nome = "Leite", preco = 4.89, unidade = "un", quantidade = 6.0),
    ItemEntity(listaId = 4, nome = "Pão", preco = 8.90, unidade = "un", quantidade = 2.0),
    ItemEntity(listaId = 5, nome = "Banana", preco = 3.99, unidade = "kg", quantidade = 1.5),
    ItemEntity(listaId = 5, nome = "Laranja", preco = 5.99, unidade = "kg", quantidade = 2.0),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelatorioScreen(app: CompreisApp) {
    val vm: RelatorioViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T = RelatorioViewModel(app.db) as T
    })
    val listas by vm.listas.collectAsState()
    val itens by vm.itens.collectAsState()
    var showExemplos by remember { mutableStateOf(false) }

    val ultimos7 = calcularUltimos7Dias(listas, itens)
    val porMes = agruparPorMes(listas, itens)
    val mediaMensal = calcularMediaMensal(porMes)
    val mediaPorCompra = calcularMediaPorCompra(listas, itens)
    val vazio = listas.isEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Relatório", fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = { showExemplos = true }) {
                        Text("Exemplos", color = Green, fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        }
    ) { padding ->
        if (vazio) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.BarChart, null, Modifier.size(64.dp), tint = Green.copy(alpha = 0.4f))
                    Text("Sem dados ainda", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("Finalize uma lista para ver o relatório", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { ResumoCards(ultimos7 = ultimos7, mediaMensal = mediaMensal, mediaPorCompra = mediaPorCompra) }
                if (porMes.isNotEmpty()) {
                    item { Text("Por mês", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp)) }
                    items(porMes) { mes -> MesCard(mes = mes) }
                }
            }
        }
    }

    if (showExemplos) ExemplosSheet(onDismiss = { showExemplos = false })
}

@Composable
private fun ResumoCards(ultimos7: Double, mediaMensal: Double, mediaPorCompra: Double) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MetricCard(titulo = "Últimos 7 dias", valor = ultimos7.brl(), icone = Icons.Default.DateRange)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard(titulo = "Média mensal", valor = mediaMensal.brl(), icone = Icons.Default.DateRange, modifier = Modifier.weight(1f))
            MetricCard(titulo = "Média por compra", valor = mediaPorCompra.brl(), icone = Icons.Default.ShoppingCart, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun MetricCard(titulo: String, valor: String, icone: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier.fillMaxWidth()) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = MaterialTheme.shapes.large, color = Green.copy(alpha = 0.15f), modifier = Modifier.size(42.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icone, null, tint = Green, modifier = Modifier.size(20.dp)) }
            }
            Column {
                Text(titulo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(valor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Green)
            }
        }
    }
}

@Composable
private fun MesCard(mes: MesResumo) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(mes.label, fontWeight = FontWeight.SemiBold)
                Text("${mes.compras} ${if (mes.compras == 1) "compra" else "compras"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(mes.total.brl(), fontWeight = FontWeight.Bold, color = Green)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExemplosSheet(onDismiss: () -> Unit) {
    val ultimos7 = calcularUltimos7Dias(mockListas, mockItens)
    val porMes = agruparPorMes(mockListas, mockItens)
    val mediaMensal = calcularMediaMensal(porMes)
    val mediaPorCompra = calcularMediaPorCompra(mockListas, mockItens)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Exemplo de relatório", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Dados fictícios para ilustrar", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
            }
            ResumoCards(ultimos7 = ultimos7, mediaMensal = mediaMensal, mediaPorCompra = mediaPorCompra)
            Text("Por mês", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            porMes.forEach { mes -> MesCard(mes = mes) }
        }
    }
}
