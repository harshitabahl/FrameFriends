package com.harshita.iykykcollage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.harshita.iykykcollage.util.CollageExporter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); enableEdgeToEdge()
        setContent { FrameFriendsTheme { FrameFriendsApp() } }
    }
}

@Composable
private fun FrameFriendsApp(vm: AppViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.process(it) }
    }
    Surface(Modifier.fillMaxSize(), color = Color(0xFFFFF8F2)) {
        when (val value = state) {
            AppState.Ready -> WelcomeScreen { picker.launch(arrayOf("video/*")) }
            is AppState.Processing -> ProcessingScreen(value.progress.fraction, value.progress.stage)
            is AppState.Complete -> ResultScreen(value.result, vm::reset)
            is AppState.Failed -> ErrorScreen(value.message, vm::reset) { value.uri?.let(vm::process) }
        }
    }
}

@Composable
private fun WelcomeScreen(onPick: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("✦", fontSize = 58.sp, color = Color(0xFF6C5CE7))
        Spacer(Modifier.height(12.dp)); Text("FrameFriends", fontSize = 38.sp, fontWeight = FontWeight.Bold, color = Color(0xFF241C34))
        Text("One beautiful frame for every person", fontSize = 17.sp, color = Color(0xFF6C6175), textAlign = TextAlign.Center)
        Spacer(Modifier.height(42.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(28.dp)) {
            Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Choose a portrait video", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp)); Text("Faces are detected, matched and grouped entirely on your device.", textAlign = TextAlign.Center, color = Color(0xFF766C80))
                Spacer(Modifier.height(24.dp)); Button(onClick = onPick, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C5CE7)), modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("Select video") }
            }
        }
    }
}

@Composable
private fun ProcessingScreen(progress: Float, stage: String) {
    Column(Modifier.fillMaxSize().padding(34.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(progress = { progress }, modifier = Modifier.size(112.dp), strokeWidth = 10.dp, color = Color(0xFF6C5CE7), trackColor = Color(0xFFE4DFF4))
        Spacer(Modifier.height(30.dp)); Text("${(progress * 100).toInt()}%", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Text(stage, fontSize = 17.sp, color = Color(0xFF6C6175), textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp)); Text("Processing stays on-device and off the main thread.", fontSize = 13.sp, color = Color(0xFF91899A), textAlign = TextAlign.Center)
    }
}

@Composable
private fun ResultScreen(result: com.harshita.iykykcollage.model.ProcessingResult, onAnother: () -> Unit) {
    val context = LocalContext.current
    var savedMessage by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 42.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Your people, one frame", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("${result.people.size} unique • ${result.totalAppearances} appearances", color = Color(0xFF6C6175))
        Spacer(Modifier.height(18.dp))
        Image(result.collage.asImageBitmap(), "Generated people collage", Modifier.fillMaxWidth().aspectRatio(9f/16f).background(Color.White, RoundedCornerShape(20.dp)), contentScale = ContentScale.Fit)
        Spacer(Modifier.height(20.dp))
        result.people.forEach { person ->
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Person ${person.id + 1}", fontWeight = FontWeight.SemiBold)
                Text("${person.appearanceCount} ${if (person.appearanceCount == 1) "appearance" else "appearances"}", color = Color(0xFF6C5CE7))
            }
        }
        savedMessage?.let { Text(it, color = Color(0xFF2E7D32), modifier = Modifier.padding(top = 10.dp)) }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button({ savedMessage = "Saved as ${CollageExporter.save(context, result.collage)}" }, Modifier.weight(1f)) { Text("Save") }
            Button({ CollageExporter.share(context, result.collage) }, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF241C34))) { Text("Share") }
        }
        TextButton(onClick = onAnother) { Text("Process another video") }
    }
}

@Composable
private fun ErrorScreen(message: String, onReset: () -> Unit, onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(30.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Couldn’t finish processing", fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp)); Text(message, color = Color(0xFF6C6175), textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp)); Button(onClick = onRetry) { Text("Try again") }; TextButton(onClick = onReset) { Text("Choose another video") }
    }
}

@Composable
private fun FrameFriendsTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF6C5CE7), background = Color(0xFFFFF8F2)), content = content)
}
