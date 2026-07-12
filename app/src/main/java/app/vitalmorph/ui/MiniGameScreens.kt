package app.vitalmorph.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.mutableLongStateOf
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
import app.vitalmorph.domain.MiniGameDifficulty
import app.vitalmorph.domain.MiniGameKind
import app.vitalmorph.domain.MiniGameRules
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val OverlayBackground = Color(0xFF0B1522)
private val CoreColor = Color(0xFF69E6A6)
private val AccentGold = Color(0xFFFFD166)
private val MissRed = Color(0xFFFF6B6B)

/**
 * ミニゲームの全画面オーバーレイ。まず難易度を選び、その後に各ゲームを描画する。
 * タイミング・描画のみを持ち、判定とスコアは [MiniGameRules] に委ねる。
 */
@Composable
fun MiniGameOverlay(
    kind: MiniGameKind,
    seed: Int,
    onFinish: (Int, MiniGameDifficulty) -> Unit,
    onCancel: () -> Unit,
) {
    // 難易度未選択(null)のあいだは選択画面、選択後にゲーム本体を表示する。
    var difficulty by remember(kind, seed) { mutableStateOf<MiniGameDifficulty?>(null) }
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
            val selected = difficulty
            if (selected == null) {
                DifficultySelector(kind) { difficulty = it }
            } else {
                when (kind) {
                    MiniGameKind.CORE_CATCH -> CoreCatchGame(seed, selected, onFinish)
                    MiniGameKind.PULSE_TRAINING -> PulseTrainingGame(selected, onFinish)
                    // TODO(U7): ミールバランスの難易度別チューニング。今は難易度を受け取り結果へ渡すのみ。
                    MiniGameKind.MEAL_BALANCE -> MealBalanceGame(seed, selected, onFinish)
                }
            }
        }
    }
}

/** 難易度選択(全ミニゲーム共通)。ゲームごとの簡単な説明を添える。 */
@Composable
private fun DifficultySelector(kind: MiniGameKind, onSelect: (MiniGameDifficulty) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("難易度をえらぶ", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
        MiniGameDifficulty.entries.forEach { difficulty ->
            val brief = when (kind) {
                MiniGameKind.CORE_CATCH ->
                    "制限時間 ${MiniGameRules.coreCatchTimeLimitMs(difficulty) / 1000}秒"
                MiniGameKind.PULSE_TRAINING ->
                    "速度 ${MiniGameRules.pulseCycleMs(difficulty)}ms・${MiniGameRules.pulseSuccessScore(difficulty)}点でクリア"
                else -> "機嫌+${MiniGameRules.successMood(difficulty)}・絆+${MiniGameRules.successBond(difficulty)}"
            }
            Button(
                modifier = Modifier.fillMaxWidth().height(56.dp),
                onClick = { onSelect(difficulty) },
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(difficulty.label, fontWeight = FontWeight.Bold)
                    Text(brief, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

/**
 * コアキャッチ(U5リニューアル): 5x5に散らばった1〜25を順番にタップする。
 * 次に押す数字のマスは強調されない(それが難しさ)。ミスで+1秒ペナルティ。
 * 実時間+ペナルティが制限時間を超えたら終了。時間内に25まで完走すれば成功。
 */
@Composable
private fun CoreCatchGame(seed: Int, difficulty: MiniGameDifficulty, onFinish: (Int, MiniGameDifficulty) -> Unit) {
    val numbers = remember(seed) { MiniGameRules.coreCatchNumbers(seed) }
    val limitMs = remember(difficulty) { MiniGameRules.coreCatchTimeLimitMs(difficulty) }
    var next by remember { mutableIntStateOf(1) } // 次に押すべき番号(1〜25)
    var mistakes by remember { mutableIntStateOf(0) }
    var penaltyMs by remember { mutableLongStateOf(0L) }
    var elapsedMs by remember { mutableLongStateOf(0L) }
    var wrongCell by remember { mutableIntStateOf(-1) } // 直近に押し間違えたマス(赤フラッシュ用)
    var finished by remember { mutableStateOf(false) }

    // 100msごとに実時間を進め、ペナルティ込みで制限時間を超えたら終了する。
    LaunchedEffect(seed, difficulty) {
        val startNs = System.nanoTime()
        while (!finished) {
            delay(100)
            elapsedMs = (System.nanoTime() - startNs) / 1_000_000
            if (elapsedMs + penaltyMs >= limitMs) {
                finished = true
                // 時間切れ: 到達した番号(next-1)を渡す。
                onFinish(next - 1, difficulty)
            }
        }
    }

    val totalMs = (elapsedMs + penaltyMs).coerceAtMost(limitMs)
    val remainingMs = (limitMs - totalMs).coerceAtLeast(0L)
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("残り ${formatTime(remainingMs)}", fontWeight = FontWeight.Bold, color = if (remainingMs < 5_000) MissRed else Color.White)
            Text("次: ${next.coerceAtMost(MiniGameRules.CORE_CATCH_CELLS)}", fontWeight = FontWeight.Bold, color = AccentGold)
            Text("ミス $mistakes", fontWeight = FontWeight.Bold, color = MissRed)
        }
        Spacer(Modifier.height(8.dp))
        // 残り時間バー
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp)),
        ) {
            val fraction = if (limitMs == 0L) 0f else (remainingMs.toFloat() / limitMs)
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(8.dp)
                    .background(if (remainingMs < 5_000) MissRed else CoreColor, RoundedCornerShape(4.dp)),
            )
        }
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(5) { rowIndex ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    repeat(5) { colIndex ->
                        val cellIndex = rowIndex * 5 + colIndex
                        val value = numbers[cellIndex]
                        val done = value < next
                        val isWrong = cellIndex == wrongCell
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .background(
                                    when {
                                        done -> CoreColor.copy(alpha = 0.18f)
                                        else -> Color.White.copy(alpha = 0.08f)
                                    },
                                    RoundedCornerShape(12.dp),
                                )
                                .then(
                                    if (isWrong) Modifier.border(2.dp, MissRed, RoundedCornerShape(12.dp)) else Modifier,
                                )
                                .clickable(enabled = !done && !finished) {
                                    if (value == next) {
                                        next++
                                        wrongCell = -1
                                        if (next > MiniGameRules.CORE_CATCH_CELLS) {
                                            // 25まで完走。時間内なので成功スコア25を渡す。
                                            finished = true
                                            onFinish(MiniGameRules.CORE_CATCH_CELLS, difficulty)
                                        }
                                    } else {
                                        mistakes++
                                        penaltyMs += MiniGameRules.CORE_CATCH_MISTAKE_PENALTY_MS
                                        wrongCell = cellIndex
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (done) {
                                Text("✓", fontSize = 18.sp, color = CoreColor)
                            } else {
                                Text("$value", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "1から25まで順番にタップ！ミスは+1秒。",
            style = MaterialTheme.typography.bodySmall,
            color = AccentGold,
        )
    }
}

/** 経過時間をm:ss.d(0.1秒)で整形する。 */
private fun formatTime(ms: Long): String {
    val totalTenths = ms / 100
    val minutes = totalTenths / 600
    val seconds = (totalTenths / 10) % 60
    val tenths = totalTenths % 10
    return "%d:%02d.%d".format(minutes, seconds, tenths)
}

@Composable
private fun PulseTrainingGame(difficulty: MiniGameDifficulty, onFinish: (Int, MiniGameDifficulty) -> Unit) {
    var round by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var lastJudge by remember { mutableStateOf<String?>(null) }
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val cycleMs = remember(difficulty) { MiniGameRules.pulseCycleMs(difficulty) }
    val successScore = remember(difficulty) { MiniGameRules.pulseSuccessScore(difficulty) }

    LaunchedEffect(round) {
        if (round >= MiniGameRules.PULSE_ROUNDS) {
            onFinish(score, difficulty)
            return@LaunchedEffect
        }
        progress.snapTo(0f)
        progress.animateTo(1f, tween(cycleMs, easing = LinearEasing))
        // タップされないまま閉じ切ったらミス扱いで次へ。
        lastJudge = "ミス…"
        round++
    }

    fun tap() {
        if (round >= MiniGameRules.PULSE_ROUNDS) return
        val gained = MiniGameRules.pulseJudge(progress.value, difficulty)
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
            "$successScore 点以上でクリア！(1周 ${cycleMs}ms)",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun MealBalanceGame(seed: Int, difficulty: MiniGameDifficulty, onFinish: (Int, MiniGameDifficulty) -> Unit) {
    val questions = remember(seed) { MiniGameRules.mealRound(seed) }
    var index by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var feedback by remember { mutableStateOf<String?>(null) }

    if (index >= questions.size) {
        LaunchedEffect(Unit) { onFinish(score, difficulty) }
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
