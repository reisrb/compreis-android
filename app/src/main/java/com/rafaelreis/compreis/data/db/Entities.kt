package com.rafaelreis.compreis.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_lists")
data class ShoppingListEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "nome") val name: String,
    @ColumnInfo(name = "dataMercado") val marketDate: Long? = null,
    @ColumnInfo(name = "criadaEm") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "finalizadaEm") val finalizedAt: Long? = null,
    @ColumnInfo(name = "finalizada") val finalized: Boolean = false,
    val isTemplate: Boolean = false,
    val isPredefined: Boolean = false,
    @ColumnInfo(name = "totalPago") val totalPaid: Double? = null,
    val inProgress: Boolean = false,
    val marketName: String? = null
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
    @ColumnInfo(name = "listaId") val listId: Long,
    @ColumnInfo(name = "nome") val name: String,
    @ColumnInfo(name = "preco") val price: Double,
    @ColumnInfo(name = "unidade") val unit: String,
    @ColumnInfo(name = "quantidade") val quantity: Double,
    @ColumnInfo(name = "categoria") val category: String = "outros",
    @ColumnInfo(name = "pegou") val picked: Boolean = false
) {
    val total: Double get() = price * quantity
}

@Entity(tableName = "product_history")
data class ProductHistoryEntity(
    @PrimaryKey @ColumnInfo(name = "nome") val name: String,
    @ColumnInfo(name = "preco") val price: Double,
    @ColumnInfo(name = "unidade") val unit: String,
    @ColumnInfo(name = "categoria") val category: String = "outros"
)

@Entity(tableName = "markets")
data class MarketEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "market_prices",
    indices = [Index("productName"), Index("marketName")]
)
data class MarketPriceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productName: String,
    val marketName: String,
    val price: Double,
    val unit: String,
    val updatedAt: Long = System.currentTimeMillis()
)
