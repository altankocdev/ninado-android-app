package com.altankoc.nidado.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

fun String.toTitleCase(): String {
    return this.split(" ").joinToString(" ") { word ->
        word.lowercase().replaceFirstChar { it.uppercase() }
    }
}

fun Float.toDifficultyColor(): Color {
    val t = ((this - 1f) / 4f).coerceIn(0f, 1f) // 1.0-5.0 → 0.0-1.0

    return when {
        t <= 0.25f -> lerp(
            Color(0xFF4CAF50), // yeşil
            Color(0xFF8BC34A), // açık yeşil
            t / 0.25f
        )
        t <= 0.5f -> lerp(
            Color(0xFF8BC34A), // açık yeşil
            Color(0xFFFFEB3B), // sarı
            (t - 0.25f) / 0.25f
        )
        t <= 0.75f -> lerp(
            Color(0xFFFFEB3B), // sarı
            Color(0xFFFF9800), // turuncu
            (t - 0.5f) / 0.25f
        )
        else -> lerp(
            Color(0xFFFF9800), // turuncu
            Color(0xFFF44336), // kırmızı
            (t - 0.75f) / 0.25f
        )
    }
}