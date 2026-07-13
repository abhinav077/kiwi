package com.abhinavsirohi.kiwi.core.navigation

enum class KiwiDestination(
    val route: String,
    val label: String,
    val symbol: String
) {
    Today(route = "today", label = "Today", symbol = "⌂"),
    Calendar(route = "calendar", label = "Calendar", symbol = "▦"),
    Diary(route = "diary", label = "Diary", symbol = "✎"),
    Assistant(route = "assistant", label = "Ask Kiwi", symbol = "✦"),
    More(route = "more", label = "More", symbol = "⋯")
}
