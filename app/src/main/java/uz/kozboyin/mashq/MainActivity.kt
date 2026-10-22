package uz.kozboyin.mashq

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "screen_check",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<ScreenTimeWorker>(15, TimeUnit.MINUTES).build()
        )

        setContent { AppTheme { App() } }
    }
}

@Composable
fun App() {
    val ctx = LocalContext.current
    var screen by rememberSaveable { mutableStateOf("home") }
    when (screen) {
        "run" -> ExerciseScreen(
            onFinished = {
                ScreenTime.markDone(ctx)
                screen = "end"
            },
            onExit = { screen = "home" }
        )
        "end" -> EndScreen(onBack = { screen = "home" })
        else -> HomeScreen(onStart = { screen = "run" })
    }
}
