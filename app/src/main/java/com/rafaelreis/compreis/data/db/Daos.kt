package com.rafaelreis.compreis.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingListDao {
    @Query("SELECT * FROM shopping_lists ORDER BY criadaEm DESC")
    fun getAll(): Flow<List<ShoppingListEntity>>

    @Query("SELECT * FROM shopping_lists WHERE isTemplate = 1 ORDER BY criadaEm ASC")
    fun getTemplates(): Flow<List<ShoppingListEntity>>

    @Query("SELECT * FROM shopping_lists WHERE isPredefined = 1 ORDER BY criadaEm ASC")
    fun getPredefined(): Flow<List<ShoppingListEntity>>

    @Insert fun insert(list: ShoppingListEntity): Long
    @Update fun update(list: ShoppingListEntity)
    @Delete fun delete(list: ShoppingListEntity)
}

@Dao
interface ItemDao {
    @Query("SELECT * FROM items ORDER BY nome ASC")
    fun getAll(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE listaId = :listId ORDER BY nome ASC")
    fun getByList(listId: Long): Flow<List<ItemEntity>>

    @Query("UPDATE items SET pegou = :picked WHERE id = :id")
    suspend fun togglePicked(id: Long, picked: Boolean)

    @Insert fun insert(item: ItemEntity): Long
    @Update fun update(item: ItemEntity)
    @Delete fun delete(item: ItemEntity)
}

@Dao
interface ProductHistoryDao {
    @Query("SELECT * FROM product_history ORDER BY nome ASC")
    fun getAll(): Flow<List<ProductHistoryEntity>>

    @Query("SELECT * FROM product_history WHERE nome LIKE '%' || :query || '%' LIMIT 4")
    suspend fun search(query: String): List<ProductHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(product: ProductHistoryEntity)

    @Delete
    suspend fun delete(product: ProductHistoryEntity)
}

@Dao
interface MarketDao {
    @Query("SELECT * FROM markets ORDER BY name ASC")
    fun getAll(): Flow<List<MarketEntity>>

    @Query("SELECT name FROM markets ORDER BY name ASC")
    suspend fun getAllNames(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(market: MarketEntity)

    @Delete
    suspend fun delete(market: MarketEntity)
}

@Dao
interface MarketPriceDao {
    @Query("SELECT * FROM market_prices ORDER BY productName ASC")
    fun getAll(): Flow<List<MarketPriceEntity>>

    @Query("SELECT * FROM market_prices WHERE productName = :productName ORDER BY price ASC")
    suspend fun getByProduct(productName: String): List<MarketPriceEntity>

    @Query("SELECT * FROM market_prices WHERE marketName = :marketName ORDER BY productName ASC")
    suspend fun getByMarket(marketName: String): List<MarketPriceEntity>

    @Query("SELECT * FROM market_prices WHERE productName = :productName AND marketName = :marketName LIMIT 1")
    suspend fun get(productName: String, marketName: String): MarketPriceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(marketPrice: MarketPriceEntity)
}
