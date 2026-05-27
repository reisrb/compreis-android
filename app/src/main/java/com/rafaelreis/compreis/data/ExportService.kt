package com.rafaelreis.compreis.data

import com.rafaelreis.compreis.data.db.AppDatabase
import com.rafaelreis.compreis.data.db.ItemEntity
import com.rafaelreis.compreis.data.db.ShoppingListEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object ExportService {

    suspend fun exportJSON(db: AppDatabase): String = withContext(Dispatchers.IO) {
        val listas = db.shoppingListDao().getAll().first()
        val todosItens = db.itemDao().getAll().first()

        val root = JSONObject()
        val listasArray = JSONArray()

        listas.forEach { lista ->
            val listaObj = JSONObject().apply {
                put("id", lista.id)
                put("nome", lista.name)
                put("dataMercado", lista.marketDate ?: JSONObject.NULL)
                put("criadaEm", lista.createdAt)
                put("finalizadaEm", lista.finalizedAt ?: JSONObject.NULL)
                put("finalizada", lista.finalized)
                put("isTemplate", lista.isTemplate)
                put("isPredefined", lista.isPredefined)
                put("totalPago", lista.totalPaid ?: JSONObject.NULL)

                val itensArray = JSONArray()
                todosItens.filter { it.listId == lista.id }.forEach { item ->
                    itensArray.put(JSONObject().apply {
                        put("id", item.id)
                        put("listaId", item.listId)
                        put("nome", item.name)
                        put("preco", item.price)
                        put("unidade", item.unit)
                        put("quantidade", item.quantity)
                        put("categoria", item.category)
                        put("pegou", item.picked)
                    })
                }
                put("itens", itensArray)
            }
            listasArray.put(listaObj)
        }

        root.put("listas", listasArray)
        root.put("exportadoEm", System.currentTimeMillis())
        root.toString(2)
    }

    suspend fun importJSON(db: AppDatabase, json: String): Pair<Int, Int> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(json)
            val listasArray = root.getJSONArray("listas")
            var listsCount = 0
            var itemsCount = 0

            repeat(listasArray.length()) { i ->
                val listaObj = listasArray.getJSONObject(i)
                val lista = ShoppingListEntity(
                    name = listaObj.getString("nome"),
                    marketDate = listaObj.optLong("dataMercado").takeIf { it != 0L },
                    createdAt = listaObj.optLong("criadaEm", System.currentTimeMillis()),
                    finalizedAt = listaObj.optLong("finalizadaEm").takeIf { it != 0L },
                    finalized = listaObj.optBoolean("finalizada", false),
                    isTemplate = listaObj.optBoolean("isTemplate", false),
                    isPredefined = false,
                    totalPaid = listaObj.optDouble("totalPago").takeIf { !it.isNaN() }
                )
                val novaListaId = db.shoppingListDao().insert(lista)
                listsCount++

                val itensArray = listaObj.optJSONArray("itens") ?: JSONArray()
                repeat(itensArray.length()) { j ->
                    val itemObj = itensArray.getJSONObject(j)
                    db.itemDao().insert(
                        ItemEntity(
                            listId = novaListaId,
                            name = itemObj.getString("nome"),
                            price = itemObj.optDouble("preco", 0.0),
                            unit = itemObj.optString("unidade", "un"),
                            quantity = itemObj.optDouble("quantidade", 1.0),
                            category = itemObj.optString("categoria", "outros"),
                            picked = itemObj.optBoolean("pegou", false)
                        )
                    )
                    itemsCount++
                }
            }
            Pair(listsCount, itemsCount)
        } catch (e: Exception) {
            Pair(0, 0)
        }
    }
}
