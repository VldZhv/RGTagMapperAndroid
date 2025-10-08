// Keep Kotlin serialization metadata
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable *;
}
-keep class kotlinx.serialization.** { *; }
-keep class com.rg.mapper.android.** { *; }
