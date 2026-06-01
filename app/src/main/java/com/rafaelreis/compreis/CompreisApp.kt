package com.rafaelreis.compreis

import android.app.Application
import com.rafaelreis.compreis.data.LocalBackupService
import com.rafaelreis.compreis.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CompreisApp : Application() {
    val db by lazy { AppDatabase.get(this) }

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch {
            LocalBackupService.restoreIfEmpty(applicationContext, db)
            LocalBackupService.write(applicationContext, db)
        }
    }
}
