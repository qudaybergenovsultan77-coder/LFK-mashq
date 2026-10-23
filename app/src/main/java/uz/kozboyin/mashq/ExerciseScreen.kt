package uz.kozboyin.mashq

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ExerciseScreen(onFinished: () -> Unit, onExit: () -> Unit) {
    val c = LocalAppColors.current
    var index by rememberSaveable { mutableIntStateOf(0) }
    var left by rememberSaveable { mutableIntStateOf(EXERCISES[0].seconds) }
    var running by rememberSaveable { mutableStateOf(true) }
    var anim by remember { mutableFloatStateOf(0f) }
    val ex = EXERCISES[index]

    BackHandler(onBack = onExit)

    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    // Animatsiya vaqti (to'xtatilganda to'xtaydi)
    LaunchedEffect(running) {
        if (running) {
            var last = withFrameNanos { it }
            while (true) {
                val now = withFrameNanos { it }
                anim += (now - last) / 1_000_000_000f
                last = now
            }
        }
    }

    // Soniyalarni sanash va keyingi mashqqa o'tish
    LaunchedEffect(running, index) {
        if (running) {
            while (left > 0) {
                delay(1000)
                left--
            }
            if (index + 1 < EXERCISES.size) {
                index++
                left = EXERCISES[index].seconds
            } else {
                onFinished()
            }
        }
    }

    val elapsed = ex.seconds - left
    val hint = if ((elapsed / 4) % 2 == 0) "Nafas oling" else "Nafas chiqaring"

    Column(
        modifier = Modifier
            .systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 28.dp)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            EXERCISES.indices.forEach { i ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (i < index) c.accent else if (i == index) c.ink else c.line)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Box(Modifier.size(280.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val breath = 0.9f + 0.1f * ((1f - cos(2f * PI.toFloat() * anim / 8f)) / 2f)
                val r = size.minDimension / 2f
                val sw = 3.dp.toPx()
                drawCircle(c.soft, radius = r * 0.88f * breath)
                drawCircle(c.line, radius = r - sw, style = Stroke(sw))
                drawArc(
                    color = c.accent,
                    startAngle = -90f,
                    sweepAngle = 360f * left / ex.seconds,
                    useCenter = false,
                    topLeft = Offset(sw, sw),
                    size = Size(size.width - 2 * sw, size.height - 2 * sw),
                    style = Stroke(sw, cap = StrokeCap.Round)
                )
            }
            if (hasBoy(ex.anim)) {
                // Personajli animatsiya: doira nafas bilan birga kattalashib-kichrayadi
                val breath = 0.94f + 0.06f * ((1f - cos(2f * PI.toFloat() * anim / 8f)) / 2f)
                Boy(
                    ex.anim,
                    anim,
                    Modifier
                        .size(236.dp)
                        .graphicsLayer { scaleX = breath; scaleY = breath }
                )
                Text(
                    "$left",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 14.dp)
                        .background(Color(0xCC14231C), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                )
            } else {
                Illustration(ex.anim, anim, Modifier.size(150.dp).offset(y = (-16).dp))
                Text(
                    "$left",
                    color = c.ink,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 36.dp)
                )
            }
        }

        Text(hint, color = c.accent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(ex.name, color = c.ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        Text(ex.desc, color = c.muted, fontSize = 15.sp, textAlign = TextAlign.Center, lineHeight = 22.sp)

        Spacer(Modifier.weight(1f))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { running = !running }, modifier = Modifier.weight(1f)) {
                Text(if (running) "To'xtatish" else "Davom etish", color = c.muted, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            TextButton(
                onClick = {
                    if (index + 1 < EXERCISES.size) {
                        index++
                        left = EXERCISES[index].seconds
                    } else {
                        onFinished()
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("O'tkazish", color = c.muted, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Chiziqli odam rasmi (v2 dizayndagi kabi): ko'z, boshni egish, burish va h.k. */
@Composable
fun Illustration(type: Anim, time: Float, modifier: Modifier = Modifier) {
    val c = LocalAppColors.current
    Canvas(modifier) {
        val s = size.width / 120f
        val ink = c.ink
        val stroke = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round)

        val period = when (type) {
            Anim.BLINK -> 2.2f
            Anim.UP_DOWN, Anim.LEFT_RIGHT -> 2.4f
            Anim.CIRCLE -> 3.0f
            Anim.TILT, Anim.NOD -> 3.2f
            Anim.TURN -> 3.6f
        }
        val t = (time % period) / period
        val w = 2f * PI.toFloat() * t

        var pdx = 0f
        var pdy = 0f
        var blink = 1f
        var angle = 0f
        var ty = 0f
        var sy = 1f
        var fdx = 0f
        when (type) {
            Anim.BLINK -> blink = if (t in 0.72f..0.88f) 0.1f else 1f
            Anim.UP_DOWN -> pdy = -4f * cos(w)
            Anim.LEFT_RIGHT -> pdx = -4f * cos(w)
            Anim.CIRCLE -> { pdx = 4f * sin(w); pdy = -4f * cos(w) }
            Anim.TILT -> angle = 20f * sin(w)
            Anim.NOD -> { val u = (1f - cos(w)) / 2f; ty = -3f + 10f * u; sy = 1.02f - 0.12f * u }
            Anim.TURN -> fdx = 16f * sin(w)
        }

        scale(s, s, pivot = Offset.Zero) {
            // yelka va bo'yin
            val body = Path().apply {
                moveTo(8f, 120f); cubicTo(10f, 100f, 30f, 96f, 52f, 94f)
                moveTo(68f, 94f); cubicTo(90f, 96f, 110f, 100f, 112f, 120f)
                moveTo(52f, 80f); lineTo(52f, 94f)
                moveTo(68f, 80f); lineTo(68f, 94f)
            }
            drawPath(body, ink, style = stroke)

            val pivot = Offset(60f, 92f)
            withTransform({
                translate(0f, ty)
                scale(1f, sy, pivot)
                rotate(angle, pivot)
            }) {
                drawOval(ink, topLeft = Offset(30f, 18f), size = Size(60f, 68f), style = stroke)
                translate(fdx, 0f) {
                    listOf(48f, 72f).forEach { cx ->
                        scale(1f, blink, Offset(cx, 48f)) {
                            drawOval(ink, topLeft = Offset(cx - 8f, 42f), size = Size(16f, 12f), style = stroke)
                            drawCircle(c.accent, radius = 3.2f, center = Offset(cx + pdx, 48f + pdy))
                        }
                    }
                    val nose = Path().apply {
                        moveTo(60f, 54f); lineTo(58f, 63f); lineTo(62f, 63f)
                    }
                    drawPath(nose, ink, style = stroke)
                    val mouth = Path().apply {
                        moveTo(52f, 72f); quadraticBezierTo(60f, 77f, 68f, 72f)
                    }
                    drawPath(mouth, ink, style = stroke)
                }
            }
        }
    }
}

@Composable
fun EndScreen(onBack: () -> Unit) {
    val c = LocalAppColors.current
    Column(
        modifier = Modifier
            .systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 28.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        Text("Mashqlar tugadi", color = c.ink, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        Text(
            "Ko'zlaringiz va bo'yningiz biroz dam oldi. Ekranga qaytishingiz mumkin.",
            color = c.muted, fontSize = 15.sp, lineHeight = 22.sp
        )
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.onAccent)
        ) {
            Text("Bosh sahifaga qaytish", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }
}
