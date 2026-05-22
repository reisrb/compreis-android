package com.rafaelreis.compreis.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingListDao {
    @Query("SELECT * FROM shopping_lists ORDER BY criadaEm DESC")
    fun getAll(): Flow<List<ShoppingListEntity>>

    @Insert fun insert(list: ShoppingListEntity): Long
    @Update fun update(list: ShoppingListEntity)
    @Delete fun delete(list: ShoppingListEntity)
}

@Dao
interface ItemDao {
    @Query("SELECT * FROM items ORDER BY nome ASC")
    fun getAll(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE listaId = :listaId ORDER BY nome ASC")
    fun getByList(listaId: Long): Flow<List<ItemEntity>>

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
}
