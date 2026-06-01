package com.rafaelreis.compreis.data

import androidx.compose.ui.graphics.Color

enum class Categoria(val label: String, val color: Color) {
    HORTIFRUTI("Hortifruti", Color(0xFF4CAF50)),
    CARNES("Carnes", Color(0xFFE53935)),
    PEIXARIA("Peixaria", Color(0xFF1E88E5)),
    LATICINIOS("Laticínios", Color(0xFFFFB300)),
    PADARIA("Padaria", Color(0xFFFF8F00)),
    BEBIDAS("Bebidas", Color(0xFF7B1FA2)),
    CONGELADOS("Congelados", Color(0xFF0288D1)),
    MERCEARIA("Mercearia", Color(0xFF795548)),
    HIGIENE("Higiene", Color(0xFF26A69A)),
    LIMPEZA("Limpeza", Color(0xFF00ACC1)),
    OUTROS("Outros", Color(0xFF9E9E9E));

    val rawValue: String = name.lowercase()

    companion object {
        fun fromRaw(raw: String) = entries.find { it.rawValue == raw.lowercase() } ?: OUTROS
    }
}
