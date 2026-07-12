package app.vitalmorph.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import app.vitalmorph.domain.BattleEffectType
import app.vitalmorph.domain.TurnEvent
import app.vitalmorph.domain.effectFor
import kotlin.math.cos
import kotlin.math.sin

private val SlashColor = Color(0xFFFFF3B0)
private val BurstColor = Color(0xFFFFC24B)
private val ShieldColor = Color(0xFF7FE0FF)
private val HealColor = Color(0xFF7BF0A8)

/**
 * ターンイベントに応じたバトルエフェクトを1回だけ再生するオーバーレイ。
 * event(インスタンス)をキーにするため、同じイベントが渡っている間は再生し直さない。
 * DAMAGE_DEALT→斬撃/閃光、GUARD→シールド、HEAL→回復光。該当なしは何も描かない。
 */
@Composable
fun BattleEffectOverlay(event: TurnEvent?, modifier: Modifier = Modifier) {
    val type = event?.let { effectFor(it) } ?: return
    val progress = remember(event) { Animatable(0f) }
    LaunchedEffect(event) {
        val duration = when (type) {
            BattleEffectType.SLASH -> 600
            BattleEffectType.BURST -> 700
            BattleEffectType.SHIELD -> 750
            BattleEffectType.HEAL -> 800
        }
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = duration, easing = LinearEasing))
    }
    when (type) {
        BattleEffectType.SLASH -> SlashEffect(progress.value, modifier)
        BattleEffectType.BURST -> BurstEffect(progress.value, modifier)
        BattleEffectType.SHIELD -> ShieldEffect(progress.value, modifier)
        BattleEffectType.HEAL -> HealEffect(progress.value, modifier)
    }
}

/** 対象を横切る2〜3本の斬撃線(通常攻撃)。 */
@Composable
fun SlashEffect(progress: Float, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        val slashes = 3
        for (i in 0 until slashes) {
            // 各線は少し時間差で現れて消える。
            val local = ((progress - i * 0.12f) / 0.5f).coerceIn(0f, 1f)
            if (local <= 0f || local >= 1f) continue
            val alpha = sin(local * Math.PI).toFloat()
            val yOffset = (i - 1) * size.height * 0.18f
            // 左上から右下へ流れる対角線を、progressで伸ばしていく。
            val sweep = local
            val startX = size.width * (0.05f + 0.2f * i)
            val start = Offset(startX, size.height * 0.15f + yOffset)
            val end = Offset(
                startX + size.width * 0.7f * sweep,
                start.y + size.height * 0.7f * sweep,
            )
            drawLine(
                color = SlashColor.copy(alpha = alpha * 0.9f),
                start = start,
                end = end,
                strokeWidth = 5.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

/** 中心から放射する閃光と光条(必殺・強打)。 */
@Composable
fun BurstEffect(progress: Float, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        val alpha = sin(progress * Math.PI).toFloat()
        val maxR = size.minDimension * 0.55f
        // 中心のフラッシュ。
        drawCircle(
            color = BurstColor.copy(alpha = alpha * 0.4f),
            radius = maxR * (0.3f + progress * 0.7f),
            center = center,
        )
        // 放射状のスパイク。
        val spikes = 10
        for (i in 0 until spikes) {
            val angle = (i.toFloat() / spikes) * 2f * Math.PI
            val inner = maxR * 0.25f * progress
            val outer = maxR * (0.4f + progress * 0.9f)
            val dir = Offset(cos(angle).toFloat(), sin(angle).toFloat())
            drawLine(
                color = BurstColor.copy(alpha = alpha),
                start = center + dir * inner,
                end = center + dir * outer,
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

/** 広がる六角形の輪郭とグロー(ガード)。 */
@Composable
fun ShieldEffect(progress: Float, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        val alpha = sin(progress * Math.PI).toFloat()
        val radius = size.minDimension * (0.28f + progress * 0.22f)
        // グロー用の淡い円。
        drawCircle(
            color = ShieldColor.copy(alpha = alpha * 0.18f),
            radius = radius * 1.05f,
            center = center,
        )
        // 六角形の輪郭。
        val path = androidx.compose.ui.graphics.Path()
        for (i in 0..6) {
            val angle = (Math.PI / 6) + (i.toFloat() / 6) * 2f * Math.PI
            val p = center + Offset(cos(angle).toFloat(), sin(angle).toFloat()) * radius
            if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        drawPath(
            path = path,
            color = ShieldColor.copy(alpha = alpha),
            style = Stroke(width = 3.dp.toPx()),
        )
    }
}

/** 上昇して消える緑のプラス光(回復)。 */
@Composable
fun HealEffect(progress: Float, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        val plusCount = 5
        for (i in 0 until plusCount) {
            val phase = ((progress - i * 0.1f)).coerceIn(0f, 1f)
            if (phase <= 0f) continue
            val alpha = (1f - phase)
            val x = size.width * (0.25f + 0.12f * i)
            val y = size.height * (0.85f - phase * 0.6f)
            val half = 8.dp.toPx()
            val color = HealColor.copy(alpha = alpha)
            drawLine(color, Offset(x - half, y), Offset(x + half, y), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
            drawLine(color, Offset(x, y - half), Offset(x, y + half), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
        }
    }
}

/** 感情エモートの種類。♪ / 💢 / 💧 / ♥ を上に浮かせてフェードで表現する。 */
enum class Emote(val symbol: String, val tint: Color) {
    HAPPY_NOTE("♪", Color(0xFF69E6A6)),
    ANGRY("💢", Color(0xFFFF6B6B)),
    SWEAT("💧", Color(0xFF7FC8FF)),
    HEART("♥", Color(0xFFFF8AB0)),
}

/**
 * モンスター右上に感情エモートを1回だけ浮かべるオーバーレイ。
 * emote(値)が変わったとき約1.2秒かけて上昇+フェードする。
 */
@Composable
fun EmoteOverlay(emote: Emote?, modifier: Modifier = Modifier) {
    emote ?: return
    val progress = remember(emote) { Animatable(0f) }
    LaunchedEffect(emote) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = 1_200, easing = LinearEasing))
    }
    val p = progress.value
    if (p >= 1f) return
    val alpha = sin(p * Math.PI).toFloat().coerceIn(0f, 1f)
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
        Text(
            text = emote.symbol,
            color = emote.tint.copy(alpha = alpha),
            fontWeight = FontWeight.Black,
            fontSize = 26.sp,
            modifier = Modifier
                .padding(top = 10.dp, end = 14.dp)
                .graphicsLayerOffsetY(-p * 40f),
        )
    }
}

/** テキストを上方向へオフセットするための軽量ヘルパ(dp換算はグラフィックスレイヤ側)。 */
private fun Modifier.graphicsLayerOffsetY(offsetDp: Float): Modifier =
    this.then(
        Modifier.graphicsLayer {
            translationY = offsetDp * density
        },
    )
