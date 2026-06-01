package com.rafaelreis.compreis

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.rafaelreis.compreis.data.LocalBackupService
import com.rafaelreis.compreis.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CompreisApp : Application() {
    val db by lazy { AppDatabase.get(this) }
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        scope.launch {
            LocalBackupService.restoreIfEmpty(applicationContext, db)
            LocalBackupService.write(applicationContext, db)
        }
        // Refresh the schema-independent local backup whenever the app goes to background
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                scope.launch { LocalBackupService.write(applicationContext, db) }
            }
        })
    }
}
