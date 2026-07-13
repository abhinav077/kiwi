package com.abhinavsirohi.kiwi.core.navigation

enum class KiwiDestination(
    val route: String,
    val label: String,
    val symbol: String,
    val appearsInBottomDock: Boolean = true
) {
    SessionRestore(route = "session-restore", label = "Opening Kiwi", symbol = "", appearsInBottomDock = false),
    Welcome(route = "welcome", label = "Welcome", symbol = "✦", appearsInBottomDock = false),
    SignIn(route = "sign-in", label = "Sign in", symbol = "", appearsInBottomDock = false),
    AccessGate(route = "access-gate", label = "Checking access", symbol = "", appearsInBottomDock = false),
    ProfileSetup(route = "profile-setup", label = "Set up profile", symbol = "", appearsInBottomDock = false),
    Today(route = "today", label = "Today", symbol = "⌂"),
    Calendar(route = "calendar", label = "Calendar", symbol = "▦"),
    Wellness(route = "wellness", label = "Wellness", symbol = "✿"),
    Diary(route = "diary", label = "Diary", symbol = "✎"),
    Assistant(route = "assistant", label = "Ask Kiwi", symbol = "✦", appearsInBottomDock = false),
    More(route = "more", label = "More", symbol = "⋯")
}
