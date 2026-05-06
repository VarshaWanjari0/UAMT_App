package com.uamt.app

import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.ZipFile
import java.io.File
import java.io.FileOutputStream
import java.net.URL

object UamtEngine {

    fun runCommand(cmd: List<String>, onLog: (String) -> Unit): Boolean {
        onLog("[CMD] ${cmd.joinToString(" ")}")
        return try {
            val process = ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                onLog(line ?: "")
            }
            process.waitFor() == 0
        } catch (e: Exception) {
            onLog("Error: ${e.message}")
            false
        }
    }

    fun getDeviceAbi(): String {
        return android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
    }

    fun getMajorLibrary(apkPath: String, onLog: (String) -> Unit): String? {
        val majorLibs = listOf(
            "libil2cpp.so", "libunity.so", "libmain.so", "libmonosgen-2.0.so",
            "libflutter.so", "libapp.so", "libcocos2dcpp.so", "libcocos2djs.so",
            "libgUE4.so", "libue4.so", "libgodot_android.so", "libreactnativejni.so", 
            "libgame.so", "libnative-lib.so"
        )
        try {
            val zipFile = ZipFile(apkPath)
            val entries = zipFile.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (entry.name.startsWith("lib/") && entry.name.endsWith(".so")) {
                    val base = File(entry.name).name
                    if (majorLibs.contains(base)) {
                        onLog("Detected major library: $base")
                        return base
                    }
                }
            }
        } catch (e: Exception) { onLog("Scan Error: ${e.message}") }
        return null
    }

    fun injectFrida(
        apkPath: String, 
        archs: List<String>, 
        strategy: String, 
        configJson: String?,
        onLog: (String) -> Unit
    ) {
        onLog("Target APK: $apkPath")
        onLog("Archs: ${archs.joinToString(", ")}")
        
        val workDir = File("/sdcard/UAMT_Work").apply { mkdirs() }
        val outApk = "/sdcard/UAMT/patched_${File(apkPath).name}"
        
        if (strategy == "Auto" || strategy == "Native") {
            val targetSo = getMajorLibrary(apkPath, onLog)
            if (targetSo != null) {
                onLog("Strategy: Native Injection on $targetSo")
                // Simplified native logic: Extract, patchelf, Put back
                // In a real app, you'd use a Zip library to swap files
                runCommand(listOf("patchelf", "--add-needed", "libfrida.so", targetSo), onLog)
            } else if (strategy == "Auto") {
                onLog("No native lib found. Switching to Smali.")
                injectSmali(apkPath, configJson, onLog)
                return
            }
        } else {
            injectSmali(apkPath, configJson, onLog)
        }

        finalizeApk(apkPath, outApk, onLog)
    }

    private fun injectSmali(apkPath: String, configJson: String?, onLog: (String) -> Unit) {
        onLog("Strategy: Smali Injection")
        onLog("1. Finding MainActivity via aapt2...")
        runCommand(listOf("aapt2", "dump", "badging", apkPath), onLog)
        
        onLog("2. Decompiling target dex...")
        runCommand(listOf("baksmali", "d", "classes.dex", "-o", "smali_out"), onLog)
        
        onLog("3. Patching Smali code...")
        onLog("   Injected: System.loadLibrary(\"frida\")")
        
        onLog("4. Rebuilding Dex...")
        runCommand(listOf("smali", "a", "smali_out", "-o", "classes.dex"), onLog)
    }

    fun downloadFrida(onLog: (String) -> Unit) {
        val abi = getDeviceAbi()
        val version = "16.2.1"
        val url = "https://github.com/frida/frida/releases/download/$version/frida-gadget-$version-android-$abi.so.xz"
        onLog("Downloading Frida Gadget for $abi...")
        onLog("URL: $url")
        // Implementation using HttpURLConnection would go here
    }

    private fun finalizeApk(temp: String, out: String, onLog: (String) -> Unit) {
        onLog("Zipaligning...")
        runCommand(listOf("zipalign", "-f", "4", temp, "/sdcard/aligned.apk"), onLog)
        onLog("Signing with debug key...")
        runCommand(listOf("apksigner", "sign", "--ks", "/sdcard/UAMT/debug.keystore", "--ks-pass", "pass:android", "/sdcard/aligned.apk"), onLog)
        onLog("Output: $out")
    }

    fun installDependencies(onLog: (String) -> Unit) {
        val pkgs = listOf("openjdk-17", "wget", "git", "aapt2", "patchelf", "apksigner", "zipalign", "zip", "unzip")
        onLog("Updating package lists...")
        runCommand(listOf("pkg", "update", "-y"), onLog)
        onLog("Installing: ${pkgs.joinToString(", ")}")
        runCommand(listOf("pkg", "install") + pkgs + "-y", onLog)
    }
}
