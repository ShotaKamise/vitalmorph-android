package app.vitalmorph.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vitalmorph.domain.MonsterForm
import app.vitalmorph.domain.MonsterSex
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin

// 演出色。VitaMorphApp.ktのトーンと揃える。
private val HatchBackground = Color(0xFF0B1522)
private val HatchMint = Color(0xFF69E6A6)
private val HatchGold = Color(0xFFFFD166)

// タイムライン(秒)。合計約4秒。
private const val WOBBLE_END = 1.5f
private const val CRACK_END = 2.5f
private const val FLASH_END = 3.0f
private const val TIMELINE_TOTAL = 4.0f

/**
 * 生命卵の孵化オーバーレイ(U4)。画像アセットは使わず、卵はすべてCanvasで描く。
 * モンスター自体の描画は既存の [MonsterVisual] 公開APIのみを使う。
 *
 * @param form 表示する形態(生まれたばかりなので幼生=モルフィを想定)。
 * @param sex 性別マーク表示用。無ければマークを省く。
 * @param onDone 「はじめまして!」で演出を閉じる。
 */
@Composable
fun HatchOverlay(
    form: MonsterForm,
    sex: MonsterSex?,
    onDone: () -> Unit,
) {
    // 単一のAnimatableで0→4秒のタイムラインを駆動する。
    val clock = remember { Animatable(0f) }
    var atEnd by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        // animateToはスキップ時のsnapToで中断されうる。中断後はスキップ側でatEndを立てる。
        runCatching { clock.animateTo(TIMELINE_TOTAL, tween(4_000, easing = LinearEasing)) }
        atEnd = true
    }

    // タップでスキップ: 終了状態(モンスター+テキスト)へ一気に飛ばす。
    fun skip() {
        if (atEnd) return
        scope.launch {
            clock.snapTo(TIMELINE_TOTAL)
            atEnd = true
        }
    }

    val t = clock.value

    // 各パートの進捗を時刻から導出する。
    val crackProgress = ((t - WOBBLE_END) / (CRACK_END - WOBBLE_END)).coerceIn(0f, 1f)
    // 白フラッシュ: 2.5→2.75で最大、3.0で0へ。
    val flashAlpha = if (t in CRACK_END..FLASH_END) {
        sin(((t - CRACK_END) / (FLASH_END - CRACK_END)) * PI).toFloat()
    } else 0f
    // 卵のフェードアウト: 3.0→3.5。
    val eggAlpha = (1f - ((t - FLASH_END) / 0.5f)).coerceIn(0f, 1f)
    // モンスターの出現: 3.0→4.0。オーバーシュート付きのスケールとフェードイン。
    val monsterProgress = ((t - FLASH_END) / (TIMELINE_TOTAL - FLASH_END)).coerceIn(0f, 1f)
    val monsterScale = easeOutBack(monsterProgress)
    val monsterAlpha = (monsterProgress / 0.4f).coerceIn(0f, 1f)
    val textAlpha = ((t - 3.3f) / 0.5f).coerceIn(0f, 1f)

    // ゆらゆら→強い揺れ。振幅と周波数を時間とともに上げる。
    val shakeAmp = when {
        t < WOBBLE_END -> 1f
        t < CRACK_END -> 2.2f
        else -> 0f
    }
    val freq = 6f + t * 4f
    val wave = sin(t.toDouble() * freq * PI).toFloat()
    val eggRotation = wave * 4f * shakeAmp
    val eggHopPx = abs(wave) * 4f * shakeAmp
    val eggShakeX = wave * 3f * (shakeAmp - 1f).coerceAtLeast(0f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(atEnd) {
                detectTapGestures { if (!atEnd) skip() }
            },
        contentAlignment = Alignment.Center,
    ) {
        // 背景(アプリのダーク色)。
        Canvas(Modifier.fillMaxSize()) { drawRect(HatchBackground) }

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier.size(260.dp),
                contentAlignment = Alignment.Center,
            ) {
                // 影(揺れないレイヤー)。
                Canvas(Modifier.fillMaxSize()) {
                    if (eggAlpha > 0f) {
                        val shadowW = size.minDimension * 0.44f
                        val shadowH = size.minDimension * 0.08f
                        drawOval(
                            color = Color.Black.copy(alpha = 0.28f * eggAlpha),
                            topLeft = Offset(
                                (size.width - shadowW) / 2f,
                                size.height * 0.5f + size.minDimension * 0.30f,
                            ),
                            size = Size(shadowW, shadowH),
                        )
                    }
                }
                // 卵本体+ヒビ(揺れるレイヤー)。
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = eggAlpha
                            rotationZ = eggRotation
                            translationX = eggShakeX * density
                            translationY = -eggHopPx * density
                        },
                ) {
                    drawEgg(crackProgress)
                }
                // 生まれたモンスター(スケールイン)。
                if (monsterProgress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = monsterAlpha
                                scaleX = monsterScale
                                scaleY = monsterScale
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        MonsterVisual(form, Modifier.fillMaxSize())
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            // 「(name)が うまれた!」テキスト。
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.graphicsLayer { alpha = textAlpha },
            ) {
                Text(
                    "${form.name}が うまれた!",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                sex?.let {
                    Text(
                        it.mark,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = if (it == MonsterSex.MALE) HatchMint else HatchGold,
                    )
                }
            }
        }

        // 白フラッシュ(全画面のアルファパルス)。
        if (flashAlpha > 0f) {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(Color.White.copy(alpha = flashAlpha))
            }
        }

        // 下部コントロール: 進行中は「タップでスキップ」、終了後は「はじめまして!」。
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom,
        ) {
            if (atEnd) {
                Button(
                    onClick = onDone,
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HatchMint,
                        contentColor = Color(0xFF062216),
                    ),
                ) {
                    Text("はじめまして!", fontWeight = FontWeight.Black, fontSize = 18.sp)
                }
            } else {
                Text(
                    "タップでスキップ",
                    color = Color.White.copy(alpha = 0.55f),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

/** 白〜ミントの卵をヒビ付きで描く。ヒビは[crackProgress]に応じて段階的に伸びる。 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEgg(crackProgress: Float) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val halfW = size.minDimension * 0.26f
    val halfH = size.minDimension * 0.34f
    val eggTopLeft = Offset(cx - halfW, cy - halfH)
    val eggSize = Size(halfW * 2f, halfH * 2f)

    // 白〜ミントのグラデーション。
    drawOval(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFFDFEFF), Color(0xFFDFF7EC), HatchMint.copy(alpha = 0.85f)),
            startY = eggTopLeft.y,
            endY = eggTopLeft.y + eggSize.height,
        ),
        topLeft = eggTopLeft,
        size = eggSize,
    )
    // 斑点をいくつか。
    val spotColor = HatchMint.copy(alpha = 0.35f)
    val spots = listOf(
        Offset(-0.35f, -0.15f) to 0.14f,
        Offset(0.30f, 0.10f) to 0.18f,
        Offset(-0.10f, 0.45f) to 0.12f,
        Offset(0.15f, -0.40f) to 0.10f,
    )
    spots.forEach { (rel, r) ->
        val rw = halfW * r
        drawOval(
            color = spotColor,
            topLeft = Offset(
                cx + rel.x * halfW - rw,
                cy + rel.y * halfH - rw,
            ),
            size = Size(rw * 2f, rw * 2f),
        )
    }

    if (crackProgress <= 0f) return
    // ヒビ: ギザギザの折れ線を段階的に描く。座標は卵の半径で正規化。
    val cracks = listOf(
        listOf(
            Offset(0.0f, -0.6f), Offset(0.14f, -0.35f), Offset(-0.08f, -0.12f),
            Offset(0.16f, 0.12f), Offset(-0.04f, 0.4f),
        ),
        listOf(
            Offset(-0.55f, -0.1f), Offset(-0.3f, -0.02f), Offset(-0.12f, -0.14f),
            Offset(0.08f, -0.05f),
        ),
        listOf(
            Offset(0.5f, -0.25f), Offset(0.28f, -0.1f), Offset(0.34f, 0.12f),
            Offset(0.12f, 0.28f),
        ),
        listOf(
            Offset(-0.2f, 0.55f), Offset(-0.05f, 0.34f), Offset(-0.18f, 0.16f),
        ),
    )
    val crackColor = Color(0xFF20303F)
    cracks.forEachIndexed { index, crack ->
        // ヒビごとに少しずつ遅らせて伸ばす。
        val local = ((crackProgress - index * 0.08f) / (1f - index * 0.08f)).coerceIn(0f, 1f)
        drawCrack(crack, local, cx, cy, halfW, halfH, crackColor)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCrack(
    points: List<Offset>,
    progress: Float,
    cx: Float,
    cy: Float,
    halfW: Float,
    halfH: Float,
    color: Color,
) {
    if (progress <= 0f || points.size < 2) return
    val stroke = size.minDimension * 0.008f
    val totalSegs = points.size - 1
    val reveal = progress * totalSegs
    fun map(o: Offset) = Offset(cx + o.x * halfW, cy + o.y * halfH)
    for (i in 0 until totalSegs) {
        val f = (reveal - i).coerceIn(0f, 1f)
        if (f <= 0f) break
        val start = map(points[i])
        val fullEnd = map(points[i + 1])
        val end = Offset(
            start.x + (fullEnd.x - start.x) * f,
            start.y + (fullEnd.y - start.y) * f,
        )
        drawLine(color, start, end, strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

/** easeOutBack: 0→1でわずかに1を超えて戻る、オーバーシュート付きスケール。 */
private fun easeOutBack(x: Float): Float {
    if (x <= 0f) return 0f
    if (x >= 1f) return 1f
    val c1 = 1.70158f
    val c3 = c1 + 1f
    return 1f + c3 * (x - 1f).pow(3) + c1 * (x - 1f).pow(2)
}
