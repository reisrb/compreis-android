package com.rafaelreis.compreis.data

import android.content.Context
import com.rafaelreis.compreis.data.db.AppDatabase
import com.rafaelreis.compreis.data.db.ItemEntity
import com.rafaelreis.compreis.data.db.ShoppingListEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

data class ProdutoBase(val nome: String, val categoria: Categoria, val unidade: String)

private val essencialItems = listOf(
    ProdutoBase("Papel higiênico", Categoria.HIGIENE, "un"),
    ProdutoBase("Cotonete", Categoria.HIGIENE, "un"),
    ProdutoBase("Algodão", Categoria.HIGIENE, "un"),
    ProdutoBase("Shampoo", Categoria.HIGIENE, "un"),
    ProdutoBase("Condicionador", Categoria.HIGIENE, "un"),
    ProdutoBase("Pasta de dente", Categoria.HIGIENE, "un"),
    ProdutoBase("Sabonete líquido", Categoria.HIGIENE, "un"),
    ProdutoBase("Desengordurante", Categoria.LIMPEZA, "un"),
    ProdutoBase("Detergente", Categoria.LIMPEZA, "un"),
    ProdutoBase("Desinfetante", Categoria.LIMPEZA, "un"),
    ProdutoBase("Água sanitária", Categoria.LIMPEZA, "un"),
    ProdutoBase("Bucha de cozinha", Categoria.LIMPEZA, "un"),
    ProdutoBase("Bucha de banho", Categoria.LIMPEZA, "un"),
    ProdutoBase("Leite", Categoria.LATICINIOS, "un"),
    ProdutoBase("Creme de leite", Categoria.LATICINIOS, "un"),
    ProdutoBase("Carne moída", Categoria.CARNES, "kg"),
    ProdutoBase("Frango", Categoria.CARNES, "kg"),
    ProdutoBase("Bife de fígado", Categoria.CARNES, "kg"),
    ProdutoBase("Feijão", Categoria.MERCEARIA, "un"),
    ProdutoBase("Arroz", Categoria.MERCEARIA, "un"),
    ProdutoBase("Óleo de soja", Categoria.MERCEARIA, "un"),
    ProdutoBase("Vinagre", Categoria.MERCEARIA, "un"),
    ProdutoBase("Macarrão", Categoria.MERCEARIA, "un"),
    ProdutoBase("Molho de tomate", Categoria.MERCEARIA, "un"),
    ProdutoBase("Extrato de tomate", Categoria.MERCEARIA, "un"),
    ProdutoBase("Ketchup", Categoria.MERCEARIA, "un"),
    ProdutoBase("Mostarda", Categoria.MERCEARIA, "un"),
    ProdutoBase("Café", Categoria.MERCEARIA, "un"),
    ProdutoBase("Chocolate em pó", Categoria.MERCEARIA, "un"),
    ProdutoBase("Sal", Categoria.MERCEARIA, "un"),
    ProdutoBase("Chimichurri", Categoria.MERCEARIA, "un"),
    ProdutoBase("Alho em pó", Categoria.MERCEARIA, "un"),
    ProdutoBase("Páprica defumada", Categoria.MERCEARIA, "un"),
    ProdutoBase("Bicarbonato de sódio", Categoria.MERCEARIA, "un")
)

private val doMesExtras = listOf(
    ProdutoBase("Alface", Categoria.HORTIFRUTI, "un"),
    ProdutoBase("Tomate", Categoria.HORTIFRUTI, "kg"),
    ProdutoBase("Cebola", Categoria.HORTIFRUTI, "kg"),
    ProdutoBase("Cenoura", Categoria.HORTIFRUTI, "kg"),
    ProdutoBase("Batata", Categoria.HORTIFRUTI, "kg"),
    ProdutoBase("Banana", Categoria.HORTIFRUTI, "kg"),
    ProdutoBase("Maçã", Categoria.HORTIFRUTI, "kg"),
    ProdutoBase("Laranja", Categoria.HORTIFRUTI, "kg"),
    ProdutoBase("Limão", Categoria.HORTIFRUTI, "un"),
    ProdutoBase("Alho", Categoria.HORTIFRUTI, "un"),
    ProdutoBase("Queijo mussarela", Categoria.LATICINIOS, "kg"),
    ProdutoBase("Iogurte", Categoria.LATICINIOS, "un"),
    ProdutoBase("Manteiga", Categoria.LATICINIOS, "un"),
    ProdutoBase("Requeijão", Categoria.LATICINIOS, "un"),
    ProdutoBase("Ovo", Categoria.LATICINIOS, "un"),
    ProdutoBase("Costela", Categoria.CARNES, "kg"),
    ProdutoBase("Linguiça", Categoria.CARNES, "kg"),
    ProdutoBase("Açúcar", Categoria.MERCEARIA, "un"),
    ProdutoBase("Farinha de trigo", Categoria.MERCEARIA, "un"),
    ProdutoBase("Azeite", Categoria.MERCEARIA, "un"),
    ProdutoBase("Maionese", Categoria.MERCEARIA, "un"),
    ProdutoBase("Pão de forma", Categoria.PADARIA, "un"),
    ProdutoBase("Água mineral", Categoria.BEBIDAS, "un"),
    ProdutoBase("Fio dental", Categoria.HIGIENE, "un"),
    ProdutoBase("Desodorante", Categoria.HIGIENE, "un"),
    ProdutoBase("Saco de lixo", Categoria.LIMPEZA, "un"),
    ProdutoBase("Esponja de aço", Categoria.LIMPEZA, "un"),
    ProdutoBase("Sabão em pó", Categoria.LIMPEZA, "un")
)

val doMesItems: List<ProdutoBase> = essencialItems + doMesExtras

fun produtosPorModelo(nomeModelo: String): List<ProdutoBase> = when (nomeModelo) {
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
