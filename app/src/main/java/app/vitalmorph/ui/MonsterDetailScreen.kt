package app.vitalmorph.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vitalmorph.domain.BattleEngine
import app.vitalmorph.domain.BattleMove
import app.vitalmorph.domain.BattleStats
import app.vitalmorph.domain.BattleStatsCalculator
import app.vitalmorph.domain.LegacyStats
import app.vitalmorph.domain.MonsterGeneration
import app.vitalmorph.domain.MonsterSex
import app.vitalmorph.domain.MoodEngine

// VitaMorphApp.kt のブランドカラーはファイルプライベートのため、詳細画面用に同色を定義する。
private val DetailMint = Color(0xFF69E6A6)
private val DetailGold = Color(0xFFFFD166)
private val DetailSurfaceHigh = Color(0xFF1D344A)

// 能力バーの表示上の基準最大値(演出専用。ゲームロジックには影響しない)。
private const val HP_BAR_MAX = 250f
private const val STAT_BAR_MAX = 60f

/**
 * モンスター詳細ステータス画面(U10 / Issue #13)。
 *
 * バトル能力は[BattleStatsCalculator]で算出し、大会開始時と完全に同じ値を表示する。
 * 全画面のスクロール可能なSurfaceとして表示し、「閉じる」で[onClose]を呼ぶ。
 */
@Composable
fun MonsterDetailSheet(
    state: GameUiState,
    onClose: () -> Unit,
) {
    val evolution = state.evolution ?: return
    val form = evolution.form
    val generation = state.generation
    val mood = generation?.mood ?: MonsterGeneration.DEFAULT_MOOD
    val bond = generation?.bond ?: MonsterGeneration.DEFAULT_BOND
    val legacy = state.legacyStats
    val stats = BattleStatsCalculator.statsFor(form, evolution.metrics, mood, bond, legacy)
    val moves = BattleEngine.movesFor(form.family)

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DetailHeader(
                form = form,
                sex = generation?.sex,
                personality = generation?.personality?.label,
                routeLabel = generation?.route?.label,
            )
            StrengthSection(stats = stats, legacy = legacy, mood = mood, bond = bond)
            MovesSection(moves = moves)
            BondSection(mood = mood, bond = bond, legacy = legacy)
            OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onClose) { Text("閉じる") }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DetailHeader(
    form: app.vitalmorph.domain.MonsterForm,
    sex: MonsterSex?,
    personality: String?,
    routeLabel: String?,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = Color(form.accent).copy(alpha = 0.12f)),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(Modifier.size(160.dp)) {
                MonsterVisual(form, Modifier.fillMaxSize())
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(form.name, fontSize = 26.sp, fontWeight = FontWeight.Black)
                sex?.let {
                    Text(
                        it.mark,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = if (it == MonsterSex.MALE) DetailMint else DetailGold,
                    )
                }
            }
            Text("${form.stage.label} ・ ${form.role}", color = DetailGold)
            personality?.let {
                Text("せいかく: $it", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f))
            }
            routeLabel?.let {
                Text("ルート適性: $it", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f))
            }
            Text(
                form.description,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun StrengthSection(stats: BattleStats, legacy: LegacyStats, mood: Int, bond: Int) {
    DetailCard(title = "つよさ") {
        StatBar("HP", stats.maxHp, HP_BAR_MAX, DetailMint, legacy.hpPoints)
        StatBar("こうげき", stats.attack, STAT_BAR_MAX, DetailGold, legacy.attackPoints)
        StatBar("ぼうぎょ", stats.defense, STAT_BAR_MAX, DetailMint, legacy.defensePoints)
        StatBar("すばやさ", stats.speed, STAT_BAR_MAX, DetailGold, legacy.speedPoints)

        Spacer(Modifier.height(6.dp))
        val legacyTotal = legacy.hpPoints + legacy.attackPoints + legacy.defensePoints + legacy.speedPoints
        BreakdownLine(
            "継承ボーナス",
            if (legacyTotal > 0) {
                "HP+${legacy.hpPoints}% こうげき+${legacy.attackPoints}% ぼうぎょ+${legacy.defensePoints}% すばやさ+${legacy.speedPoints}%"
            } else "なし",
        )
        // 機嫌補正(すばやさの増減・開始シールド・開始エネルギー)。
        val moodBits = buildList {
            if (stats.moodSpeedDelta != 0) {
                val sign = if (stats.moodSpeedDelta > 0) "+" else ""
                add("すばやさ$sign${stats.moodSpeedDelta}")
            }
            if (stats.startShield) add("開始シールドあり")
            if (stats.startEnergy < BattleStatsCalculator.MAX_ENERGY) add("開始エネルギー${stats.startEnergy}")
        }
        BreakdownLine("機嫌補正(${MoodEngine.moodBand(mood).label})", if (moodBits.isEmpty()) "なし" else moodBits.joinToString(" / "))
        BreakdownLine(
            "応援",
            if (stats.cheerAvailable) "使用可(絆${BattleEngine.CHEER_BOND_THRESHOLD}以上)"
            else "絆${BattleEngine.CHEER_BOND_THRESHOLD}以上で使用可(現在${bond})",
        )
    }
}

@Composable
private fun StatBar(label: String, value: Int, max: Float, color: Color, legacyPoints: Int) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(
                if (legacyPoints > 0) "$value  (継承+$legacyPoints%)" else "$value",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        LinearProgressIndicator(
            progress = { (value / max).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = color,
        )
    }
}

@Composable
private fun MovesSection(moves: List<BattleMove>) {
    DetailCard(title = "わざ") {
        moves.forEach { move ->
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(move.name, fontWeight = FontWeight.Bold, color = DetailMint)
                Text(move.description, style = MaterialTheme.typography.bodySmall)
                val attrs = buildList {
                    if (move.power > 0) add("威力 ${move.power}")
                    if (move.energyCost > 0) add("消費エネルギー ${move.energyCost}")
                    if (move.priority != 0) add("優先度 ${move.priority}")
                    if (move.heal > 0) add("回復 ${move.heal}")
                    if (move.recoil > 0) add("反動 ${move.recoil}")
                }
                if (attrs.isNotEmpty()) {
                    Text(
                        attrs.joinToString(" ・ "),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

@Composable
private fun BondSection(mood: Int, bond: Int, legacy: LegacyStats) {
    DetailCard(title = "きずな") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(Modifier.weight(1f)) {
                Text("機嫌・${MoodEngine.moodBand(mood).label}", style = MaterialTheme.typography.labelMedium)
                LinearProgressIndicator(
                    progress = { mood / 100f },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    color = DetailMint,
                )
                Text("$mood / 100", style = MaterialTheme.typography.labelSmall)
            }
            Column(Modifier.weight(1f)) {
                Text("絆・${MoodEngine.bondBand(bond).label}", style = MaterialTheme.typography.labelMedium)
                LinearProgressIndicator(
                    progress = { bond / 100f },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    color = DetailGold,
                )
                Text("$bond / 100", style = MaterialTheme.typography.labelSmall)
            }
        }
        Spacer(Modifier.height(6.dp))
        val legacyTotal = legacy.hpPoints + legacy.attackPoints + legacy.defensePoints + legacy.speedPoints
        BreakdownLine("継承ポイント合計", "${legacyTotal}pt(${legacy.totalGenerations}世代分)")
    }
}

@Composable
private fun DetailCard(title: String, content: @Composable () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = DetailSurfaceHigh),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = DetailGold)
            content()
        }
    }
}

@Composable
private fun BreakdownLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f))
        Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}
