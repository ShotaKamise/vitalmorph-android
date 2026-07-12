package app.vitalmorph.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.vitalmorph.domain.BattleEngine
import app.vitalmorph.domain.DailyHealthData
import app.vitalmorph.domain.BattleOutcome
import app.vitalmorph.data.ExternalFood
import app.vitalmorph.domain.DayNutritionChoice
import app.vitalmorph.domain.DialogueChoice
import app.vitalmorph.domain.DialogueLine
import app.vitalmorph.domain.EvolutionEngine
import app.vitalmorph.domain.EvolutionResult
import app.vitalmorph.domain.FoodCatalog
import app.vitalmorph.domain.FoodCatalogItem
import app.vitalmorph.domain.FoodEntry
import app.vitalmorph.domain.LegacyStats
import app.vitalmorph.domain.MealSlot
import app.vitalmorph.domain.MonsterGeneration
import app.vitalmorph.domain.NutritionSource
import app.vitalmorph.domain.MiniGameKind
import app.vitalmorph.domain.MiniGameRules
import app.vitalmorph.domain.MonsterForm
import app.vitalmorph.domain.MonsterSex
import app.vitalmorph.domain.MonsterStage
import app.vitalmorph.domain.MoodEngine
import app.vitalmorph.domain.TouchArea
import app.vitalmorph.domain.TouchReactionType
import app.vitalmorph.domain.TrainerNameRules
import app.vitalmorph.domain.TournamentResult
import app.vitalmorph.domain.TurnBattleState
import app.vitalmorph.domain.TrainerRank
import app.vitalmorph.domain.UserGoals
import app.vitalmorph.domain.WorkoutTag
import kotlinx.coroutines.delay
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
                state.activeMiniGame != null -> MiniGameOverlay(
                    kind = state.activeMiniGame!!,
                    seed = state.miniGameSeed,
                    onFinish = viewModel::finishMiniGame,
                    onCancel = viewModel::cancelMiniGame,
                )
                else -> MainGameScreen(
                    state = state,
                    snackbar = snackbar,
                    onTab = viewModel::selectTab,
                    onRequestHealthPermissions = onRequestHealthPermissions,
                    onRefresh = viewModel::refresh,
                    onWorkoutTag = viewModel::setWorkoutTag,
                    onStartTournament = viewModel::startTournament,
                    onBattleMove = viewModel::useBattleMove,
                    onBattleItem = viewModel::useBattleItem,
                    onNextRound = viewModel::advanceBattleRound,
                    onAdvanceDemo = viewModel::advanceDemoWeek,
                    onCompleteSeason = viewModel::completeSeason,
                    onReset = viewModel::resetAll,
                    onSetTrainerName = viewModel::setTrainerName,
                    onTouch = viewModel::onMonsterTouched,
                    onReactionShown = viewModel::clearTouchReaction,
                    onDialogueChoice = viewModel::onDialogueChoice,
                    onTalkAgain = viewModel::talkAgain,
                    onStartMiniGame = viewModel::startMiniGame,
                    onAddCatalogFood = viewModel::addCatalogFood,
                    onAddManualFood = viewModel::addFood,
                    onDeleteFood = viewModel::deleteFood,
                    onCopyYesterdayMeals = viewModel::copyYesterdayMeals,
                    onToggleFoodFavorite = viewModel::toggleFoodFavorite,
                    onSetNutritionSource = viewModel::setNutritionSource,
                    onSearchExternalFood = viewModel::searchExternalFood,
                    onLookupBarcode = viewModel::lookupBarcode,
                    onAddExternalFood = viewModel::addExternalFood,
                    onClearExternalResults = viewModel::clearExternalResults,
                    onSetTodayNutritionChoice = viewModel::setTodayNutritionChoice,
                    onSaveRecipe = viewModel::saveRecipe,
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
    onStartTournament: () -> Unit,
    onBattleMove: (String) -> Unit,
    onBattleItem: (String) -> Unit,
    onNextRound: () -> Unit,
    onAdvanceDemo: () -> Unit,
    onCompleteSeason: () -> Unit,
    onReset: () -> Unit,
    onSetTrainerName: (String) -> Unit,
    onTouch: (TouchArea) -> Unit,
    onReactionShown: () -> Unit,
    onDialogueChoice: (DialogueChoice) -> Unit,
    onTalkAgain: () -> Unit,
    onStartMiniGame: (MiniGameKind) -> Unit,
    onAddCatalogFood: (MealSlot, FoodCatalogItem, Double) -> Unit,
    onAddManualFood: (MealSlot, String, Double, String, Double, Double, Double, Double, Boolean, Double, Double, Double) -> Unit,
    onDeleteFood: (FoodEntry) -> Unit,
    onCopyYesterdayMeals: () -> Unit,
    onToggleFoodFavorite: (String) -> Unit,
    onSetNutritionSource: (NutritionSource) -> Unit,
    onSearchExternalFood: (String) -> Unit,
    onLookupBarcode: (String) -> Unit,
    onAddExternalFood: (MealSlot, ExternalFood, Double) -> Unit,
    onClearExternalResults: () -> Unit,
    onSetTodayNutritionChoice: (DayNutritionChoice) -> Unit,
    onSaveRecipe: (String, List<Pair<FoodCatalogItem, Double>>) -> Unit,
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
                AppTab.HOME -> HomeScreen(
                    state,
                    onRequestHealthPermissions,
                    onRefresh,
                    onWorkoutTag,
                    onSetTrainerName,
                    onTouch,
                    onReactionShown,
                    onDialogueChoice,
                    onTalkAgain,
                    onStartMiniGame,
                )
                AppTab.MEALS -> MealsScreen(
                    state = state,
                    onAddCatalog = onAddCatalogFood,
                    onAddManual = onAddManualFood,
                    onDelete = onDeleteFood,
                    onCopyYesterday = onCopyYesterdayMeals,
                    onToggleFavorite = onToggleFoodFavorite,
                    onSearchExternal = onSearchExternalFood,
                    onLookupBarcode = onLookupBarcode,
                    onAddExternal = onAddExternalFood,
                    onClearExternal = onClearExternalResults,
                    onSetTodayChoice = onSetTodayNutritionChoice,
                    onSaveRecipe = onSaveRecipe,
                )
                AppTab.EVOLUTION -> EvolutionScreen(state.evolution)
                AppTab.ARENA -> ArenaScreen(state, onStartTournament, onBattleMove, onBattleItem, onNextRound)
                AppTab.TRAINER -> TrainerScreen(state)
                AppTab.SETTINGS -> SettingsScreen(state, onAdvanceDemo, onCompleteSeason, onReset, onSetTrainerName, onSetNutritionSource)
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
    onSetTrainerName: (String) -> Unit,
    onTouch: (TouchArea) -> Unit,
    onReactionShown: () -> Unit,
    onDialogueChoice: (DialogueChoice) -> Unit,
    onTalkAgain: () -> Unit,
    onStartMiniGame: (MiniGameKind) -> Unit,
) {
    val evolution = state.evolution ?: return
    val todayData = state.days.lastOrNull()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            MonsterHero(
                evolution = evolution,
                sex = state.generation?.sex,
                mood = state.generation?.mood,
                bond = state.generation?.bond,
                touchReaction = state.touchReaction,
                onTouch = onTouch,
                onReactionShown = onReactionShown,
            )
        }
        state.dialogue?.let { dialogue ->
            item {
                DialogueCard(
                    monsterName = evolution.form.name,
                    dialogue = dialogue,
                    reply = state.dialogueReply,
                    onChoice = onDialogueChoice,
                    onTalkAgain = onTalkAgain,
                )
            }
        }
        item {
            MiniGameCard(
                rewardsUsedToday = state.interaction.miniGameRewardCountToday,
                onStart = onStartMiniGame,
            )
        }
        if (state.trainerName == null) {
            item {
                ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = SurfaceHigh)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("トレーナー名を設定", fontWeight = FontWeight.Bold)
                        Text("モンスターがあなたを呼ぶ名前です。あとから設定でも変更できます。", style = MaterialTheme.typography.bodySmall)
                        TrainerNameEditor(current = null, buttonLabel = "この名前にする", onSave = onSetTrainerName)
                    }
                }
            }
        }
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
private fun MonsterHero(
    evolution: EvolutionResult,
    sex: MonsterSex? = null,
    mood: Int? = null,
    bond: Int? = null,
    touchReaction: TouchReactionType? = null,
    onTouch: ((TouchArea) -> Unit)? = null,
    onReactionShown: () -> Unit = {},
) {
    // タッチ反応は少し表示してから通常モーションへ戻す。
    LaunchedEffect(touchReaction) {
        if (touchReaction != null && touchReaction != TouchReactionType.NONE) {
            delay(1_400)
            onReactionShown()
        }
    }
    // TOUCH_HAPPY等の専用モーションはCodex制作待ちのため、既存モーションから選択する
    // (docs/MONSTER_ASSET_CONTRACT.md)。
    val motion = when (touchReaction) {
        TouchReactionType.HAPPY -> MonsterMotion.VICTORY
        TouchReactionType.ANNOYED -> MonsterMotion.HIT
        else -> MonsterMotion.IDLE
    }
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = Color(evolution.form.accent).copy(alpha = 0.12f)),
    ) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(evolution.form.stage.label, color = Color(evolution.form.accent), fontWeight = FontWeight.Bold)
            Box(
                modifier = Modifier
                    .size(210.dp)
                    .pointerInput(onTouch != null) {
                        detectTapGestures { offset ->
                            // 上半分を頭、下半分を体としてタッチ部位を判定する。
                            val area = if (offset.y < size.height / 2f) TouchArea.HEAD else TouchArea.BODY
                            onTouch?.invoke(area)
                        }
                    },
            ) {
                MonsterVisual(evolution.form, Modifier.fillMaxSize(), motion = motion)
            }
            val reactionLabel = when (touchReaction) {
                TouchReactionType.HAPPY -> "うれしそうにしている!"
                TouchReactionType.SHY -> "少し照れているみたい…"
                TouchReactionType.ANNOYED -> "ちょっと嫌がっている…そっとしておこう。"
                else -> null
            }
            if (reactionLabel != null) {
                Text(reactionLabel, color = Gold, style = MaterialTheme.typography.labelMedium)
            } else if (onTouch != null) {
                Text("タッチしてふれあおう", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.labelMedium)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(evolution.form.name, fontSize = 28.sp, fontWeight = FontWeight.Black)
                sex?.let {
                    Text(
                        it.mark,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = if (it == MonsterSex.MALE) Mint else Gold,
                    )
                }
            }
            Text(evolution.form.role, color = Gold)
            Text(evolution.form.description, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
            if (mood != null && bond != null) {
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("機嫌・${MoodEngine.moodBand(mood).label}", style = MaterialTheme.typography.labelMedium)
                        LinearProgressIndicator(
                            progress = { mood / 100f },
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            color = Mint,
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text("絆・${MoodEngine.bondBand(bond).label}", style = MaterialTheme.typography.labelMedium)
                        LinearProgressIndicator(
                            progress = { bond / 100f },
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            color = Gold,
                        )
                    }
                }
            }
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
private fun MiniGameCard(
    rewardsUsedToday: Int,
    onStart: (MiniGameKind) -> Unit,
) {
    val remaining = (MiniGameRules.REWARDS_PER_DAY - rewardsUsedToday).coerceAtLeast(0)
    ElevatedCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("ミニゲーム", fontWeight = FontWeight.Bold)
                Text(
                    if (remaining > 0) "きょうのごほうび 残り$remaining 回" else "きょうのごほうびは上限(あそぶのは自由)",
                    style = MaterialTheme.typography.labelMedium,
                    color = Gold,
                )
            }
            MiniGameKind.entries.forEach { kind ->
                OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { onStart(kind) }) {
                    Column(Modifier.fillMaxWidth()) {
                        Text(kind.label, fontWeight = FontWeight.Bold)
                        Text(kind.summary, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogueCard(
    monsterName: String,
    dialogue: DialogueLine,
    reply: String?,
    onChoice: (DialogueChoice) -> Unit,
    onTalkAgain: () -> Unit,
) {
    ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = SurfaceHigh)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(monsterName, color = Mint, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text(reply ?: dialogue.text)
            if (reply == null) {
                dialogue.choices.forEach { choice ->
                    OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { onChoice(choice) }) {
                        Text(choice.text)
                    }
                }
            } else {
                OutlinedButton(onClick = onTalkAgain) { Text("また話す") }
            }
        }
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
            Text("第1週の生活で系統、第2週の傾向で職業、第3週の記録と機嫌・絆で最終形態が決まります。")
        }
        items(evolution.path) { form ->
            ElevatedCard {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    MonsterVisual(form, Modifier.size(64.dp), showAura = false)
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
private fun ArenaScreen(
    state: GameUiState,
    onStartTournament: () -> Unit,
    onBattleMove: (String) -> Unit,
    onBattleItem: (String) -> Unit,
    onNextRound: () -> Unit,
) {
    val currentForm = state.evolution?.form
    val battle = state.battle
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SectionTitle("トレーナーバトル大会")
            Text("技とアイテムを選び、CPUトレーナーとの3試合を勝ち抜こう。アイテムは大会中で共通です。")
        }
        if (currentForm != null && battle == null) {
            item {
                ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = Color(currentForm.accent).copy(alpha = 0.10f))) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        MonsterVisual(currentForm, Modifier.size(190.dp), motion = MonsterMotion.VICTORY)
                        Text(currentForm.name, fontSize = 24.sp, fontWeight = FontWeight.Black)
                        Text("4つの技と3種類のアイテムで戦います", color = Gold)
                        state.generation?.let { generation ->
                            Text(
                                "コンディション: ${MoodEngine.moodBand(generation.mood).label}" +
                                    if (generation.bond >= BattleEngine.CHEER_BOND_THRESHOLD) "・応援スタンバイ!" else "",
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
            }
            item {
                Button(modifier = Modifier.fillMaxWidth().height(52.dp), onClick = onStartTournament) {
                    Text("大会を開始する")
                }
            }
        }
        if (currentForm != null && battle != null) {
            item { TurnBattleStage(currentForm, battle) }
            when (battle.outcome) {
                BattleOutcome.IN_PROGRESS -> item { BattleCommandPanel(battle, onBattleMove, onBattleItem) }
                BattleOutcome.ROUND_WON -> item {
                    Button(modifier = Modifier.fillMaxWidth().height(52.dp), onClick = onNextRound) {
                        Text("HPを35%回復して次の試合へ")
                    }
                }
                BattleOutcome.TOURNAMENT_WON, BattleOutcome.PLAYER_LOST -> {
                    state.tournament?.let { result -> item { TournamentSummary(result, currentForm) } }
                    item {
                        OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onStartTournament) {
                            Text("最初から再挑戦する")
                        }
                    }
                }
            }
            item { BattleLog(battle) }
        }
    }
}

@Composable
private fun TurnBattleStage(form: MonsterForm, battle: TurnBattleState) {
    ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = Color(form.accent).copy(alpha = 0.10f))) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(battle.roundLabel, color = Gold, fontWeight = FontWeight.Black)
                Text("TURN ${battle.turn}", fontWeight = FontWeight.Bold)
            }
            BattleHealthBar(battle.opponentName, battle.opponentHp, battle.opponentMaxHp, Color(0xFFB89CFF))
            Row(
                Modifier.fillMaxWidth().height(160.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MonsterVisual(
                    form = form,
                    modifier = Modifier.weight(1f).height(150.dp),
                    motion = when (battle.outcome) {
                        BattleOutcome.TOURNAMENT_WON, BattleOutcome.ROUND_WON -> MonsterMotion.VICTORY
                        BattleOutcome.PLAYER_LOST -> MonsterMotion.HIT
                        BattleOutcome.IN_PROGRESS -> MonsterMotion.ATTACK
                    },
                )
                Text("VS", color = Gold, fontWeight = FontWeight.Black, fontSize = 20.sp)
                MonsterSprite(
                    drawableRes = MonsterArtwork.resourceForOpponent(battle.opponentName),
                    contentDescription = battle.opponentName,
                    accent = Color(0xFFB89CFF),
                    modifier = Modifier.weight(1f).height(150.dp),
                    motion = when (battle.outcome) {
                        BattleOutcome.PLAYER_LOST -> MonsterMotion.VICTORY
                        else -> MonsterMotion.HIT
                    },
                    facingRight = false,
                )
            }
            BattleHealthBar(battle.playerName, battle.playerHp, battle.playerMaxHp, Color(form.accent))
            Text("コアエネルギー ${"●".repeat(battle.playerEnergy)}${"○".repeat(3 - battle.playerEnergy)}", color = Gold)
        }
    }
}

@Composable
private fun BattleHealthBar(name: String, hp: Int, maxHp: Int, accent: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name, fontWeight = FontWeight.Bold)
            Text("HP $hp / $maxHp", style = MaterialTheme.typography.labelMedium)
        }
        LinearProgressIndicator(
            progress = { hp.toFloat() / maxHp.coerceAtLeast(1) },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = accent,
        )
    }
}

@Composable
private fun BattleCommandPanel(
    battle: TurnBattleState,
    onMove: (String) -> Unit,
    onItem: (String) -> Unit,
) {
    ElevatedCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("技を選ぶ", fontSize = 20.sp, fontWeight = FontWeight.Black)
            battle.moves.forEach { move ->
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = battle.playerEnergy >= move.energyCost,
                    onClick = { onMove(move.id) },
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(move.name, fontWeight = FontWeight.Bold)
                            Text(if (move.energyCost == 0) "消費なし" else "●${move.energyCost}")
                        }
                        Text(
                            if (move.power > 0) "${move.description}・威力 ${move.power}" else move.description,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
            HorizontalDivider()
            Text("アイテムを使う", fontWeight = FontWeight.Bold)
            battle.items.forEach { stock ->
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onItem(stock.item.id) },
                    enabled = stock.remaining > 0,
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        Text("${stock.item.name} ×${stock.remaining}", fontWeight = FontWeight.Bold)
                        Text(stock.item.description, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Text("アイテムを使ってもCPUのターンは進みます。", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun BattleLog(battle: TurnBattleState) {
    ElevatedCard {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("バトルログ", fontWeight = FontWeight.Black)
            battle.log.takeLast(10).forEach { entry ->
                Text(entry, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun TournamentSummary(result: TournamentResult, form: MonsterForm?) {
    ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = Gold.copy(alpha = 0.12f))) {
        Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            form?.let {
                MonsterVisual(
                    form = it,
                    modifier = Modifier.size(126.dp),
                    motion = if (result.placement == 1) MonsterMotion.VICTORY else MonsterMotion.IDLE,
                )
            }
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
        item { LegacyStatsCard(state.legacyStats) }
        if (state.pastGenerations.isNotEmpty()) {
            item {
                SectionTitle("系譜")
                Text("歴代のパートナーと、受け継がれた力の記録。")
            }
            items(state.pastGenerations) { generation ->
                GenerationCard(generation)
            }
        }
    }
}

@Composable
private fun LegacyStatsCard(stats: LegacyStats) {
    ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = Mint.copy(alpha = 0.08f))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("継承された力", fontWeight = FontWeight.Bold, color = Mint)
            if (stats.totalGenerations == 0) {
                Text("まだ継承の記録はありません。シーズンを完了すると、過ごし方に応じて次の世代へ力が受け継がれます。", style = MaterialTheme.typography.bodySmall)
            } else {
                Text(
                    "HP +${stats.hpPoints}%・攻撃 +${stats.attackPoints}%・防御 +${stats.defensePoints}%・素早さ +${stats.speedPoints}%",
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "${stats.totalGenerations}世代分の記録。各能力は+15%が上限で、受け継いだ力が減ることはありません。",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun GenerationCard(generation: MonsterGeneration) {
    val form = generation.finalFormId?.let { id -> EvolutionEngine.allForms.firstOrNull { it.id == id } }
    ElevatedCard {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            if (form != null) {
                MonsterVisual(form, Modifier.size(64.dp), showAura = false)
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("第${generation.generationNumber}世代", fontWeight = FontWeight.Bold)
                    Text(
                        generation.sex.mark,
                        fontWeight = FontWeight.Black,
                        color = if (generation.sex == MonsterSex.MALE) Mint else Gold,
                    )
                }
                Text(form?.name ?: "記録なし", style = MaterialTheme.typography.bodyMedium)
                Text(
                    when (generation.finalPlacement) {
                        1 -> "大会: 優勝"
                        2 -> "大会: 準優勝"
                        4 -> "大会: ベスト4"
                        8 -> "大会: 出場"
                        else -> "大会: 記録なし"
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
                val awarded = buildList {
                    if (generation.awardedHp > 0) add("HP+${generation.awardedHp}")
                    if (generation.awardedAttack > 0) add("攻撃+${generation.awardedAttack}")
                    if (generation.awardedDefense > 0) add("防御+${generation.awardedDefense}")
                    if (generation.awardedSpeed > 0) add("素早さ+${generation.awardedSpeed}")
                }
                Text(
                    if (awarded.isEmpty()) "継承: なし" else "継承: ${awarded.joinToString("・")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Gold,
                )
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
    onSetTrainerName: (String) -> Unit,
    onSetNutritionSource: (NutritionSource) -> Unit,
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
                    Text("トレーナー名", fontWeight = FontWeight.Bold)
                    Text(
                        state.trainerName?.let { "現在の名前: $it" } ?: "まだ設定されていません",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    TrainerNameEditor(
                        current = state.trainerName,
                        buttonLabel = if (state.trainerName == null) "設定する" else "変更する",
                        onSave = onSetTrainerName,
                    )
                }
            }
        }
        item {
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
                    Text("栄養データの優先元", fontWeight = FontWeight.Bold)
                    Text(
                        "あすけん等の記録とVitaMorphの記録がある日に、どちらを育成へ使うかを選びます。合算はしません。",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    NutritionSource.entries.forEach { source ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = state.nutritionSource == source,
                                onClick = { onSetNutritionSource(source) },
                            )
                            Column {
                                Text(source.label)
                                Text(source.description, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
        item {
            ElevatedCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("プライバシー", fontWeight = FontWeight.Bold)
                    Text("健康データは端末内でのみ処理し、外部へ送信しません。")
                    Text(
                        "通信が発生するのは食品のネット検索(Open Food Facts)だけで、送信されるのは検索キーワードまたはバーコード番号のみです。バーコードの読み取り自体は端末内で処理されます。",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "VitaMorphで記録した食事は、許可があればHealth Connectにも保存されます(このアプリのData Origin付き)。",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(FoodCatalog.ATTRIBUTION, style = MaterialTheme.typography.bodySmall)
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
private fun TrainerNameEditor(
    current: String?,
    buttonLabel: String,
    onSave: (String) -> Unit,
) {
    var input by remember(current) { mutableStateOf(current ?: "") }
    val error = if (input.isEmpty()) null else TrainerNameRules.validate(input)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("トレーナー名 (1〜${TrainerNameRules.MAX_LENGTH}文字)") },
            singleLine = true,
            isError = error != null,
            supportingText = { error?.let { Text(it) } },
        )
        Button(
            onClick = { onSave(input) },
            enabled = input.isNotBlank() && error == null && TrainerNameRules.normalize(input) != current,
        ) { Text(buttonLabel) }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontSize = 22.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(vertical = 4.dp))
}
