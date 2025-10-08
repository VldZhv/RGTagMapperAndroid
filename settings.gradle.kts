// settings.gradle.kts
import java.io.*
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Properties
import java.util.zip.ZipInputStream

// === Параметры, при необходимости поменяйте под свой compileSdk/buildTools ===
val sdkRoot = File(rootDir, ".android-sdk")
val compileSdk = 34
val buildTools = "34.0.0"

// Подбираем ZIP для cmdline-tools по ОС (значения проверенные; при желании обновите revision)
val cltUrl = when (System.getProperty("os.name").lowercase()) {
    in listOf("mac os x", "macos", "darwin") -> "https://dl.google.com/android/repository/commandlinetools-mac-11076708_latest.zip"
    in listOf("windows 10", "windows 11", "windows 7", "windows 8", "windows") -> "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip"
    else -> "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
}

val cmdlineToolsLatest = File(sdkRoot, "cmdline-tools/latest")
val sdkManager = File(cmdlineToolsLatest, "bin/sdkmanager").absolutePath

fun log(msg: String) = println("[android-sdk] $msg")

fun unzip(zipStream: InputStream, destDir: File) {
    ZipInputStream(BufferedInputStream(zipStream)).use { zis ->
        var entry = zis.nextEntry
        while (entry != null) {
            val outFile = File(destDir, entry.name)
            if (entry.isDirectory) {
                outFile.mkdirs()
            } else {
                outFile.parentFile?.mkdirs()
                FileOutputStream(outFile).use { fos ->
                    zis.copyTo(fos)
                }
                if (outFile.name.endsWith(".sh") || outFile.name.endsWith(".bat")) {
                    outFile.setExecutable(true)
                }
            }
            zis.closeEntry()
            entry = zis.nextEntry
        }
    }
}

fun ensureCmdlineTools() {
    if (cmdlineToolsLatest.exists()) return
    log("cmdline-tools не найдены, скачиваю…")
    sdkRoot.mkdirs()
    val tmpZip = Files.createTempFile("cmdline-tools", ".zip").toFile()
    URL(cltUrl).openStream().use { ins ->
        Files.copy(ins, tmpZip.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }
    // В ZIP корне папка cmdline-tools — распакуем и переместим её в cmdline-tools/latest
    val tmpDir = Files.createTempDirectory("clt-unzip").toFile()
    FileInputStream(tmpZip).use { unzip(it, tmpDir) }
    val unpacked = File(tmpDir, "cmdline-tools")
    val targetParent = File(sdkRoot, "cmdline-tools")
    targetParent.mkdirs()
    unpacked.copyRecursively(File(targetParent, "latest"), overwrite = true)
    tmpZip.delete()
    tmpDir.deleteRecursively()
}

fun runProcInteractive(args: List<String>) {
    log(">> " + args.joinToString(" "))
    val pb = ProcessBuilder(args)
        .redirectErrorStream(true)
        .start()
    // Периодически отвечаем "y" на лицензионные вопросы
    val writer = BufferedWriter(OutputStreamWriter(pb.outputStream))
    val reader = BufferedReader(InputStreamReader(pb.inputStream))
    var line: String?
    while (reader.readLine().also { line = it } != null) {
        println(line)
        if (line!!.contains("[y/N]", ignoreCase = true) || line!!.contains("Accept? (y/N)", ignoreCase = true)) {
            writer.write("y\n"); writer.flush()
        }
    }
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

fun ensureSdkPackages() {
    // чтобы не дергать каждый раз
    val platformOk = File(sdkRoot, "platforms/android-$compileSdk").exists()
    val buildToolsOk = File(sdkRoot, "build-tools/$buildTools").exists()
    if (platformOk && buildToolsOk) return
    runProcInteractive(listOf(sdkManager, "--sdk_root=${sdkRoot.path}", "--licenses"))
    runProcInteractive(
        listOf(
            sdkManager, "--sdk_root=${sdkRoot.path}",
            "platform-tools",
            "platforms;android-$compileSdk",
            "build-tools;$buildTools"
        )
    )
}

fun File.makeExecutableRecursively() {
    if (isFile) setExecutable(true)
    if (isDirectory) walkTopDown().forEach { if (it.isFile) it.setExecutable(true) }
}

// === bootstrap ===
try {
    ensureCmdlineTools()
    // иногда после распаковки теряются +x — вернём
    File(cmdlineToolsLatest, "bin").apply { if (exists()) makeExecutableRecursively() }

    if (!File(sdkManager).exists()) {
        log("sdkmanager всё ещё не найден по пути: $sdkManager")
    } else {
        ensureSdkPackages()
        System.setProperty("android.sdk.root", sdkRoot.path)
        System.setProperty("android.home", sdkRoot.path)
        writeLocalProps(sdkRoot.path)
        // репорты SDK любят наличие файла repositories.cfg
        File(System.getProperty("user.home")).resolve(".android").apply {
            mkdirs(); resolve("repositories.cfg").apply { if (!exists()) createNewFile() }
        }
        log("Android SDK готов: ${sdkRoot.path}")
    }
} catch (t: Throwable) {
    log("WARNING: не удалось подготовить SDK автоматически: ${t.message}")
    log("Если сборка всё ещё падает, укажите ANDROID_SDK_ROOT/ANDROID_HOME или положите cmdline-tools вручную.")
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
