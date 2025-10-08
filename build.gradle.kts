﻿plugins {
    id("com.android.application") version "8.6.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.10" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.10" apply false
    // ВАЖНО для Kotlin 2.0+: отдельный плагин компилятора Compose
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.10" apply false
}
