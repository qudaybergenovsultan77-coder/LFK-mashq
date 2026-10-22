package uz.kozboyin.mashq

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Qovoq yopilish darajasi (0 = ochiq, 1 = yopiq): 4 soniyada 3 marta pirpiratadi. */
private val BLINK_KEYS = listOf(
    0f to 0f, 0.08f to 0f, 0.11f to 1f, 0.14f to 0f,
    0.20f to 0f, 0.23f to 1f, 0.26f to 0f,
    0.32f to 0f, 0.35f to 1f, 0.38f to 0f, 1f to 0f
)

/** Qorachiqning yuqoriga-pastga siljishi (piksel, 400x400 koordinatada). */
private val LOOK_KEYS = listOf(
    0f to 0f, 0.15f to -10f, 0.35f to -10f, 0.5f to 0f,
    0.65f to 10f, 0.85f to 10f, 1f to 0f
)

/** Qorachiqning chapga-o'ngga siljishi (piksel, 400x400 koordinatada). */
private val LOOK_X_KEYS = listOf(
    0f to 0f, 0.15f to -9f, 0.35f to -9f, 0.5f to 0f,
    0.65f to 9f, 0.85f to 9f, 1f to 0f
)

/** Boshni yonga egish burchagi (gradus): chapga -10, o'ngga +10, 4 soniyada bir sikl. */
private val TILT_KEYS = listOf(
    0f to 0f, 0.25f to -10f, 0.5f to 0f, 0.75f to 10f, 1f to 0f
)

/** Bosh egilishi: pastga siljish (px) va qisilish (scaleY), 4 soniyada bir sikl. */
private val NOD_TY_KEYS = listOf(0f to 0f, 0.25f to 4f, 0.5f to 0f, 0.75f to -2f, 1f to 0f)
private val NOD_SY_KEYS = listOf(0f to 1f, 0.25f to 0.94f, 0.5f to 1f, 0.75f to 1.05f, 1f to 1f)

/** Boshni yon tomonga burish: siljish (px) va torayish (scaleX), 4 soniyada bir sikl. */
private val TURN_TX_KEYS = listOf(0f to 0f, 0.25f to -10f, 0.5f to 0f, 0.75f to 10f, 1f to 0f)
private val TURN_SX_KEYS = listOf(0f to 1f, 0.25f to 0.93f, 0.5f to 1f, 0.75f to 0.93f, 1f to 1f)

private fun ease(x: Float) = x * x * (3f - 2f * x)

private fun interp(keys: List<Pair<Float, Float>>, p: Float, smooth: Boolean): Float {
    for (i in 0 until keys.size - 1) {
        val (p0, v0) = keys[i]
        val (p1, v1) = keys[i + 1]
        if (p >= p0 && p <= p1) {
            var k = if (p1 == p0) 0f else (p - p0) / (p1 - p0)
            if (smooth) k = ease(k)
            return v0 + (v1 - v0) * k
        }
    }
    return 0f
}

/** Barcha 7 ta mashq uchun bola personaji tayyor. */
@Suppress("UNUSED_PARAMETER")
fun hasBoy(type: Anim): Boolean = true

private class Dot(val x: Float, val y: Float, val r: Float, val shift: Float)

private val DOTS = listOf(
    Dot(122f, 205f, 4f, 0f), Dot(132f, 262f, 3f, 0.8f), Dot(196f, 198f, 3.5f, 0.4f),
    Dot(190f, 258f, 3f, 1.2f), Dot(140f, 180f, 3f, 1.6f), Dot(278f, 205f, 4f, 0.8f),
    Dot(268f, 262f, 3f, 0f), Dot(204f, 258f, 3.5f, 1.2f), Dot(260f, 180f, 3f, 0.4f),
    Dot(212f, 212f, 3f, 1.6f)
)

/** Multfilm bola (doira ichida). type = mashq turi, time = soniyalarda animatsiya vaqti. */
@Composable
fun Boy(type: Anim, time: Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.width / 400f
        val circle = Path().apply { addOval(Rect(Offset.Zero, size)) }
        val p4 = (time % 4f) / 4f
        val lid = interp(BLINK_KEYS, p4, false)
        val w4 = 2f * PI.toFloat() * p4
        val headAngle = if (type == Anim.TILT) interp(TILT_KEYS, p4, true) else 0f
        val headPivot = Offset(200f, 340f)
        val headTx = if (type == Anim.TURN) interp(TURN_TX_KEYS, p4, true) else 0f
        val headSx = if (type == Anim.TURN) interp(TURN_SX_KEYS, p4, true) else 1f
        val headTy = if (type == Anim.NOD) interp(NOD_TY_KEYS, p4, true) else 0f
        val headSy = if (type == Anim.NOD) interp(NOD_SY_KEYS, p4, true) else 1f
        // Aylana: soat strelkasi bo'yicha, radiusi 9, 4 soniyada bir marta aylanadi
        val pdy = when (type) {
            Anim.UP_DOWN -> interp(LOOK_KEYS, p4, true)
            Anim.CIRCLE -> -9f * cos(w4)
            else -> 0f
        }
        val pdx = when (type) {
            Anim.LEFT_RIGHT -> interp(LOOK_X_KEYS, p4, true)
            Anim.CIRCLE -> 9f * sin(w4)
            else -> 0f
        }
        val skinDark = Color(0xFFEDAE82)
        val hair = Color(0xFF14141A)
        val round = StrokeCap.Round

        clipPath(circle) {
            scale(s, s, Offset.Zero) {
                // fon
                drawRect(Color(0xFFFFD93B), Offset.Zero, Size(400f, 400f))

                // bosh (bo'yin asosi atrofida buriladi)
                withTransform({
                    rotate(headAngle, headPivot)
                    translate(headTx, headTy)
                    scale(headSx, headSy, headPivot)
                }) {
                // quloqlar va yuz
                drawOval(skinDark, Offset(78f, 201f), Size(28f, 48f))
                drawOval(skinDark, Offset(294f, 201f), Size(28f, 48f))
                drawOval(
                    brush = Brush.radialGradient(
                        0f to Color(0xFFF8CBA1), 1f to skinDark,
                        center = Offset(200f, 185.6f), radius = 146f
                    ),
                    topLeft = Offset(88f, 88f), size = Size(224f, 244f)
                )

                // soch
                val hairPath = Path().apply {
                    moveTo(92f, 205f); lineTo(78f, 125f); lineTo(122f, 148f); lineTo(128f, 66f)
                    lineTo(168f, 118f); lineTo(200f, 44f); lineTo(232f, 118f); lineTo(272f, 66f)
                    lineTo(278f, 148f); lineTo(322f, 125f); lineTo(308f, 205f)
                    quadraticBezierTo(300f, 150f, 200f, 150f)
                    quadraticBezierTo(100f, 150f, 92f, 205f)
                    close()
                }
                drawPath(hairPath, hair)

                // qoshlar
                val brow1 = Path().apply { moveTo(130f, 190f); quadraticBezierTo(160f, 176f, 188f, 190f) }
                val brow2 = Path().apply { moveTo(212f, 190f); quadraticBezierTo(240f, 176f, 270f, 190f) }
                drawPath(brow1, hair, style = Stroke(7f, cap = round))
                drawPath(brow2, hair, style = Stroke(7f, cap = round))

                // ko'zlar
                for (cx in listOf(160f, 240f)) {
                    drawOval(Color.White, Offset(cx - 24f, 197f), Size(48f, 56f))
                    drawCircle(Color(0xFF2B1B10), 14f, Offset(cx + 2f + pdx, 228f + pdy))
                    drawCircle(Color.Black, 7f, Offset(cx + 2f + pdx, 228f + pdy))
                    drawCircle(Color.White, 4f, Offset(cx + 7f + pdx, 221f + pdy))
                    // qovoq (yuqoridan tushadi)
                    if (lid > 0.01f) {
                        scale(1f, lid, Offset(cx, 195f)) {
                            drawOval(Color(0xFFF2B98C), Offset(cx - 26f, 195f), Size(52f, 60f))
                        }
                    }
                }

                // burun, og'iz, yonoq
                val nose = Path().apply { moveTo(200f, 245f); quadraticBezierTo(194f, 262f, 204f, 264f) }
                drawPath(nose, Color(0xFFD9966A), style = Stroke(4f, cap = round))
                val mouth = Path().apply { moveTo(170f, 285f); quadraticBezierTo(200f, 305f, 230f, 285f) }
                drawPath(mouth, Color(0xFF8A4B33), style = Stroke(5f, cap = round))
                drawOval(Color(0x80F29A8A), Offset(114f, 254f), Size(28f, 16f))
                drawOval(Color(0x80F29A8A), Offset(258f, 254f), Size(28f, 16f))

                }

                // hoodie
                val hoodie = Path().apply {
                    moveTo(0f, 400f); lineTo(0f, 378f)
                    quadraticBezierTo(40f, 345f, 120f, 338f)
                    quadraticBezierTo(200f, 388f, 280f, 338f)
                    quadraticBezierTo(360f, 345f, 400f, 378f)
                    lineTo(400f, 400f); close()
                }
                drawPath(hoodie, Color(0xFF101014))
                val neck = Path().apply { moveTo(120f, 338f); quadraticBezierTo(200f, 388f, 280f, 338f) }
                drawPath(neck, Color(0xFF2A2A33), style = Stroke(5f))
                drawLine(Color(0xFFE9E9EE), Offset(178f, 362f), Offset(176f, 398f), 4f, round)
                drawLine(Color(0xFFE9E9EE), Offset(222f, 362f), Offset(224f, 398f), 4f, round)

                // yashil bo'g'im nuqtalari (nur bilan pulsatsiya)
                val green = Color(0xFF39FF6A)
                withTransform({
                    rotate(headAngle, headPivot)
                    translate(headTx, headTy)
                    scale(headSx, headSy, headPivot)
                }) {
                for (d in DOTS) {
                    val ph = (time + d.shift) / 2f
                    val u = (1f - cos(2f * PI.toFloat() * ph)) / 2f
                    val alpha = 0.25f + 0.75f * u
                    val sc = 0.7f + 0.5f * u
                    drawCircle(green.copy(alpha = 0.30f * alpha), d.r * sc * 3f, Offset(d.x, d.y))
                    drawCircle(green.copy(alpha = alpha), d.r * sc, Offset(d.x, d.y))
                }
                }
            }
        }
    }
}
