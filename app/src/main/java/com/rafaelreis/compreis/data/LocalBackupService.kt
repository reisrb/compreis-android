package com.rafaelreis.compreis.data

import android.content.Context
import com.rafaelreis.compreis.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Keeps a local JSON snapshot of the user's data so nothing is lost across app
 * updates. On launch we restore it if the database came back without real
 * (non-template) lists, then refresh the snapshot from the current data.
 */
object LocalBackupService {
    private const val FILE_NAME = "compreis_backup.json"

    private fun backupFile(context: Context) = File(context.filesDir, FILE_NAME)

    suspend fun restoreIfEmpty(context: Context, db: AppDatabase) = withContext(Dispatchers.IO) {
        val realLists = db.shoppingListDao().getAll().first().filter { !it.isTemplate }
        val file = backupFile(context)
        if (realLists.isEmpty() && file.exists()) {
            val json = file.readText()
            if (json.isNotBlank()) ExportService.importJSON(db, json)
        }
    }

    suspend fun write(context: Context, db: AppDatabase) = withContext(Dispatchers.IO) {
        val json = ExportService.exportJSON(db)
        backupFile(context).writeText(json)
    }
}
