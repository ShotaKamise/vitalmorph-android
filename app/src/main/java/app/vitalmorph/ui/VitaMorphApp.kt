package app.vitalmorph.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.vitalmorph.domain.DailyHealthData
import app.vitalmorph.domain.EvolutionResult
import app.vitalmorph.domain.MonsterFamily
import app.vitalmorph.domain.MonsterForm
import app.vitalmorph.domain.MonsterStage
import app.vitalmorph.domain.TournamentResult
import app.vitalmorph.domain.TrainerRank
import app.vitalmorph.domain.UserGoals
import app.vitalmorph.domain.WorkoutTag
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val Background = Color(0xFF0B1522)
private val SurfaceColor = Color(0xFF132437)
private val SurfaceHigh = Color(0xFF1D344A)
private val Mint = Color(0xFF69E6A6)
private val Gold = Color(0xFFFFD166)

private val VitaColors = darkColorScheme(
    primary = Mint,
    secondary = Gold,
    background = Background,
    surface = SurfaceColor,
    surfaceVariant = SurfaceHigh,
    onPrimary = Color(0xFF062216),
    onBackground = Color(0xFFF1F7FA),
    onSurface = Color(0xFFF1F7FA),
)

@Composable
fun VitaMorphTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = VitaColors, content = content)
}

@Composable
fun VitaMorphApp(
    viewModel: GameViewModel,
    onRequestHealthPermissions: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    VitaMorphTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when {
                state.loading && !state.onboardingComplete -> LoadingScreen()
                !state.onboardingComplete -> OnboardingScreen(onFinish = viewModel::finishOnboarding)
                else -> MainGameScreen(
                    state = state,
                    snackbar = snackbar,
                    onTab = viewModel::selectTab,
                    onRequestHealthPermissions = onRequestHealthPermissions,
                    onRefresh = viewModel::refresh,
                    onWorkoutTag = viewModel::setWorkoutTag,
                    onTournament = viewModel::runTournament,
                    onAdvanceDemo = viewModel::advanceDemoWeek,
                    onCompleteSeason = viewModel::completeSeason,
                    onReset = viewModel::resetAll,
                )
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("生命卵を準備中…", color = Mint)
    }
}

@Composable
private fun OnboardingScreen(onFinish: (UserGoals, Boolean) -> Unit) {
    var calories by remember { mutableStateOf("2000") }
    var protein by remember { mutableStateOf("80") }
    var carbs by remember { mutableStateOf("250") }
    var fat by remember { mutableStateOf("60") }
    var steps by remember { mutableStateOf("8000") }
    var exercise by remember { mutableStateOf("150") }
    var demoMode by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Spacer(Modifier.height(16.dp)) }
        item {
            Text("VitaMorph", fontSize = 36.sp, fontWeight = FontWeight.Black, color = Mint)
            Text("28日間の生活から、あなただけのモンスターが生まれます。", color = Color.White.copy(alpha = 0.75f))
        }
        item { SectionTitle("あすけんの目標値") }
        item {
            Text("あすけんに表示されている目標に合わせてください。これは医療上の目標を新しく決める画面ではありません。", style = MaterialTheme.typography.bodySmall)
        }
        item {
            GoalField("1日の摂取カロリー", calories, "kcal") { calories = it }
            GoalField("たんぱく質", protein, "g") { protein = it }
            GoalField("炭水化物", carbs, "g") { carbs = it }
            GoalField("脂質", fat, "g") { fat = it }
        }
        item { SectionTitle("活動目標") }
        item {
            GoalField("1日の歩数", steps, "歩") { steps = it }
            GoalField("1週間の運動時間", exercise, "分") { exercise = it }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = demoMode, onCheckedChange = { demoMode = it })
                Column {
                    Text("デモモードで開始")
                    Text("実データを使わず、28日サイクルをすぐ確認できます。", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Button(
                modifier = Modifier.fillMaxWidth().height(52.dp),
                onClick = {
                    onFinish(
                        UserGoals(
                            calories = calories.toIntOrNull()?.coerceAtLeast(1) ?: 2_000,
                            proteinGrams = protein.toIntOrNull()?.coerceAtLeast(1) ?: 80,
                            carbsGrams = carbs.toIntOrNull()?.coerceAtLeast(1) ?: 250,
                            fatGrams = fat.toIntOrNull()?.coerceAtLeast(1) ?: 60,
                            dailySteps = steps.toIntOrNull()?.coerceAtLeast(1) ?: 8_000,
                            weeklyExerciseMinutes = exercise.toIntOrNull()?.coerceAtLeast(1) ?: 150,
                        ),
                        demoMode,
                    )
                },
            ) { Text("生命卵を受け取る") }
        }
        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
private fun GoalField(label: String, value: String, suffix: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        value = value,
        onValueChange = { onChange(it.filter(Char::isDigit).take(5)) },
        label = { Text(label) },
        suffix = { Text(suffix) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainGameScreen(
    state: GameUiState,
    snackbar: SnackbarHostState,
    onTab: (AppTab) -> Unit,
    onRequestHealthPermissions: () -> Unit,
    onRefresh: () -> Unit,
    onWorkoutTag: (WorkoutTag) -> Unit,
    onTournament: () -> Unit,
    onAdvanceDemo: () -> Unit,
    onCompleteSeason: () -> Unit,
    onReset: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.navigationBarsPadding(),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("VitaMorph", fontWeight = FontWeight.Bold)
                        Text("28日シーズン・${state.evolution?.seasonDay ?: 1}日目", style = MaterialTheme.typography.labelSmall)
                    }
                },
                actions = {
                    Text(
                        if (state.demoMode) "DEMO" else "LIVE",
                        modifier = Modifier.padding(end = 16.dp).background(
                            if (state.demoMode) Gold.copy(alpha = 0.2f) else Mint.copy(alpha = 0.2f),
                            CircleShape,
                        ).padding(horizontal = 10.dp, vertical = 5.dp),
                        color = if (state.demoMode) Gold else Mint,
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
            )
        },
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = state.selectedTab == tab,
                        onClick = { onTab(tab) },
                        icon = { Text(tab.symbol, fontSize = 18.sp) },
                        label = { Text(tab.label, fontSize = 10.sp) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (state.selectedTab) {
                AppTab.HOME -> HomeScreen(state, onRequestHealthPermissions, onRefresh, onWorkoutTag)
                AppTab.EVOLUTION -> EvolutionScreen(state.evolution)
                AppTab.ARENA -> ArenaScreen(state, onTournament)
                AppTab.TRAINER -> TrainerScreen(state)
                AppTab.SETTINGS -> SettingsScreen(state, onAdvanceDemo, onCompleteSeason, onReset)
            }
        }
    }
}

@Composable
private fun HomeScreen(
    state: GameUiState,
    onRequestPermissions: () -> Unit,
    onRefresh: () -> Unit,
    onWorkoutTag: (WorkoutTag) -> Unit,
) {
    val evolution = state.evolution ?: return
    val todayData = state.days.lastOrNull()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { MonsterHero(evolution) }
        if (!state.demoMode && !state.permissionsGranted) {
            item {
                ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = SurfaceHigh)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("ヘルスコネクトを接続", fontWeight = FontWeight.Bold)
                        Text(
                            if (state.healthConnectAvailable) "歩数・運動・あすけんの栄養データを、端末内で育成へ反映します。"
                            else "この端末でヘルスコネクトを利用できません。Android 9以上とGoogle Play開発者サービスを確認してください。",
                        )
                        Button(onClick = onRequestPermissions, enabled = state.healthConnectAvailable) { Text("データアクセスを許可") }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("歩数", "${todayData?.steps ?: 0}", "目標 ${state.goals.dailySteps}", Modifier.weight(1f))
                MetricCard("運動", "${todayData?.exerciseMinutes ?: 0}分", "活動 ${(todayData?.activeCalories ?: 0.0).roundToInt()}kcal", Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("摂取", "${(todayData?.calories ?: 0.0).roundToInt()}kcal", "目標 ${state.goals.calories}", Modifier.weight(1f))
                MetricCard("記録", "${evolution.metrics.recordedDays}日", "同期済み", Modifier.weight(1f))
            }
        }
        item {
            ElevatedCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("今日の運動タイプ", fontWeight = FontWeight.Bold)
                    Text("Zeppから種別が届かない場合だけ補足してください。", style = MaterialTheme.typography.bodySmall)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WorkoutTag.entries.filter { it != WorkoutTag.NONE }.forEach { tag ->
                            AssistChip(onClick = { onWorkoutTag(tag) }, label = {
                                Text(when (tag) {
                                    WorkoutTag.CARDIO -> "有酸素"
                                    WorkoutTag.STRENGTH -> "筋トレ"
                                    WorkoutTag.OTHER -> "その他"
                                    WorkoutTag.NONE -> "なし"
                                })
                            })
                        }
                    }
                }
            }
        }
        item { OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onRefresh) { Text("最新データに同期") } }
    }
}

@Composable
private fun MonsterHero(evolution: EvolutionResult) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = Color(evolution.form.accent).copy(alpha = 0.12f)),
    ) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(evolution.form.stage.label, color = Color(evolution.form.accent), fontWeight = FontWeight.Bold)
            MonsterVisual(evolution.form, Modifier.size(210.dp))
            Text(evolution.form.name, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text(evolution.form.role, color = Gold)
            Text(evolution.form.description, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
            Spacer(Modifier.height(16.dp))
            val progress = ((evolution.seasonDay - 1) % 7 + 1) / 7f
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            Text(
                evolution.nextEvolutionDay?.let { "次の進化まで ${(it - evolution.seasonDay).coerceAtLeast(0)}日" } ?: "最終形態・大会へ向けて覚醒中",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun MonsterVisual(form: MonsterForm, modifier: Modifier = Modifier) {
    val accent = Color(form.accent)
    Canvas(modifier = modifier.aspectRatio(1f)) {
        val center = Offset(size.width / 2, size.height * 0.54f)
        val radius = size.minDimension * if (form.stage == MonsterStage.BABY) 0.30f else 0.34f
        drawCircle(accent.copy(alpha = 0.15f), radius * 1.35f, center)
        drawOval(accent, topLeft = Offset(center.x - radius, center.y - radius * 0.88f), size = Size(radius * 2, radius * 1.8f))
        val path = Path()
        when (form.family) {
            MonsterFamily.POWER -> {
                path.moveTo(center.x - radius * 0.75f, center.y - radius * 0.55f)
                path.lineTo(center.x - radius * 1.05f, center.y - radius * 1.15f)
                path.lineTo(center.x - radius * 0.30f, center.y - radius * 0.80f)
                path.moveTo(center.x + radius * 0.75f, center.y - radius * 0.55f)
                path.lineTo(center.x + radius * 1.05f, center.y - radius * 1.15f)
                path.lineTo(center.x + radius * 0.30f, center.y - radius * 0.80f)
            }
            MonsterFamily.SPEED, MonsterFamily.OVERDRIVE -> {
                path.moveTo(center.x - radius * 0.45f, center.y - radius * 0.75f)
                path.lineTo(center.x - radius * 0.85f, center.y - radius * 1.40f)
                path.lineTo(center.x - radius * 0.05f, center.y - radius * 0.85f)
                path.moveTo(center.x + radius * 0.45f, center.y - radius * 0.75f)
                path.lineTo(center.x + radius * 0.85f, center.y - radius * 1.40f)
                path.lineTo(center.x + radius * 0.05f, center.y - radius * 0.85f)
            }
            else -> {
                path.moveTo(center.x - radius * 0.55f, center.y - radius * 0.70f)
                path.quadraticTo(center.x - radius, center.y - radius * 1.2f, center.x - radius * 0.85f, center.y - radius * 0.30f)
                path.moveTo(center.x + radius * 0.55f, center.y - radius * 0.70f)
                path.quadraticTo(center.x + radius, center.y - radius * 1.2f, center.x + radius * 0.85f, center.y - radius * 0.30f)
            }
        }
        drawPath(path, accent, style = Stroke(width = radius * 0.22f))
        drawCircle(Color(0xFF07111D), radius * 0.12f, Offset(center.x - radius * 0.35f, center.y - radius * 0.15f))
        drawCircle(Color(0xFF07111D), radius * 0.12f, Offset(center.x + radius * 0.35f, center.y - radius * 0.15f))
        drawCircle(Color.White, radius * 0.035f, Offset(center.x - radius * 0.38f, center.y - radius * 0.18f))
        drawCircle(Color.White, radius * 0.035f, Offset(center.x + radius * 0.32f, center.y - radius * 0.18f))
        drawArc(Color(0xFF07111D), 20f, 140f, false, Offset(center.x - radius * 0.28f, center.y), Size(radius * 0.56f, radius * 0.35f), style = Stroke(radius * 0.06f))
    }
}

@Composable
private fun MetricCard(title: String, value: String, subtitle: String, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.65f))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun EvolutionScreen(evolution: EvolutionResult?) {
    if (evolution == null) return
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SectionTitle("現在の進化経路")
            Text("直近7日を70%、それ以前を30%として最終進化を判定します。")
        }
        items(evolution.path) { form ->
            ElevatedCard {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).background(Color(form.accent), CircleShape))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(form.stage.label, style = MaterialTheme.typography.labelMedium)
                        Text(form.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(form.role, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        if (evolution.finalCandidates.isNotEmpty() && evolution.form.stage == MonsterStage.INTERMEDIATE) {
            item { SectionTitle("最終進化候補") }
            items(evolution.finalCandidates) { form ->
                CandidateCard(form, if (form == evolution.finalCandidates.first()) "条件が近い" else "まだ変化可能")
            }
        }
        item {
            SectionTitle("今週の傾向")
            ScoreBar("栄養調和", evolution.metrics.nutritionScore)
            ScoreBar("活動エネルギー", evolution.metrics.activityScore.coerceAtMost(100))
            ScoreBar("継続性", evolution.metrics.consistencyScore)
        }
    }
}

@Composable
private fun CandidateCard(form: MonsterForm, hint: String) {
    ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = Color(form.accent).copy(alpha = 0.10f))) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            MonsterVisual(form, Modifier.size(72.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(form.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(form.role, color = Color(form.accent))
                Text(form.description, style = MaterialTheme.typography.bodySmall)
            }
            Text(hint, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun ScoreBar(label: String, score: Int) {
    Column(Modifier.padding(vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label)
            Text("$score")
        }
        LinearProgressIndicator(progress = { score.coerceIn(0, 100) / 100f }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun ArenaScreen(state: GameUiState, onTournament: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SectionTitle("CPUオートトーナメント")
            Text("8体参加。準々決勝から決勝まで自動で戦い、結果をダイジェスト表示します。")
        }
        item {
            Button(modifier = Modifier.fillMaxWidth().height(52.dp), onClick = onTournament) {
                Text(if (state.tournament == null) "大会に参加する" else "再戦する")
            }
        }
        state.tournament?.let { result ->
            item { TournamentSummary(result) }
            items(result.matches) { match ->
                ElevatedCard {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(match.round, fontWeight = FontWeight.Bold)
                            Text(if (match.won) "WIN" else "LOSE", color = if (match.won) Mint else Color(0xFFFF7777))
                        }
                        Text("VS ${match.opponent}")
                        Text("${match.playerScore} - ${match.opponentScore}", fontSize = 24.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun TournamentSummary(result: TournamentResult) {
    ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = Gold.copy(alpha = 0.12f))) {
        Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (result.placement == 1) "CHAMPION" else "TOP ${result.placement}", color = Gold, fontWeight = FontWeight.Black)
            Text("大会ポイント +${result.tournamentPoints}", fontSize = 20.sp)
        }
    }
}

@Composable
private fun TrainerScreen(state: GameUiState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { RankCard(state.trainerRank, state.trainerProgress.xp, state.trainerProgress.completedSeasons) }
        item {
            SectionTitle("マスターへの道")
            Text("28日 × 13シーズン＝364日。マスターは経験値1,100以上かつ13シーズン完了が必要です。")
        }
        item {
            ElevatedCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("今シーズン", fontWeight = FontWeight.Bold)
                    Text("大会ポイント ${state.tournamentPoints} / 10")
                    Text("健康データの継続・栄養・活動がシーズン経験値の90%を占めます。")
                }
            }
        }
    }
}

@Composable
private fun RankCard(rank: TrainerRank, xp: Int, seasons: Int) {
    ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = Gold.copy(alpha = 0.12f))) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("TRAINER RANK", style = MaterialTheme.typography.labelLarge, color = Gold)
            Text(rank.name, fontSize = 34.sp, fontWeight = FontWeight.Black)
            Text("$xp XP・$seasons / 13シーズン")
            rank.nextRankXp?.let {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(progress = { (xp.toFloat() / it).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                Text("次のランクまで ${(it - xp).coerceAtLeast(0)} XP", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    state: GameUiState,
    onAdvanceDemo: () -> Unit,
    onCompleteSeason: () -> Unit,
    onReset: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SectionTitle("設定")
            ElevatedCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("目標", fontWeight = FontWeight.Bold)
                    Text("${state.goals.calories} kcal・P ${state.goals.proteinGrams}g / F ${state.goals.fatGrams}g / C ${state.goals.carbsGrams}g")
                    Text("${state.goals.dailySteps}歩・週${state.goals.weeklyExerciseMinutes}分")
                }
            }
        }
        item {
            ElevatedCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("プライバシー", fontWeight = FontWeight.Bold)
                    Text("健康データは端末内でのみ処理します。ゲームはネットワーク権限を要求しません。")
                }
            }
        }
        if (state.demoMode) {
            item { Button(modifier = Modifier.fillMaxWidth(), onClick = onAdvanceDemo) { Text("デモを7日進める") } }
        }
        item {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onCompleteSeason,
                enabled = (state.evolution?.seasonDay ?: 0) >= 28,
            ) { Text("シーズンを完了して次の卵へ") }
        }
        item { HorizontalDivider() }
        item { OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onReset) { Text("すべてのゲームデータを初期化") } }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontSize = 22.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(vertical = 4.dp))
}
