package com.anto426.dynamicisland.plugins

enum class PluginPriority(val weight: Int) {
    CRITICAL(1000),
    HIGH(800),
    MEDIUM(500),
    LOW(200),
    BACKGROUND(0);

    companion object {
        fun fromString(value: String?): PluginPriority =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: MEDIUM
    }
}
