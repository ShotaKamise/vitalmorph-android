package app.vitalmorph.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vitalmorph.domain.MacroNutrient
import app.vitalmorph.domain.MiniGameKind
import app.vitalmorph.domain.MiniGameRules
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val OverlayBackground = Color(0xFF0B1522)
private val CoreColor = Color(0xFF69E6A6)
private val AccentGold = Color(0xFFFFD166)

/**
 * ミニゲームの全画面オーバーレイ。タイミング・描画のみを持ち、
 * 判定とスコアは [MiniGameRules] に委ねる。
 */
@Composable
fun MiniGameOverlay(
    kind: MiniGameKind,
    seed: Int,
    onFinish: (Int) -> Unit,
    onCancel: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = OverlayBackground) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(kind.label, fontSize = 22.sp, fontWeight = FontWeight.Black, color = CoreColor)
                    Text(kind.summary, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                }
                OutlinedButton(onClick = onCancel) { Text("やめる") }
            }
            Spacer(Modifier.height(16.dp))
            when (kind) {
                MiniGameKind.CORE_CATCH -> CoreCatchGame(seed, onFinish)
                MiniGameKind.PULSE_TRAINING -> PulseTrainingGame(onFinish)
                MiniGameKind.MEAL_BALANCE -> MealBalanceGame(seed, onFinish)
            }
        }
    }
}

@Composable
private fun CoreCatchGame(seed: Int, onFinish: (Int) -> Unit) {
    val cells = remember(seed) { MiniGameRules.coreCatchCells(seed) }
    var round by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var activeCell by remember { mutableIntStateOf(-1) }

    LaunchedEffect(round) {
        if (round >= cells.size) {
            onFinish(score)
            return@LaunchedEffect
        }
        activeCell = cells[round]
        delay(MiniGameRules.CORE_CATCH_SHOW_MS)
        activeCell = -1
        delay(MiniGameRules.CORE_CATCH_INTERVAL_MS)
        round++
    }

    Column {
        GameStatusRow("コア ${round.coerceAtMost(cells.size)} / ${cells.size}", "スコア $score")
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(3) { rowIndex ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(3) { colIndex ->
                        val cellIndex = rowIndex * 3 + colIndex
                        val isActive = cellIndex == activeCell
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .background(
                                    if (isActive) CoreColor.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.06f),
                                    RoundedCornerShape(18.dp),
                                )
                                .clickable(enabled = isActive) {
                                    if (cellIndex == activeCell) {
                                        score++
                                        activeCell = -1
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isActive) Text("●", fontSize = 34.sp, color = OverlayBackground)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            "${MiniGameRules.CORE_CATCH_SUCCESS_SCORE}個キャッチでクリア！",
            style = MaterialTheme.typography.bodySmall,
            color = AccentGold,
        )
    }
}

@Composable
private fun PulseTrainingGame(onFinish: (Int) -> Unit) {
    var round by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var lastJudge by remember { mutableStateOf<String?>(null) }
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(round) {
        if (round >= MiniGameRules.PULSE_ROUNDS) {
            onFinish(score)
            return@LaunchedEffect
        }
        progress.snapTo(0f)
        progress.animateTo(1f, tween(MiniGameRules.PULSE_CYCLE_MS, easing = LinearEasing))
        // タップされないまま閉じ切ったらミス扱いで次へ。
        lastJudge = "ミス…"
        round++
    }

    fun tap() {
        if (round >= MiniGameRules.PULSE_ROUNDS) return
        val gained = MiniGameRules.pulseJudge(progress.value)
        score += gained
        lastJudge = when (gained) {
            3 -> "パーフェクト！ +3"
            2 -> "グッド！ +2"
            1 -> "オーケー +1"
            else -> "はやい…"
        }
        scope.launch { progress.stop() }
        round++
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        GameStatusRow("パルス ${round.coerceAtMost(MiniGameRules.PULSE_ROUNDS)} / ${MiniGameRules.PULSE_ROUNDS}", "スコア $score")
        Spacer(Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .size(260.dp)
                .clickable { tap() },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val target = size.minDimension * 0.22f
                val outer = size.minDimension * 0.48f
                // ターゲットリング
                drawCircle(color = AccentGold, radius = target, style = Stroke(width = 6.dp.toPx()))
                // 縮んでいくパルス
                val current = outer - (outer - target) * progress.value
                drawCircle(color = CoreColor, radius = current, style = Stroke(width = 10.dp.toPx()))
            }
            Text("タップ！", color = Color.White.copy(alpha = 0.6f))
        }
        Spacer(Modifier.height(12.dp))
        Text(lastJudge ?: "リングが重なる瞬間にタップ", color = AccentGold, fontWeight = FontWeight.Bold)
        Text(
            "${MiniGameRules.PULSE_SUCCESS_SCORE}点以上でクリア！",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun MealBalanceGame(seed: Int, onFinish: (Int) -> Unit) {
    val questions = remember(seed) { MiniGameRules.mealRound(seed) }
    var index by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var feedback by remember { mutableStateOf<String?>(null) }

    if (index >= questions.size) {
        LaunchedEffect(Unit) { onFinish(score) }
        return
    }
    val question = questions[index]

    Column(modifier = Modifier.fillMaxWidth()) {
        GameStatusRow("もんだい ${index + 1} / ${questions.size}", "正解 $score")
        Spacer(Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
                .padding(vertical = 36.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(question.foodName, fontSize = 30.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(10.dp))
        Text("いちばん多い栄養素はどれ？", color = Color.White.copy(alpha = 0.7f))
        Spacer(Modifier.height(10.dp))
        MacroNutrient.entries.forEach { macro ->
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .height(48.dp),
                onClick = {
                    val correct = macro == question.answer
                    if (correct) score++
                    feedback = if (correct) {
                        "せいかい！ ${question.foodName}は${question.answer.label}が豊富。"
                    } else {
                        "${question.foodName}は${question.answer.label}が多いんだ。おぼえておこう！"
                    }
                    index++
                },
            ) { Text(macro.label) }
        }
        feedback?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = AccentGold, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "${MiniGameRules.MEAL_SUCCESS_SCORE}問正解でクリア！ まちがえても大丈夫。",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun GameStatusRow(left: String, right: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(left, fontWeight = FontWeight.Bold)
        Text(right, fontWeight = FontWeight.Bold, color = AccentGold)
    }
}
