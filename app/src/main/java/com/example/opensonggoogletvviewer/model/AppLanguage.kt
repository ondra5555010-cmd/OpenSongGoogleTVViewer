package com.example.opensonggoogletvviewer.model

enum class AppLanguage(val displayName: String) {
    Czech("\u010ce\u0161tina"),
    Slovak("Sloven\u0161tina"),
    English("English");

    fun next(): AppLanguage {
        val languages = entries
        return languages[(ordinal + 1) % languages.size]
    }
}
