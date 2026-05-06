package com.uamt.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    UamtApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UamtApp() {
    var currentScreen by remember { mutableStateOf("menu") }
    var logs by remember { mutableStateOf(listOf("Welcome to UAMT UI v1.3.1")) }
    val coroutineScope = rememberCoroutineScope()

    fun log(msg: String) {
        logs = logs + msg
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ultimate Android Modding Toolkit") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            
            Box(modifier = Modifier.weight(1f)) {
                when (currentScreen) {
                    "menu" -> {
                        Column {
                            Button(onClick = { currentScreen = "inject_frida" }, modifier = Modifier.fillMaxWidth().padding(bottom=8.dp)) { Text("Inject Frida Gadget") }
                            Button(onClick = { currentScreen = "inject_custom" }, modifier = Modifier.fillMaxWidth().padding(bottom=8.dp)) { Text("Inject Custom Library") }
                            Button(onClick = { currentScreen = "connect" }, modifier = Modifier.fillMaxWidth().padding(bottom=8.dp)) { Text("Connect to Gadget") }
                            Button(onClick = { currentScreen = "download" }, modifier = Modifier.fillMaxWidth().padding(bottom=8.dp)) { Text("Download Frida Gadget") }
                            Button(onClick = { currentScreen = "install" }, modifier = Modifier.fillMaxWidth().padding(bottom=8.dp)) { Text("Install and Update Tools") }
                        }
                    }
                    "inject_frida" -> {
                        Column {
                            Text("Frida Injection", style = MaterialTheme.typography.titleLarge)
                            var apkPath by remember { mutableStateOf("/sdcard/target.apk") }
                            OutlinedTextField(value = apkPath, onValueChange = { apkPath = it }, label = { Text("Target APK Path") }, modifier = Modifier.fillMaxWidth())
                            
                            Button(onClick = {
                                log("Starting Frida Injection for $apkPath...")
                                coroutineScope.launch {
                                    withContext(Dispatchers.IO) {
                                        UamtEngine.injectFrida(apkPath) { log(it) }
                                    }
                                }
                            }, modifier = Modifier.padding(top=8.dp)) { Text("Start Injection") }
                            Button(onClick = { currentScreen = "menu" }, modifier = Modifier.padding(top=8.dp)) { Text("Back") }
                        }
                    }
                    "inject_custom" -> {
                        Column {
                            Text("Custom Library Injection", style = MaterialTheme.typography.titleLarge)
                            var apkPath by remember { mutableStateOf("/sdcard/target.apk") }
                            var soPath by remember { mutableStateOf("/sdcard/mylib.so") }
                            OutlinedTextField(value = apkPath, onValueChange = { apkPath = it }, label = { Text("Target APK Path") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = soPath, onValueChange = { soPath = it }, label = { Text("Custom .so Path") }, modifier = Modifier.fillMaxWidth())
                            
                            Button(onClick = {
                                log("Starting Custom Injection...")
                                coroutineScope.launch {
                                    withContext(Dispatchers.IO) {
                                        UamtEngine.injectCustom(apkPath, soPath) { log(it) }
                                    }
                                }
                            }, modifier = Modifier.padding(top=8.dp)) { Text("Start Injection") }
                            Button(onClick = { currentScreen = "menu" }, modifier = Modifier.padding(top=8.dp)) { Text("Back") }
                        }
                    }
                    "connect" -> {
                        Column {
                            Text("Connect to Gadget", style = MaterialTheme.typography.titleLarge)
                            var ip by remember { mutableStateOf("127.0.0.1") }
                            var port by remember { mutableStateOf("27042") }
                            OutlinedTextField(value = ip, onValueChange = { ip = it }, label = { Text("IP Address") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = port, onValueChange = { port = it }, label = { Text("Port") }, modifier = Modifier.fillMaxWidth())
                            
                            Button(onClick = {
                                log("Connecting to $ip:$port...")
                                coroutineScope.launch {
                                    withContext(Dispatchers.IO) {
                                        UamtEngine.runCommand(listOf("frida", "-H", "$ip:$port", "gadget")) { log(it) }
                                    }
                                }
                            }, modifier = Modifier.padding(top=8.dp)) { Text("Connect") }
                            Button(onClick = { currentScreen = "menu" }, modifier = Modifier.padding(top=8.dp)) { Text("Back") }
                        }
                    }
                    "download" -> {
                        Column {
                            Text("Download Frida Gadget", style = MaterialTheme.typography.titleLarge)
                            Button(onClick = {
                                log("Downloading Frida Gadget...")
                                coroutineScope.launch {
                                    withContext(Dispatchers.IO) {
                                        UamtEngine.downloadFrida { log(it) }
                                    }
                                }
                            }, modifier = Modifier.padding(top=8.dp)) { Text("Download") }
                            Button(onClick = { currentScreen = "menu" }, modifier = Modifier.padding(top=8.dp)) { Text("Back") }
                        }
                    }
                    "install" -> {
                        Column {
                            Text("Install Dependencies", style = MaterialTheme.typography.titleLarge)
                            Button(onClick = {
                                log("Installing dependencies via pkg...")
                                coroutineScope.launch {
                                    withContext(Dispatchers.IO) {
                                        UamtEngine.installDependencies { log(it) }
                                    }
                                }
                            }, modifier = Modifier.padding(top=8.dp)) { Text("Install") }
                            Button(onClick = { currentScreen = "menu" }, modifier = Modifier.padding(top=8.dp)) { Text("Back") }
                        }
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))
            Text("Console Log:", style = MaterialTheme.typography.labelMedium)
            LazyColumn(modifier = Modifier.weight(0.5f).fillMaxWidth()) {
                items(logs) { logMsg ->
                    Text(logMsg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
