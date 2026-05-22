package com.rafaelreis.compreis

import android.app.Application
import com.rafaelreis.compreis.data.db.AppDatabase

class CompreisApp : Application() {
    val db by lazy { AppDatabase.get(this) }
}
