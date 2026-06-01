package com.rafaelreis.compreis.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rafaelreis.compreis.CompreisApp
import com.rafaelreis.compreis.R
import com.rafaelreis.compreis.data.ExportService
import com.rafaelreis.compreis.data.db.AppDatabase
import com.rafaelreis.compreis.ui.theme.Green
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ProfileViewModel(private val db: AppDatabase) : ViewModel() {

    suspend fun exportJSON(): String = ExportService.exportJSON(db)

    suspend fun importJSON(json: String): Pair<Int, Int> = ExportService.importJSON(db, json)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(app: CompreisApp) {
    val vm: ProfileViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ProfileViewModel(app.db) as T
    })
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var loading by remember { mutableStateOf(false) }

    val importSuccessTemplate = stringResource(R.string.profile_import_success)
    val importError = stringResource(R.string.profile_import_error)
    val readError = stringResource(R.string.profile_read_error)
    val exportError = stringResource(R.string.profile_export_error)
    val exportTitle = stringResource(R.string.profile_export_title)

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                loading = true
                try {
                    val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
                    val (lists, items) = vm.importJSON(json)
                    if (lists > 0) {
                        snackbarHostState.showSnackbar(importSuccessTemplate.format(lists, items))
                    } else {
                        snackbarHostState.showSnackbar(importError)
                    }
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar(readError)
                } finally {
                    loading = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_title), fontWeight = FontWeight.Bold) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            Text(
                stringResource(R.string.profile_section_data),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ProfileActionCard(
                icon = Icons.Default.Upload,
                title = stringResource(R.string.profile_export_title),
                subtitle = stringResource(R.string.profile_export_subtitle),
                onClick = {
                    scope.launch {
                        loading = true
                        try {
                            val json = vm.exportJSON()
                            val fmt = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                            val fileName = "compreis_${fmt.format(Date())}.json"
                            val file = File(context.cacheDir, fileName)
                            file.writeText(json)
                            val fileUri = androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.provider",
                                file
                            )
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_STREAM, fileUri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, exportTitle))
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar(exportError)
                        } finally {
                            loading = false
                        }
                    }
                }
            )

            ProfileActionCard(
                icon = Icons.Default.Download,
                title = stringResource(R.string.profile_import_title),
                subtitle = stringResource(R.string.profile_import_subtitle),
                onClick = { importLauncher.launch("application/json") }
            )

            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.profile_section_storage),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = Green.copy(alpha = 0.15f),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Storage, null, tint = Green, modifier = Modifier.size(20.dp))
                        }
                    }
                    Column {
                        Text(stringResource(R.string.profile_storage_title), fontWeight = FontWeight.Medium)
                        Text(
                            stringResource(R.string.profile_storage_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (loading) {
                Box(Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Green)
                }
            }
        }
    }
}

@Composable
private fun ProfileActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = Green.copy(alpha = 0.15f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = Green, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
