val sdkmgr = File(latest, "bin/sdkmanager").absolutePath

fun runProc(cmd: List<String>, workDir: File = rootDir) {
    println(">> " + cmd.joinToString(" "))
    val proc = ProcessBuilder(cmd)
        .directory(workDir)
        .redirectErrorStream(true)
        .start()
    proc.inputStream.bufferedReader().useLines { it.forEach(::println) }
    val code = proc.waitFor()
    if (code != 0) error("Command failed: ${cmd.joinToString(" ")} (exit=$code)")
}

fun acceptLicenses() {
    // без bash/pipe: просто передаём "--licenses"
    runProc(listOf(sdkmgr, "--sdk_root=${sdkRoot.path}", "--licenses"))
}

fun installSdk(api: Int = 34, buildTools: String = "34.0.0") {
    // каждый аргумент отдельным элементом списка (важно!)
    runProc(listOf(
        sdkmgr, "--sdk_root=${sdkRoot.path}",
        "platform-tools",
        "platforms;android-$api",
        "build-tools;$buildTools"
    ))
}

println("Installing Android SDK packages...")
acceptLicenses()
installSdk(api = 34, buildTools = "34.0.0")

System.setProperty("android.sdk.root", sdkRoot.path)
System.setProperty("android.home", sdkRoot.path)
writeLocalProps(sdkRoot.path)
println("Android SDK installed at: ${sdkRoot.path}")



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
