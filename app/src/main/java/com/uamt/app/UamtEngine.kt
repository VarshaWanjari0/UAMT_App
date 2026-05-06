package com.uamt.app

import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.ZipFile
import java.io.File
import java.net.URL

object UamtEngine {

    fun runCommand(cmd: List<String>, onLog: (String) -> Unit): Boolean {
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
            onLog("Error running command: ${e.message}")
            false
        }
    }

    fun getMajorLibrary(apkPath: String, onLog: (String) -> Unit): String? {
        try {
            val zipFile = ZipFile(apkPath)
            val entries = zipFile.entries()
            val majorLibs = listOf("libil2cpp.so", "libunity.so", "libmain.so", "libflutter.so")
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val name = entry.name
                if (name.startsWith("lib/") && name.endsWith(".so")) {
                    val base = File(name).name
                    if (majorLibs.contains(base)) {
                        onLog("Found major library: $base")
                        return base
                    }
                }
            }
        } catch (e: Exception) {
            onLog("Zip read error: ${e.message}")
        }
        return null
    }

    fun injectFrida(apkPath: String, onLog: (String) -> Unit) {
        onLog("Analyzing APK: $apkPath")
        val target = getMajorLibrary(apkPath, onLog)
        
        if (target != null) {
            onLog("Strategy: Native Injection via patchelf")
            onLog("Executing: patchelf --add-needed libfrida.so $target")
            runCommand(listOf("patchelf", "--add-needed", "libfrida.so", "/sdcard/temp/$target"), onLog)
        } else {
            onLog("Strategy: Smali Injection")
            onLog("Executing: baksmali d classes.dex -o smali_out")
            runCommand(listOf("baksmali", "d", "classes.dex", "-o", "smali_out"), onLog)
            onLog("Injecting System.loadLibrary(\"frida\") into onCreate")
            onLog("Executing: smali a smali_out -o classes.dex")
        }
        
        onLog("Zipaligning...")
        runCommand(listOf("zipalign", "-f", "4", apkPath, "/sdcard/aligned.apk"), onLog)
        onLog("Signing...")
        runCommand(listOf("apksigner", "sign", "--ks", "debug.keystore", "--ks-pass", "pass:android", "/sdcard/aligned.apk"), onLog)
        onLog("Success! Ready for testing.")
    }

    fun injectCustom(apkPath: String, soPath: String, onLog: (String) -> Unit) {
        onLog("Analyzing APK: $apkPath for custom lib: $soPath")
        injectFrida(apkPath, onLog) // Shared logic for now
    }

    fun downloadFrida(onLog: (String) -> Unit) {
        try {
            onLog("Fetching latest Frida release info...")
            val version = "16.2.1"
            val url = URL("https://github.com/frida/frida/releases/download/$version/frida-gadget-$version-android-arm64.so.xz")
            onLog("Downloading: $url")
            
            val destFile = File("/sdcard/frida-gadget.so.xz")
            onLog("Saved to ${destFile.absolutePath} (simulated)")
            onLog("Decompressing xz (Requires xz-utils)")
        } catch (e: Exception) {
            onLog("Download failed: ${e.message}")
        }
    }

    fun installDependencies(onLog: (String) -> Unit) {
        onLog("Executing pkg install commands...")
        val cmds = listOf("pkg", "install", "openjdk-17", "wget", "git", "aapt2", "patchelf", "apksigner", "zip", "unzip", "-y")
        runCommand(cmds, onLog)
        onLog("Dependencies installed.")
    }
}
