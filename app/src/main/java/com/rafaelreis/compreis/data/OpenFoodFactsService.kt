package com.rafaelreis.compreis.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class OFFProduto(val id: String, val titulo: String, val thumbnail: String?)

object OpenFoodFactsService {

    suspend fun buscar(query: String): List<OFFProduto> = withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = URL(
                "https://world.openfoodfacts.org/cgi/search.pl" +
                "?search_terms=$encoded" +
                "&search_simple=1" +
                "&action=process" +
                "&json=1" +
                "&page_size=6" +
                "&countries_tags=en:brazil" +
                "&fields=code,product_name,product_name_pt,image_front_small_url"
            )
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 8000
            conn.setRequestProperty("User-Agent", "Compreis Android App")

            val response = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
            val root = JSONObject(response)
            val products = root.optJSONArray("products") ?: return@withContext emptyList()

            val result = mutableListOf<OFFProduto>()
            for (i in 0 until products.length()) {
                val p = products.getJSONObject(i)
                val code = p.optString("code", "").ifBlank { continue }
                val titulo = p.optString("product_name_pt").ifBlank {
                    p.optString("product_name").ifBlank { continue }
                }
                val thumbnail = p.optString("image_front_small_url").ifBlank { null }
                result.add(OFFProduto(id = code, titulo = titulo, thumbnail = thumbnail))
            }
            result
        } catch (e: Exception) {
            emptyList()
        }
    }
}
