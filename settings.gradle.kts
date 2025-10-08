// settings.gradle.kts
import java.io.File
import java.util.Properties

// Где будет лежать SDK (можно поменять при желании)
val sdkRoot = File(rootDir, ".android-sdk")

// Путь к cmdline-tools/latest (у вас они уже скачиваются авто-скриптом)
val cmdlineToolsLatest = File(sdkRoot, "cmdline-tools/latest")
val sdkManager = File(cmdlineToolsLatest, "bin/sdkmanager").absolutePath

fun runProc(args: List<String>, workDir: File = rootDir) {
    println(">> " + args.joinToString(" "))
    val pb = ProcessBuilder(args)
        .directory(workDir)
        .redirectErrorStream(true)
        .start()
    pb.inputStream.bufferedReader().useLines { it.forEach(::println) }
    val code = pb.waitFor()
    if (code != 0) error("Command failed: ${args.joinToString(" ")} (exit=$code)")
}

fun writeLocalProps(sdkPath: String) {
    val file = File(rootDir, "local.properties")
    val props = Properties()
    if (file.exists()) file.inputStream().use { props.load(it) }
    props["sdk.dir"] = File(sdkPath).absolutePath.replace("\\", "\\\\")
    file.outputStream().use { props.store(it, null) }
}

fun acceptLicenses() {
    runProc(listOf(sdkManager, "--sdk_root=${sdkRoot.path}", "--licenses"))
}

fun installSdk(api: Int = 34, buildTools: String = "34.0.0") {
    runProc(
        listOf(
            sdkManager, "--sdk_root=${sdkRoot.path}",
            "platform-tools",
            "platforms;android-$api",
            "build-tools;$buildTools"
        )
    )
}

// Ставим SDK только если его нет
val needInstall = !File(sdkRoot, "platforms/android-34").exists()

if (cmdlineToolsLatest.exists() && File(sdkManager).exists()) {
    println("Installing Android SDK packages...")
    if (needInstall) {
        acceptLicenses()
        installSdk(api = 34, buildTools = "34.0.0") // поменяйте при необходимости
    } else {
        println("Android SDK already present at: ${sdkRoot.path}")
    }
    System.setProperty("android.sdk.root", sdkRoot.path)
    System.setProperty("android.home", sdkRoot.path)
    writeLocalProps(sdkRoot.path)
    println("Android SDK ready at: ${sdkRoot.path}")
} else {
    println("WARNING: cmdline-tools not found at $cmdlineToolsLatest — пропускаю авто-установку SDK.")
    println("Убедитесь, что авто-скрипт скачал cmdline-tools, или задайте ANDROID_SDK_ROOT/ANDROID_HOME.")
}



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
