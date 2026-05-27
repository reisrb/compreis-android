package com.rafaelreis.compreis.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE shopping_lists ADD COLUMN isTemplate INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE shopping_lists ADD COLUMN isPredefined INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE shopping_lists ADD COLUMN totalPago REAL")
        db.execSQL("ALTER TABLE items ADD COLUMN categoria TEXT NOT NULL DEFAULT 'outros'")
        db.execSQL("ALTER TABLE items ADD COLUMN pegou INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE product_history ADD COLUMN categoria TEXT NOT NULL DEFAULT 'outros'")
    }
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE shopping_lists ADD COLUMN inProgress INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE shopping_lists ADD COLUMN marketName TEXT")
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS markets (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )"""
        )
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS market_prices (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                productName TEXT NOT NULL,
                marketName TEXT NOT NULL,
                price REAL NOT NULL,
                unit TEXT NOT NULL,
                updatedAt INTEGER NOT NULL
            )"""
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_market_prices_productName ON market_prices (productName)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_market_prices_marketName ON market_prices (marketName)")
    }
}

@Database(
    entities = [ShoppingListEntity::class, ItemEntity::class, ProductHistoryEntity::class, MarketEntity::class, MarketPriceEntity::class],
    version = 3
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun shoppingListDao(): ShoppingListDao
    abstract fun itemDao(): ItemDao
    abstract fun productHistoryDao(): ProductHistoryDao
    abstract fun marketDao(): MarketDao
    abstract fun marketPriceDao(): MarketPriceDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "compreis.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                .also { INSTANCE = it }
        }
    }
}
