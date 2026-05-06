package com.uamt.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
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
    var logs by remember { mutableStateOf(listOf(">>> UAMT UI v1.3.1 Loaded")) }
    val scope = rememberCoroutineScope()

    fun log(msg: String) { logs = logs + msg }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("UAMT", fontFamily = FontFamily.Monospace) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFF121212))
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            
            Box(modifier = Modifier.weight(1f)) {
                when (currentScreen) {
                    "menu" -> MenuScreen { currentScreen = it }
                    "inject_frida" -> FridaScreen(scope, ::log) { currentScreen = "menu" }
                    "inject_custom" -> CustomLibScreen(scope, ::log) { currentScreen = "menu" }
                    "connect" -> ConnectScreen(scope, ::log) { currentScreen = "menu" }
                    "tools" -> ToolsScreen(scope, ::log) { currentScreen = "menu" }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            // Console Output Area
            Surface(
                modifier = Modifier.weight(0.4f).fillMaxWidth(),
                color = Color.Black,
                shape = MaterialTheme.shapes.medium
            ) {
                LazyColumn(modifier = Modifier.padding(8.dp), reverseLayout = true) {
                    items(logs.reversed()) { msg ->
                        Text(msg, color = Color.Green, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
fun MenuScreen(onNavigate: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        MenuButton("INJECT FRIDA GADGET", "inject_frida") { onNavigate(it) }
        MenuButton("INJECT CUSTOM LIB", "inject_custom") { onNavigate(it) }
        MenuButton("CONNECT TO GADGET", "connect") { onNavigate(it) }
        MenuButton("INSTALL / UPDATE TOOLS", "tools") { onNavigate(it) }
    }
}

@Composable
fun MenuButton(label: String, route: String, onClick: (String) -> Unit) {
    Button(
        onClick = { onClick(route) },
        modifier = Modifier.fillMaxWidth().height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Text(label, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
    }
}

@Composable
fun FridaScreen(scope: kotlinx.coroutines.CoroutineScope, log: (String) -> Unit, onBack: () -> Unit) {
    var apkPath by remember { mutableStateOf("/sdcard/game.apk") }
    var strategy by remember { mutableStateOf("Auto") }
    var selectedArchs by remember { mutableStateOf(setOf("arm64-v8a", "armeabi-v7a")) }

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text("Frida Injection Settings", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(value = apkPath, onValueChange = { apkPath = it }, label = { Text("APK Path") }, modifier = Modifier.fillMaxWidth())
        
        Text("Target Architectures:", modifier = Modifier.padding(top=8.dp))
        Row {
            listOf("arm64-v8a", "armeabi-v7a").forEach { arch ->
                FilterChip(
                    selected = selectedArchs.contains(arch),
                    onClick = { if(selectedArchs.contains(arch)) selectedArchs -= arch else selectedArchs += arch },
                    label = { Text(arch) },
                    modifier = Modifier.padding(end=4.dp)
                )
            }
        }

        Button(onClick = {
            scope.launch {
                withContext(Dispatchers.IO) {
                    UamtEngine.injectFrida(apkPath, selectedArchs.toList(), strategy, null, log)
                }
            }
        }, modifier = Modifier.fillMaxWidth().padding(top=16.dp)) { Text("START INJECTION") }
        
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("CANCEL") }
    }
}

@Composable
fun CustomLibScreen(scope: kotlinx.coroutines.CoroutineScope, log: (String) -> Unit, onBack: () -> Unit) {
    var apkPath by remember { mutableStateOf("/sdcard/base.apk") }
    var soPath by remember { mutableStateOf("/sdcard/libhook.so") }
    Column {
        OutlinedTextField(value = apkPath, onValueChange = { apkPath = it }, label = { Text("Target APK") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = soPath, onValueChange = { soPath = it }, label = { Text("Library .so") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            scope.launch { withContext(Dispatchers.IO) { UamtEngine.runCommand(listOf("echo", "Injecting $soPath into $apkPath"), log) } }
        }, modifier = Modifier.fillMaxWidth().padding(top=16.dp)) { Text("INJECT") }
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("BACK") }
    }
}

@Composable
fun ConnectScreen(scope: kotlinx.coroutines.CoroutineScope, log: (String) -> Unit, onBack: () -> Unit) {
    var host by remember { mutableStateOf("127.0.0.1:27042") }
    Column {
        OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text("Frida Host") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            scope.launch { withContext(Dispatchers.IO) { UamtEngine.runCommand(listOf("frida", "-H", host, "gadget"), log) } }
        }, modifier = Modifier.fillMaxWidth().padding(top=16.dp)) { Text("CONNECT") }
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("BACK") }
    }
}

@Composable
fun ToolsScreen(scope: kotlinx.coroutines.CoroutineScope, log: (String) -> Unit, onBack: () -> Unit) {
    Column {
        Text("System Tools Manager", style = MaterialTheme.typography.titleMedium)
        Button(onClick = {
            scope.launch { withContext(Dispatchers.IO) { UamtEngine.installDependencies(log) } }
        }, modifier = Modifier.fillMaxWidth().padding(top=16.dp)) { Text("INSTALL / UPDATE ALL") }
        Button(onClick = {
            scope.launch { withContext(Dispatchers.IO) { UamtEngine.downloadFrida(log) } }
        }, modifier = Modifier.fillMaxWidth().padding(top=8.dp)) { Text("DOWNLOAD FRIDA GADGET") }
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("BACK") }
    }
}
