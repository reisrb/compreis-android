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
import com.rafaelreis.compreis.data.db.AppDatabase
import com.rafaelreis.compreis.data.db.ShoppingListEntity
import com.rafaelreis.compreis.ui.theme.Green
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TemplatesViewModel(private val db: AppDatabase) : ViewModel() {
    val templates = db.shoppingListDao().getTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun criarTemplate(nome: String) = viewModelScope.launch {
        db.shoppingListDao().insert(ShoppingListEntity(name = nome, isTemplate = true, isPredefined = false))
    }

    fun deletarTemplate(template: ShoppingListEntity) = viewModelScope.launch {
        db.shoppingListDao().delete(template)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(
    app: CompreisApp,
    onBack: () -> Unit,
    onTemplateAbrir: (Long) -> Unit
) {
    val vm: TemplatesViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T = TemplatesViewModel(app.db) as T
    })
    val templates by vm.templates.collectAsState()
    var showNovo by remember { mutableStateOf(false) }

    val predefined = templates.filter { it.isPredefined }
    val meus = templates.filter { !it.isPredefined }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Templates", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { showNovo = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Novo template", tint = Green)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    "Padrão",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            items(predefined, key = { it.id }) { template ->
                TemplateCard(
                    template = template,
                    onTap = { onTemplateAbrir(template.id) },
                    onDelete = null
                )
            }

            item {
                Text(
                    "Meus templates",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )
            }
            if (meus.isEmpty()) {
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Nenhum template criado",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(meus, key = { it.id }) { template ->
                    TemplateCard(
                        template = template,
                        onTap = { onTemplateAbrir(template.id) },
                        onDelete = { vm.deletarTemplate(template) }
                    )
                }
            }
        }
    }

    if (showNovo) {
        NovoTemplateSheet(
            onDismiss = { showNovo = false },
            onCreate = { nome -> vm.criarTemplate(nome); showNovo = false }
        )
    }
}

@Composable
private fun TemplateCard(
    template: ShoppingListEntity,
    onTap: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Card(
        Modifier.fillMaxWidth().clickable { onTap() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = Green.copy(alpha = 0.15f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.List,
                        contentDescription = null,
                        tint = Green,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(template.name, fontWeight = FontWeight.SemiBold)
                if (template.isPredefined) {
                    Text(
                        "Padrão do sistema",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Deletar template",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NovoTemplateSheet(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var nome by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Novo template", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = nome,
                onValueChange = { nome = it },
                label = { Text("Nome do template") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Button(
                onClick = { if (nome.isNotBlank()) onCreate(nome.trim()) },
                modifier = Modifier.fillMaxWidth(),
                enabled = nome.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Green)
            ) {
                Text("Criar template")
            }
        }
    }
}
