// --- auto-detect Android SDK and write local.properties if needed ---
import java.util.Properties
import java.io.File

val localPropsFile = File(rootDir, "local.properties")

// кандидаты: переменные окружения + типовые пути контейнеров
val candidates = buildList {
    System.getenv("ANDROID_SDK_ROOT")?.let { add(it) }
    System.getenv("ANDROID_HOME")?.let { add(it) }
    add("/usr/local/lib/android/sdk")
    add("/opt/android-sdk")
    add("/opt/android-sdk-linux")
    add("/usr/lib/android-sdk")
    add("${System.getProperty("user.home")}/android-sdk")
}.distinct()

val found = candidates.firstOrNull { it.isNotBlank() && File(it).isDirectory }

if (found != null) {
    // пробрасываем внутрь Gradle
    System.setProperty("android.sdk.root", found)
    System.setProperty("android.home", found)

    // обновляем/создаём local.properties
    val p = Properties()
    if (localPropsFile.exists()) localPropsFile.inputStream().use { p.load(it) }
    p["sdk.dir"] = found
    localPropsFile.outputStream().use { out ->
        p.store(out, "auto-added by settings.gradle.kts")
    }
    println("Android SDK detected at: $found")
} else {
    println("WARNING: Android SDK not found in known locations. " +
            "Set ANDROID_SDK_ROOT/ANDROID_HOME or commit local.properties with sdk.dir=...")
}
// --- end auto-detect ---

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "RGTagMapperAndroid"
include(":app")
