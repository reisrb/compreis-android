package com.rafaelreis.compreis.data

import android.content.Context
import com.rafaelreis.compreis.data.db.AppDatabase
import com.rafaelreis.compreis.data.db.ItemEntity
import com.rafaelreis.compreis.data.db.ShoppingListEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

data class ProductBase(val nome: String, val categoria: Categoria, val unidade: String)

private val essencialItems = listOf(
    ProductBase("Papel higiênico", Categoria.HIGIENE, "un"),
    ProductBase("Cotonete", Categoria.HIGIENE, "un"),
    ProductBase("Algodão", Categoria.HIGIENE, "un"),
    ProductBase("Shampoo", Categoria.HIGIENE, "un"),
    ProductBase("Condicionador", Categoria.HIGIENE, "un"),
    ProductBase("Pasta de dente", Categoria.HIGIENE, "un"),
    ProductBase("Sabonete líquido", Categoria.HIGIENE, "un"),
    ProductBase("Desengordurante", Categoria.LIMPEZA, "un"),
    ProductBase("Detergente", Categoria.LIMPEZA, "un"),
    ProductBase("Desinfetante", Categoria.LIMPEZA, "un"),
    ProductBase("Água sanitária", Categoria.LIMPEZA, "un"),
    ProductBase("Bucha de cozinha", Categoria.LIMPEZA, "un"),
    ProductBase("Bucha de banho", Categoria.LIMPEZA, "un"),
    ProductBase("Leite", Categoria.LATICINIOS, "un"),
    ProductBase("Creme de leite", Categoria.LATICINIOS, "un"),
    ProductBase("Carne moída", Categoria.CARNES, "kg"),
    ProductBase("Frango", Categoria.CARNES, "kg"),
    ProductBase("Bife de fígado", Categoria.CARNES, "kg"),
    ProductBase("Feijão", Categoria.MERCEARIA, "un"),
    ProductBase("Arroz", Categoria.MERCEARIA, "un"),
    ProductBase("Óleo de soja", Categoria.MERCEARIA, "un"),
    ProductBase("Vinagre", Categoria.MERCEARIA, "un"),
    ProductBase("Macarrão", Categoria.MERCEARIA, "un"),
    ProductBase("Molho de tomate", Categoria.MERCEARIA, "un"),
    ProductBase("Extrato de tomate", Categoria.MERCEARIA, "un"),
    ProductBase("Ketchup", Categoria.MERCEARIA, "un"),
    ProductBase("Mostarda", Categoria.MERCEARIA, "un"),
    ProductBase("Café", Categoria.MERCEARIA, "un"),
    ProductBase("Chocolate em pó", Categoria.MERCEARIA, "un"),
    ProductBase("Sal", Categoria.MERCEARIA, "un"),
    ProductBase("Chimichurri", Categoria.MERCEARIA, "un"),
    ProductBase("Alho em pó", Categoria.MERCEARIA, "un"),
    ProductBase("Páprica defumada", Categoria.MERCEARIA, "un"),
    ProductBase("Bicarbonato de sódio", Categoria.MERCEARIA, "un")
)

private val doMesExtras = listOf(
    ProductBase("Alface", Categoria.HORTIFRUTI, "un"),
    ProductBase("Tomate", Categoria.HORTIFRUTI, "kg"),
    ProductBase("Cebola", Categoria.HORTIFRUTI, "kg"),
    ProductBase("Cenoura", Categoria.HORTIFRUTI, "kg"),
    ProductBase("Batata", Categoria.HORTIFRUTI, "kg"),
    ProductBase("Banana", Categoria.HORTIFRUTI, "kg"),
    ProductBase("Maçã", Categoria.HORTIFRUTI, "kg"),
    ProductBase("Laranja", Categoria.HORTIFRUTI, "kg"),
    ProductBase("Limão", Categoria.HORTIFRUTI, "un"),
    ProductBase("Alho", Categoria.HORTIFRUTI, "un"),
    ProductBase("Queijo mussarela", Categoria.LATICINIOS, "kg"),
    ProductBase("Iogurte", Categoria.LATICINIOS, "un"),
    ProductBase("Manteiga", Categoria.LATICINIOS, "un"),
    ProductBase("Requeijão", Categoria.LATICINIOS, "un"),
    ProductBase("Ovo", Categoria.LATICINIOS, "un"),
    ProductBase("Costela", Categoria.CARNES, "kg"),
    ProductBase("Linguiça", Categoria.CARNES, "kg"),
    ProductBase("Açúcar", Categoria.MERCEARIA, "un"),
    ProductBase("Farinha de trigo", Categoria.MERCEARIA, "un"),
    ProductBase("Azeite", Categoria.MERCEARIA, "un"),
    ProductBase("Maionese", Categoria.MERCEARIA, "un"),
    ProductBase("Pão de forma", Categoria.PADARIA, "un"),
    ProductBase("Água mineral", Categoria.BEBIDAS, "un"),
    ProductBase("Fio dental", Categoria.HIGIENE, "un"),
    ProductBase("Desodorante", Categoria.HIGIENE, "un"),
    ProductBase("Saco de lixo", Categoria.LIMPEZA, "un"),
    ProductBase("Esponja de aço", Categoria.LIMPEZA, "un"),
    ProductBase("Sabão em pó", Categoria.LIMPEZA, "un")
)

val doMesItems: List<ProductBase> = essencialItems + doMesExtras

fun produtosPorModelo(nomeModelo: String): List<ProductBase> = when (nomeModelo) {
    "Essencial" -> essencialItems
    "Do mês" -> doMesItems
    else -> emptyList()
}

suspend fun sementarTemplates(context: Context, db: AppDatabase) = withContext(Dispatchers.IO) {
    val prefs = context.getSharedPreferences("compreis_prefs", Context.MODE_PRIVATE)
    if (prefs.getBoolean("semente_templates_v1", false)) return@withContext

    val essencialId = db.shoppingListDao().insert(
        ShoppingListEntity(name = "Essencial", isTemplate = true, isPredefined = true)
    )
    essencialItems.forEach { p ->
        db.itemDao().insert(
            ItemEntity(listId = essencialId, name = p.nome, price = 0.0, unit = p.unidade, quantity = 1.0, category = p.categoria.rawValue)
        )
    }

    val doMesId = db.shoppingListDao().insert(
        ShoppingListEntity(name = "Do mês", isTemplate = true, isPredefined = true)
    )
    doMesItems.forEach { p ->
        db.itemDao().insert(
            ItemEntity(listId = doMesId, name = p.nome, price = 0.0, unit = p.unidade, quantity = 1.0, category = p.categoria.rawValue)
        )
    }

    prefs.edit().putBoolean("semente_templates_v1", true).apply()
}

suspend fun criarItensDeModelo(db: AppDatabase, listaId: Long, nomeModelo: String) = withContext(Dispatchers.IO) {
    val allTemplates = db.shoppingListDao().getPredefined().first()
    val templateList = allTemplates.find { it.name == nomeModelo }

    if (templateList != null) {
        val templateItems = db.itemDao().getByList(templateList.id).first()
        templateItems.forEach { templateItem ->
            val history = db.productHistoryDao().search(templateItem.name).firstOrNull { it.name == templateItem.name }
            db.itemDao().insert(
                ItemEntity(
                    listId = listaId,
                    name = templateItem.name,
                    price = history?.price ?: 0.0,
                    unit = templateItem.unit,
                    quantity = templateItem.quantity,
                    category = templateItem.category,
                    picked = false
                )
            )
        }
    } else {
        val produtos = produtosPorModelo(nomeModelo)
        produtos.forEach { p ->
            val history = db.productHistoryDao().search(p.nome).firstOrNull { it.name == p.nome }
            db.itemDao().insert(
                ItemEntity(
                    listId = listaId,
                    name = p.nome,
                    price = history?.price ?: 0.0,
                    unit = p.unidade,
                    quantity = 1.0,
                    category = p.categoria.rawValue,
                    picked = false
                )
            )
        }
    }
}
