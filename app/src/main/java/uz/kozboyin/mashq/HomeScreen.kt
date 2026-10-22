package uz.kozboyin.mashq

import android.Manifest
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay

/** Daqiqani "45 daqiqa", "1 soat", "1 soat 30 daqiqa" ko'rinishida yozadi. */
private fun fmtMin(m: Int): String = when {
    m < 60 -> "$m daqiqa"
    m % 60 == 0 -> "${m / 60} soat"
    else -> "${m / 60} soat ${m % 60} daqiqa"
}

private fun bigTime(ms: Long, muted: Color): AnnotatedString = buildAnnotatedString {
    val m = (ms / 60_000L).toInt()
    val small = SpanStyle(fontSize = 20.sp, color = muted, fontWeight = FontWeight.SemiBold)
    if (m >= 60) {
        append("${m / 60}")
        withStyle(small) { append(" s ") }
        append("${m % 60}")
        withStyle(small) { append(" daq") }
    } else {
        append("$m")
        withStyle(small) { append(" daq") }
    }
}

@Composable
fun HomeScreen(onStart: () -> Unit) {
    val c = LocalAppColors.current
    val ctx = LocalContext.current

    var tick by remember { mutableIntStateOf(0) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) tick++ }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            tick++
        }
    }

    var interval by remember { mutableIntStateOf(Prefs.intervalMin(ctx)) }
    var menu by remember { mutableStateOf(false) }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }

    val hasAccess = remember(tick) { ScreenTime.hasUsageAccess(ctx) }
    val notifOk = remember(tick) { Notifier.hasPermission(ctx) }
    val used = remember(tick) { if (hasAccess) ScreenTime.todayMs(ctx) else 0L }
    val since = remember(tick, interval) { Prefs.sinceMs(ctx, used) }
    val limitMs = interval * 60_000L
    val due = hasAccess && since >= limitMs
    val frac = (since.toFloat() / limitMs).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        Text("Ko'z va bo'yin mashqlari", color = c.ink, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)

        Column {
            Text("Bugungi ekran vaqti", color = c.muted, fontSize = 15.sp)
            Spacer(Modifier.height(6.dp))
            Text(bigTime(used, c.muted), color = c.ink, fontSize = 56.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(c.line)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(frac)
                        .fillMaxHeight()
                        .background(c.accent)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Oxirgi mashqdan beri: ${fmtMin((since / 60_000L).toInt())} / ${fmtMin(interval)}",
                color = c.muted, fontSize = 13.sp
            )
        }

        Column {
            Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
            Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Eslatma oralig'i", color = c.ink, fontSize = 16.sp)
                Box {
                    TextButton(onClick = { menu = true }) {
                        Text(fmtMin(interval), color = c.ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        listOf(30, 60, 120, 180, 240).forEach { m ->
                            DropdownMenuItem(
                                text = { Text(fmtMin(m)) },
                                onClick = {
                                    interval = m
                                    Prefs.setIntervalMin(ctx, m)
                                    menu = false
                                }
                            )
                        }
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
        }

        if (due) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(c.soft, RoundedCornerShape(14.dp))
                    .border(1.dp, c.accent, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Text(
                    "Mashq vaqti. Ekranga uzoq qaradingiz. Ko'z va bo'yin uchun qisqa mashq qiling.",
                    color = c.ink, fontSize = 15.sp
                )
            }
        }

        if (!hasAccess) {
            SetupCard(
                text = "Ekran vaqtini hisoblash uchun \"Foydalanish statistikasiga kirish\" ruxsatini yoqing.",
                button = "Ruxsat berish"
            ) {
                ctx.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
        if (!notifOk) {
            SetupCard(
                text = "Eslatma ko'rinishi uchun bildirishnomalarga ruxsat bering.",
                button = "Bildirishnomaga ruxsat"
            ) {
                permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.onAccent)
        ) {
            Text("Boshlash", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }

        TextButton(
            onClick = {
                val ok = Notifier.send(ctx)
                if (!ok) Toast.makeText(ctx, "Bildirishnoma yuborilmadi: ruxsatni tekshiring", Toast.LENGTH_LONG).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Bildirishnomani sinab ko'rish", color = c.muted, fontSize = 15.sp)
        }
    }
}

@Composable
private fun SetupCard(text: String, button: String, onClick: () -> Unit) {
    val c = LocalAppColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, c.line, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text, color = c.ink, fontSize = 15.sp)
        TextButton(onClick = onClick) {
            Text(button, color = c.accent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}
