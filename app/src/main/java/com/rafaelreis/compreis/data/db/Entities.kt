package com.rafaelreis.compreis.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_lists")
data class ShoppingListEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val dataMercado: Long? = null,
    val criadaEm: Long = System.currentTimeMillis(),
    val finalizadaEm: Long? = null,
    val finalizada: Boolean = false
)

@Entity(
    tableName = "items",
    foreignKeys = [ForeignKey(
        entity = ShoppingListEntity::class,
        parentColumns = ["id"],
        childColumns = ["listaId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("listaId")]
)
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listaId: Long,
    val nome: String,
    val preco: Double,
    val unidade: String,
    val quantidade: Double
) {
    val total: Double get() = preco * quantidade
}

@Entity(tableName = "product_history")
data class ProductHistoryEntity(
    @PrimaryKey val nome: String,
    val preco: Double,
    val unidade: String
)
