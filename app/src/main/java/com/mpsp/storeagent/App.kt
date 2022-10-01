package com.mpsp.storeagent

import android.app.Application
import androidx.room.Room
import androidx.room.RoomDatabase
import com.airbnb.mvrx.Mavericks
import com.mpsp.storeagent.database.AppDatabase

class App : Application() {
    @Volatile private var DatabaseInstance: AppDatabase? = null

    companion object {
        var AppInstance: App? = null

        fun getInstance(): App {
            return AppInstance!!
        }
    }


    override fun onCreate() {
        super.onCreate()
        Mavericks.initialize(this)
        AppInstance = this
    }


    fun getDatabase(): AppDatabase {
        synchronized(this) {
            if(DatabaseInstance == null) {
                DatabaseInstance = Room.databaseBuilder(
                    this,
                    AppDatabase::class.java, "storeagent"
                ).build()
            }

            return DatabaseInstance!!
        }
    }
}