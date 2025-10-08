// --- BOOTSTRAP ANDROID SDK (works in CI/no-terminal) ---
@file:Suppress("UnstableApiUsage")

import java.io.File
import java.net.URL
import java.nio.file.Files
import java.util.Properties

fun run(cmd: List<String>, workDir: File = rootDir) {
    println(">> " + cmd.joinToString(" "))
    val proc = ProcessBuilder(cmd)
        .directory(workDir)
        .redirectErrorStream(true)
        .start()
    proc.inputStream.bufferedReader().useLines { it.forEach(::println) }
    val code = proc.waitFor()
    if (code != 0) error("Command failed: ${cmd.joinToString(" ")} (exit=$code)")
}

val sdkRoot = File(rootDir, ".android-sdk").absoluteFile
val localPropsFile = File(rootDir, "local.properties")

fun writeLocalProps(path: String) {
    val p = Properties()
    if (localPropsFile.exists()) localPropsFile.inputStream().use { p.load(it) }
    p["sdk.dir"] = path
    localPropsFile.outputStream().use { p.store(it, "auto-added by settings.gradle.kts") }
}

fun ensureAndroidSdk() {
    // уже установлен?
    val buildTools = File(sdkRoot, "build-tools")
    if (buildTools.isDirectory && buildTools.list()?.isNotEmpty() == true) {
        System.setProperty("android.sdk.root", sdkRoot.path)
        System.setProperty("android.home", sdkRoot.path)
        writeLocalProps(sdkRoot.path)
        println("Android SDK already present at: ${sdkRoot.path}")
        return
    }

    // 1) скачать commandline-tools
    sdkRoot.mkdirs()
    val zip = File(rootDir, "cmdline-tools.zip")
    if (!zip.exists()) {
        println("Downloading Android commandline-tools...")
        // актуальная ссылка (linux)
        URL("https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip")
            .openStream().use { input -> Files.copy(input, zip.toPath()) }
    }

    // 2) распаковать в .android-sdk/cmdline-tools/latest
    val cmdline = File(sdkRoot, "cmdline-tools")
    val latest = File(cmdline, "latest")
    if (!latest.isDirectory) {
        cmdline.mkdirs()
        // требуем наличие unzip в контейнере (обычно есть)
        run(listOf("unzip", "-q", zip.absolutePath, "-d", cmdline.absolutePath))
        // архив распаковывается в папку "cmdline-tools"; переместим в "latest"
        val extracted = File(cmdline, "cmdline-tools")
        extracted.renameTo(latest)
    }

    // 3) установить пакеты SDK
    val sdkmgr = File(latest, "bin/sdkmanager").absolutePath
    val yes = if (System.getProperty("os.name").lowercase().contains("win")) "cmd" else "bash"

    fun sdk(cmd: String) = run(listOf(yes, "-lc", "yes | \"$sdkmgr\" --sdk_root=\"${sdkRoot.path}\" $cmd"))

    val required = listOf(
        "platform-tools",
        "platforms;android-34",
        "build-tools;34.0.0"
    )
    println("Installing Android SDK packages...")
    sdk("--licenses")
    sdk(required.joinToString(" "))

    // 4) применить для Gradle/AGP и записать local.properties
    System.setProperty("android.sdk.root", sdkRoot.path)
    System.setProperty("android.home", sdkRoot.path)
    writeLocalProps(sdkRoot.path)

    println("Android SDK installed at: ${sdkRoot.path}")
}

ensureAndroidSdk()
// --- END BOOTSTRAP ANDROID SDK ---


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
