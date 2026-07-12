package app.vitalmorph.ui

import android.app.Application
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.vitalmorph.data.DemoDataFactory
import app.vitalmorph.data.ExternalFood
import app.vitalmorph.data.FoodRepository
import app.vitalmorph.data.GameStore
import app.vitalmorph.data.HealthConnectRepository
import app.vitalmorph.data.OpenFoodFactsRepository
import app.vitalmorph.data.ProfileRepository
import app.vitalmorph.data.db.VitaMorphDatabase
import app.vitalmorph.domain.BattleEngine
import app.vitalmorph.domain.DailyHealthData
import app.vitalmorph.domain.DialogueChoice
import app.vitalmorph.domain.DialogueContext
import app.vitalmorph.domain.DialogueEngine
import app.vitalmorph.domain.DialogueLine
import app.vitalmorph.domain.DayNutritionChoice
import app.vitalmorph.domain.EvolutionEngine
import app.vitalmorph.domain.EvolutionResult
import app.vitalmorph.domain.EvolutionRoute
import app.vitalmorph.domain.FoodCatalogItem
import app.vitalmorph.domain.FoodEntry
import app.vitalmorph.domain.InteractionEngine
import app.vitalmorph.domain.InteractionState
import app.vitalmorph.domain.LegacyAwardEngine
import app.vitalmorph.domain.LegacyStats
import app.vitalmorph.domain.MiniGameKind
import app.vitalmorph.domain.MiniGameRules
import app.vitalmorph.domain.MealSlot
import app.vitalmorph.domain.MonsterGeneration
import app.vitalmorph.domain.MoodEngine
import app.vitalmorph.domain.NutritionResolver
import app.vitalmorph.domain.NutritionSource
import app.vitalmorph.domain.SexAssigner
import app.vitalmorph.domain.TimeOfDay
import app.vitalmorph.domain.TouchArea
import app.vitalmorph.domain.TouchReactionType
import app.vitalmorph.domain.TournamentResult
import app.vitalmorph.domain.TrainerNameRules
import app.vitalmorph.domain.TrainerProgress
import app.vitalmorph.domain.TrainerRank
import app.vitalmorph.domain.TrainerRankEngine
import app.vitalmorph.domain.TurnBattleState
import app.vitalmorph.domain.UserGoals
import app.vitalmorph.domain.WorkoutTag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

enum class AppTab(val label: String, val symbol: String) {
    HOME("育成", "●"),
    MEALS("食事", "🍙"),
    EVOLUTION("進化", "◇"),
    ARENA("大会", "⚔"),
    TRAINER("トレーナー", "★"),
    SETTINGS("設定", "≡"),
}

data class GameUiState(
    val loading: Boolean = true,
    val onboardingComplete: Boolean = false,
    val goals: UserGoals = UserGoals(),
    val seasonStart: LocalDate = LocalDate.now(),
    val today: LocalDate = LocalDate.now(),
    val demoMode: Boolean = false,
    val healthConnectAvailable: Boolean = false,
    val permissionsGranted: Boolean = false,
    val days: List<DailyHealthData> = emptyList(),
    val evolution: EvolutionResult? = null,
    val trainerProgress: TrainerProgress = TrainerProgress(),
    val trainerRank: TrainerRank = TrainerRankEngine.rankFor(TrainerProgress()),
    val tournament: TournamentResult? = null,
    val battle: TurnBattleState? = null,
    val tournamentPoints: Int = 0,
    val trainerName: String? = null,
    val generation: MonsterGeneration? = null,
    val pastGenerations: List<MonsterGeneration> = emptyList(),
    val legacyStats: LegacyStats = LegacyStats(),
    val interaction: InteractionState = InteractionState(),
    val dialogue: DialogueLine? = null,
    val dialogueReply: String? = null,
    val touchReaction: TouchReactionType? = null,
    val miniGameCelebration: Boolean = false,
    val activeMiniGame: MiniGameKind? = null,
    val miniGameSeed: Int = 0,
    val foodEntriesToday: List<FoodEntry> = emptyList(),
    val customFoods: List<FoodCatalogItem> = emptyList(),
    val favoriteFoodIds: Set<String> = emptySet(),
    val recentFoods: List<FoodEntry> = emptyList(),
    val nutritionSource: NutritionSource = NutritionSource.VITALMORPH_FIRST,
    val todayNutritionChoice: DayNutritionChoice? = null,
    val externalResults: List<ExternalFood> = emptyList(),
    val externalSearching: Boolean = false,
    val selectedTab: AppTab = AppTab.HOME,
    val message: String? = null,
)

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val store = GameStore(application)
    private val health = HealthConnectRepository(application)
    private val profiles = ProfileRepository(VitaMorphDatabase.getInstance(application))
    private val foods = FoodRepository(VitaMorphDatabase.getInstance(application))
    private val openFoodFacts = OpenFoodFactsRepository()
    private val mutableState = MutableStateFlow(GameUiState())
    val state: StateFlow<GameUiState> = mutableState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            mutableState.update { it.copy(loading = true, message = null) }
            val stored = store.load()
            val today = if (stored.demoMode) LocalDate.now().plusDays(stored.demoDayOffset.toLong()) else LocalDate.now()
            val available = health.sdkStatus == HealthConnectClient.SDK_AVAILABLE
            val granted = available && runCatching { health.hasAllPermissions() }.getOrDefault(false)
            val seasonEnd = minOf(today, stored.seasonStart.plusDays(27))
            val baseDays = when {
                !stored.onboardingComplete -> emptyList()
                stored.demoMode -> DemoDataFactory.create(stored.seasonStart, today, stored.goals)
                granted -> runCatching {
                    health.readDays(stored.seasonStart, seasonEnd, store.workoutTags())
                }.getOrElse { error ->
                    mutableState.update { it.copy(message = "同期できませんでした: ${error.message ?: "不明なエラー"}") }
                    emptyList()
                }
                else -> emptyList()
            }
            // アプリ内の食事記録を優先元ルール(あすけん優先/VitaMorph優先)で重ねる。
            // デモモードはデモデータをそのまま使う。
            val rawDays = if (stored.onboardingComplete && !stored.demoMode) {
                val entriesByDate = runCatching {
                    foods.entriesBetween(stored.seasonStart, seasonEnd).groupBy { it.date }
                }.getOrDefault(emptyMap())
                NutritionResolver.mergeDays(
                    baseDays,
                    entriesByDate,
                    stored.nutritionSource,
                    stored.seasonStart,
                    seasonEnd,
                    stored.nutritionDayChoices,
                )
            } else {
                baseDays
            }
            // 性別・機嫌・絆が進化ルートへ影響するため、世代を先に確定してから進化を評価する。
            var generation = if (stored.onboardingComplete) {
                runCatching { profiles.ensureCurrentGeneration(stored) }.getOrNull()
            } else null
            var interaction = runCatching { profiles.interactionState() }.getOrDefault(InteractionState())

            // 日付が変わっていたら、前日の記録・交流に応じた機嫌の日次変化を1回だけ適用する。
            val lastReset = interaction.lastDailyResetDate
            if (generation != null && lastReset != null && today.isAfter(lastReset)) {
                val yesterday = today.minusDays(1)
                val recordedYesterday = rawDays.any { it.date == yesterday && (it.hasNutrition || it.hasActivity) }
                val interactedYesterday = interaction.lastInteractionAt > 0 &&
                    Instant.ofEpochMilli(interaction.lastInteractionAt)
                        .atZone(ZoneId.systemDefault()).toLocalDate() == yesterday
                val delta = MoodEngine.dailyMoodDelta(recordedYesterday, interactedYesterday)
                if (delta != 0) {
                    generation = MoodEngine.applyDelta(generation, delta, 0)
                    runCatching { profiles.updateMoodBond(generation) }
                }
            }
            val freshInteraction = InteractionEngine.resetIfNewDay(interaction, today)
            if (freshInteraction != interaction) {
                interaction = freshInteraction
                runCatching { profiles.saveInteractionState(interaction) }
            }

            val evolution = if (stored.onboardingComplete) {
                EvolutionEngine.evaluate(
                    rawDays,
                    stored.goals,
                    stored.seasonStart,
                    today,
                    sex = generation?.sex ?: SexAssigner.deterministicFor(stored.seasonStart),
                    mood = generation?.mood ?: MonsterGeneration.DEFAULT_MOOD,
                    bond = generation?.bond ?: MonsterGeneration.DEFAULT_BOND,
                    route = generation?.route ?: EvolutionRoute.HUMANOID,
                )
            } else null
            val trainerName = runCatching { profiles.trainerProfile()?.name }.getOrNull()
            val legacyStats = runCatching { profiles.legacyStats() }.getOrDefault(LegacyStats())
            val pastGenerations = runCatching {
                profiles.allGenerations().filter { it.seasonEnd != null }.sortedByDescending { it.generationNumber }
            }.getOrDefault(emptyList())
            val foodEntriesToday = runCatching { foods.entriesOn(today) }.getOrDefault(emptyList())
            val customFoods = runCatching { foods.customFoods() }.getOrDefault(emptyList())
            val favoriteFoodIds = runCatching { foods.favoriteIds() }.getOrDefault(emptySet())
            val recentFoods = runCatching { foods.recentFoods() }.getOrDefault(emptyList())
            val dialogue = dialogueFor(trainerName, evolution, generation, rawDays, stored.goals, today)
            mutableState.update {
                it.copy(
                    loading = false,
                    onboardingComplete = stored.onboardingComplete,
                    goals = stored.goals,
                    seasonStart = stored.seasonStart,
                    today = today,
                    demoMode = stored.demoMode,
                    healthConnectAvailable = available,
                    permissionsGranted = granted,
                    days = rawDays,
                    evolution = evolution,
                    trainerProgress = stored.trainerProgress,
                    trainerRank = TrainerRankEngine.rankFor(stored.trainerProgress),
                    tournamentPoints = stored.tournamentPoints,
                    trainerName = trainerName,
                    generation = generation,
                    pastGenerations = pastGenerations,
                    legacyStats = legacyStats,
                    interaction = interaction,
                    dialogue = dialogue,
                    dialogueReply = null,
                    touchReaction = null,
                    miniGameCelebration = false,
                    foodEntriesToday = foodEntriesToday,
                    customFoods = customFoods,
                    favoriteFoodIds = favoriteFoodIds,
                    recentFoods = recentFoods,
                    nutritionSource = stored.nutritionSource,
                    todayNutritionChoice = stored.nutritionDayChoices[today],
                )
            }
        }
    }

    fun finishOnboarding(goals: UserGoals, demoMode: Boolean) {
        store.completeOnboarding(goals, demoMode)
        refresh()
    }

    fun onPermissionResult(granted: Set<String>) {
        mutableState.update { it.copy(permissionsGranted = granted.containsAll(HealthConnectRepository.permissions)) }
        refresh()
    }

    fun selectTab(tab: AppTab) {
        mutableState.update { it.copy(selectedTab = tab) }
    }

    fun setTrainerName(rawName: String) {
        val error = TrainerNameRules.validate(rawName)
        if (error != null) {
            mutableState.update { it.copy(message = error) }
            return
        }
        viewModelScope.launch {
            val profile = runCatching { profiles.setTrainerName(rawName) }.getOrNull()
            if (profile == null) {
                mutableState.update { it.copy(message = "トレーナー名を保存できませんでした") }
            } else {
                mutableState.update { it.copy(trainerName = profile.name, message = "トレーナー名を保存しました") }
            }
        }
    }

    fun setWorkoutTag(tag: WorkoutTag) {
        store.setTodayWorkoutTag(mutableState.value.today, tag)
        refresh()
    }

    /** モンスターをタッチしたとき。部位判定はUI側、反応と報酬はInteractionEngineが決める。 */
    fun onMonsterTouched(area: TouchArea) {
        val current = mutableState.value
        val generation = current.generation ?: return
        val result = InteractionEngine.onTouch(
            state = current.interaction,
            now = System.currentTimeMillis(),
            today = current.today,
            area = area,
            mood = generation.mood,
        )
        if (result.reaction == TouchReactionType.NONE) return
        val updated = MoodEngine.applyDelta(generation, result.moodDelta, result.bondDelta)
        viewModelScope.launch {
            runCatching {
                profiles.saveInteractionState(result.state)
                if (updated != generation) profiles.updateMoodBond(updated)
            }
        }
        mutableState.update {
            it.copy(interaction = result.state, generation = updated, touchReaction = result.reaction)
        }
    }

    /** タッチ反応の表示が終わったらモーションを通常へ戻す。 */
    fun clearTouchReaction() {
        mutableState.update { it.copy(touchReaction = null) }
    }

    /** 会話の選択肢を選んだとき。1日の上限内なら機嫌・絆へ反映する。 */
    fun onDialogueChoice(choice: DialogueChoice) {
        val current = mutableState.value
        val generation = current.generation ?: return
        val result = InteractionEngine.onConversation(
            state = current.interaction,
            now = System.currentTimeMillis(),
            today = current.today,
        )
        val updated = if (result.rewarded) {
            MoodEngine.applyDelta(generation, choice.moodDelta, choice.bondDelta)
        } else generation
        viewModelScope.launch {
            runCatching {
                profiles.saveInteractionState(result.state)
                if (updated != generation) profiles.updateMoodBond(updated)
            }
        }
        mutableState.update {
            it.copy(interaction = result.state, generation = updated, dialogueReply = choice.reply)
        }
    }

    /** 新しい話題で会話を生成し直す。 */
    fun talkAgain() {
        dialogueSeedBump += 1
        val current = mutableState.value
        mutableState.update {
            it.copy(
                dialogue = dialogueFor(
                    current.trainerName,
                    current.evolution,
                    current.generation,
                    current.days,
                    current.goals,
                    current.today,
                ),
                dialogueReply = null,
            )
        }
    }

    /** カタログ(同梱または自作)から食事を記録する。amountは選んだ数量。 */
    fun addCatalogFood(slot: MealSlot, item: FoodCatalogItem, amount: Double) {
        val scaled = item.scaledTo(amount)
        addFood(
            slot = slot,
            name = scaled.name,
            amount = amount,
            unit = scaled.amountUnit,
            calories = scaled.calories,
            protein = scaled.proteinGrams,
            fat = scaled.fatGrams,
            carbs = scaled.carbsGrams,
            saveAsCustom = false,
        )
    }

    /** 手入力で食事を記録する。saveAsCustomなら自作食品としても保存する。 */
    fun addFood(
        slot: MealSlot,
        name: String,
        amount: Double,
        unit: String,
        calories: Double,
        protein: Double,
        fat: Double,
        carbs: Double,
        saveAsCustom: Boolean,
        vitaminCMg: Double = 0.0,
        calciumMg: Double = 0.0,
        ironMg: Double = 0.0,
    ) {
        if (name.isBlank()) {
            mutableState.update { it.copy(message = "食品名を入力してください") }
            return
        }
        val current = mutableState.value
        viewModelScope.launch {
            runCatching {
                val entry = foods.addEntry(
                    date = current.today,
                    mealSlot = slot,
                    foodName = name,
                    amount = amount,
                    amountUnit = unit,
                    calories = calories,
                    proteinGrams = protein,
                    fatGrams = fat,
                    carbsGrams = carbs,
                    vitaminCMg = vitaminCMg,
                    calciumMg = calciumMg,
                    ironMg = ironMg,
                )
                if (saveAsCustom) {
                    foods.saveCustomFood(name, amount, unit, calories, protein, fat, carbs)
                }
                // Health Connectへも書き込む(権限なし・非対応・デモ中は記録のみ)。
                if (!current.demoMode && current.permissionsGranted) {
                    health.writeNutrition(entry)
                }
            }.onFailure {
                mutableState.update { state -> state.copy(message = "記録を保存できませんでした") }
            }
            refresh()
        }
    }

    fun deleteFood(entry: FoodEntry) {
        val current = mutableState.value
        viewModelScope.launch {
            runCatching {
                foods.deleteEntry(entry.entryId)
                if (!current.demoMode && current.permissionsGranted) {
                    health.deleteNutrition(entry.clientRecordId)
                }
            }
            refresh()
        }
    }

    /** 昨日の記録を今日へコピーする。 */
    fun copyYesterdayMeals() {
        val current = mutableState.value
        viewModelScope.launch {
            val copied = runCatching { foods.copyDay(current.today.minusDays(1), current.today) }.getOrDefault(0)
            if (copied == 0) {
                mutableState.update { it.copy(message = "昨日の記録がありません") }
            } else {
                // コピー分もHealth Connectへ反映する。
                if (!current.demoMode && current.permissionsGranted) {
                    runCatching {
                        foods.entriesOn(current.today).takeLast(copied).forEach { health.writeNutrition(it) }
                    }
                }
                mutableState.update { it.copy(message = "昨日の${copied}件をコピーしました") }
            }
            refresh()
        }
    }

    fun toggleFoodFavorite(foodId: String) {
        val current = mutableState.value
        val nowFavorite = foodId !in current.favoriteFoodIds
        viewModelScope.launch {
            runCatching { foods.setFavorite(foodId, nowFavorite) }
            mutableState.update {
                it.copy(
                    favoriteFoodIds = if (nowFavorite) it.favoriteFoodIds + foodId else it.favoriteFoodIds - foodId,
                )
            }
        }
    }

    /** 栄養データの優先元を切り替える(VitaMorph優先/あすけん優先/日ごとに選択)。 */
    fun setNutritionSource(source: NutritionSource) {
        store.setNutritionSource(source)
        refresh()
    }

    /** 「日ごとに選択」モードで今日の優先元を選ぶ。 */
    fun setTodayNutritionChoice(choice: DayNutritionChoice) {
        store.setNutritionDayChoice(mutableState.value.today, choice)
        refresh()
    }

    /** Open Food Factsから食品名で検索する。送信するのはキーワードのみ。 */
    fun searchExternalFood(query: String) {
        if (query.isBlank()) return
        mutableState.update { it.copy(externalSearching = true, externalResults = emptyList()) }
        viewModelScope.launch {
            val result = openFoodFacts.searchByName(query)
            mutableState.update {
                it.copy(
                    externalSearching = false,
                    externalResults = result.getOrDefault(emptyList()),
                    message = when {
                        result.isFailure -> "ネット検索に失敗しました。オフラインでも同梱カタログは使えます。"
                        result.getOrDefault(emptyList()).isEmpty() -> "見つかりませんでした。手入力もできます。"
                        else -> null
                    },
                )
            }
        }
    }

    /** バーコード番号からOpen Food Factsを照会する。送信するのは番号のみ。 */
    fun lookupBarcode(barcode: String) {
        mutableState.update { it.copy(externalSearching = true, externalResults = emptyList()) }
        viewModelScope.launch {
            val result = openFoodFacts.findByBarcode(barcode)
            val food = result.getOrNull()
            mutableState.update {
                it.copy(
                    externalSearching = false,
                    externalResults = listOfNotNull(food),
                    message = when {
                        result.isFailure -> "照会に失敗しました。通信状態を確認してください。"
                        food == null -> "この商品はデータベースに見つかりませんでした。手入力で記録できます。"
                        else -> "「${food.displayName}」が見つかりました。内容を確認して記録してください。"
                    },
                )
            }
        }
    }

    fun clearExternalResults() {
        mutableState.update { it.copy(externalResults = emptyList()) }
    }

    /** 外部検索結果を確認済みの数量(g)で記録する。ビタミン・ミネラルも換算して保存。 */
    fun addExternalFood(slot: MealSlot, food: ExternalFood, grams: Double) {
        if (grams <= 0) return
        val ratio = grams / 100.0
        val current = mutableState.value
        viewModelScope.launch {
            runCatching {
                val entry = foods.addEntry(
                    date = current.today,
                    mealSlot = slot,
                    foodName = food.displayName.take(40),
                    amount = grams,
                    amountUnit = "g",
                    calories = food.caloriesPer100g * ratio,
                    proteinGrams = food.proteinPer100g * ratio,
                    fatGrams = food.fatPer100g * ratio,
                    carbsGrams = food.carbsPer100g * ratio,
                    vitaminCMg = food.vitaminCMgPer100g * ratio,
                    calciumMg = food.calciumMgPer100g * ratio,
                    ironMg = food.ironMgPer100g * ratio,
                )
                if (!current.demoMode && current.permissionsGranted) {
                    health.writeNutrition(entry)
                }
            }.onFailure {
                mutableState.update { state -> state.copy(message = "記録を保存できませんでした") }
            }
            mutableState.update { it.copy(externalResults = emptyList()) }
            refresh()
        }
    }

    /** 複数の食品を1つのレシピ(自作食品)として保存する。栄養は合計値で記録する。 */
    fun saveRecipe(name: String, parts: List<Pair<FoodCatalogItem, Double>>) {
        if (name.isBlank() || parts.isEmpty()) {
            mutableState.update { it.copy(message = "レシピ名と材料を入力してください") }
            return
        }
        viewModelScope.launch {
            runCatching {
                val scaled = parts.map { (item, amount) -> item.scaledTo(amount) }
                foods.saveCustomFood(
                    name = name,
                    standardAmount = 1.0,
                    amountUnit = "食",
                    calories = scaled.sumOf { it.calories },
                    proteinGrams = scaled.sumOf { it.proteinGrams },
                    fatGrams = scaled.sumOf { it.fatGrams },
                    carbsGrams = scaled.sumOf { it.carbsGrams },
                )
            }.onFailure {
                mutableState.update { state -> state.copy(message = "レシピを保存できませんでした") }
                return@launch
            }
            mutableState.update { it.copy(message = "レシピ「$name」を自作食品として保存しました") }
            refresh()
        }
    }

    private var dialogueSeedBump = 0

    private fun dialogueFor(
        trainerName: String?,
        evolution: EvolutionResult?,
        generation: MonsterGeneration?,
        days: List<DailyHealthData>,
        goals: UserGoals,
        today: LocalDate,
    ): DialogueLine? {
        if (evolution == null || generation == null) return null
        val timeOfDay = TimeOfDay.fromHour(LocalTime.now().hour)
        val todayData = days.lastOrNull { it.date == today }
        val context = DialogueContext(
            trainerName = trainerName,
            stage = evolution.form.stage,
            mood = generation.mood,
            bond = generation.bond,
            timeOfDay = timeOfDay,
            seasonDay = evolution.seasonDay,
            recordedToday = todayData?.let { it.hasNutrition || it.hasActivity } ?: false,
            stepsToday = todayData?.steps ?: 0,
            stepGoal = goals.dailySteps,
            exerciseMinutesToday = todayData?.exerciseMinutes ?: 0,
            lastTournamentWon = mutableState.value.tournament?.let { it.placement == 1 },
        )
        val seed = (today.toEpochDay() + timeOfDay.ordinal * 7 + dialogueSeedBump).toInt()
        return DialogueEngine.greeting(context, seed)
    }

    fun startTournament() {
        val current = mutableState.value
        val evolution = current.evolution ?: return
        val battle = BattleEngine.startTournament(
            evolution.form,
            evolution.metrics,
            seed = current.seasonStart.toEpochDay().toInt() + evolution.seasonDay + current.tournamentPoints,
            mood = current.generation?.mood ?: MonsterGeneration.DEFAULT_MOOD,
            bond = current.generation?.bond ?: MonsterGeneration.DEFAULT_BOND,
            legacy = current.legacyStats,
        )
        mutableState.update { it.copy(battle = battle, tournament = null) }
    }

    /** ミニゲームを開始する。プレイ自体は無制限、機嫌・絆への反映は1日3回まで。 */
    fun startMiniGame(kind: MiniGameKind) {
        mutableState.update {
            it.copy(activeMiniGame = kind, miniGameSeed = System.currentTimeMillis().toInt())
        }
    }

    fun cancelMiniGame() {
        mutableState.update { it.copy(activeMiniGame = null) }
    }

    /** ミニゲーム終了。スコアから成否を判定し、上限内なら機嫌・絆へ反映する。 */
    fun finishMiniGame(score: Int) {
        val current = mutableState.value
        val kind = current.activeMiniGame ?: return
        val result = MiniGameRules.resultFor(kind, score)
        val reward = InteractionEngine.onMiniGame(
            state = current.interaction,
            now = System.currentTimeMillis(),
            today = current.today,
        )
        val moodDelta = if (result.success) MoodEngine.MINIGAME_SUCCESS_MOOD else MoodEngine.MINIGAME_TRY_MOOD
        val bondDelta = if (result.success) MoodEngine.MINIGAME_SUCCESS_BOND else 0
        val generation = current.generation
        val updated = if (generation != null && reward.rewarded) {
            MoodEngine.applyDelta(generation, moodDelta, bondDelta)
        } else generation
        viewModelScope.launch {
            runCatching {
                profiles.saveInteractionState(reward.state)
                if (updated != null && updated != generation) profiles.updateMoodBond(updated)
            }
        }
        val message = buildString {
            append("${kind.label}: ${result.score} / ${result.maxScore}点")
            append(if (result.success) "でクリア！" else "。また挑戦しよう！")
            if (reward.rewarded) {
                append(" 機嫌+$moodDelta")
                if (bondDelta > 0) append("・絆+$bondDelta")
            } else {
                append("(今日の報酬は上限。あそぶのは自由！)")
            }
        }
        mutableState.update {
            it.copy(
                activeMiniGame = null,
                miniGameCelebration = result.success,
                interaction = reward.state,
                generation = updated ?: it.generation,
                message = message,
            )
        }
    }

    /** ミニゲーム成功時の祝福モーションを一定時間後に解除する。 */
    fun clearMiniGameCelebration() {
        mutableState.update { it.copy(miniGameCelebration = false) }
    }

    fun useBattleMove(moveId: String) {
        updateBattle { BattleEngine.useMove(it, moveId) }
    }

    fun useBattleItem(itemId: String) {
        updateBattle { BattleEngine.useItem(it, itemId) }
    }

    fun advanceBattleRound() {
        val current = mutableState.value.battle ?: return
        mutableState.update { it.copy(battle = BattleEngine.nextRound(current)) }
    }

    private fun updateBattle(action: (TurnBattleState) -> TurnBattleState) {
        val current = mutableState.value
        val battle = current.battle ?: return
        val updated = action(battle)
        val result = BattleEngine.resultFor(updated)
        val bestPoints = result?.let { maxOf(current.tournamentPoints, it.tournamentPoints) }
        if (bestPoints != null) store.saveTournamentPoints(bestPoints)
        mutableState.update {
            it.copy(
                battle = updated,
                tournament = result ?: it.tournament,
                tournamentPoints = bestPoints ?: it.tournamentPoints,
            )
        }
    }

    fun advanceDemoWeek() {
        if (!mutableState.value.demoMode) return
        store.advanceDemo(7)
        refresh()
    }

    fun completeSeason() {
        val current = mutableState.value
        val evolution = current.evolution ?: return
        if (evolution.seasonDay < 28) {
            mutableState.update { it.copy(message = "シーズン完了は28日目からです") }
            return
        }
        val seasonMetrics = EvolutionEngine.metrics(current.days, current.goals)
        val points = TrainerRankEngine.seasonPoints(current.days, seasonMetrics, current.tournamentPoints)
        // この世代の過ごし方(食事・運動・交流・大会)から継承ポイント候補を決める。
        val award = LegacyAwardEngine.award(
            metrics = seasonMetrics,
            bond = current.generation?.bond ?: 0,
            tournamentPoints = current.tournamentPoints,
        )
        viewModelScope.launch {
            var grantedTotal = 0
            runCatching {
                val before = profiles.legacyStats()
                val after = before.addingGeneration(award.hp, award.attack, award.defense, award.speed)
                profiles.saveLegacyStats(after)
                // 実際に付与された量(1世代3pt・各能力15ptの上限適用後)を世代へ記録する。
                val grantedHp = after.hpPoints - before.hpPoints
                val grantedAttack = after.attackPoints - before.attackPoints
                val grantedDefense = after.defensePoints - before.defensePoints
                val grantedSpeed = after.speedPoints - before.speedPoints
                grantedTotal = grantedHp + grantedAttack + grantedDefense + grantedSpeed
                profiles.closeCurrentGeneration(
                    endDate = current.today,
                    finalFormId = evolution.form.id,
                    finalPlacement = LegacyAwardEngine.placementForPoints(current.tournamentPoints),
                    awardedHp = grantedHp,
                    awardedAttack = grantedAttack,
                    awardedDefense = grantedDefense,
                    awardedSpeed = grantedSpeed,
                )
            }
            store.completeSeason(points)
            val message = buildString {
                append("シーズン完了: トレーナー経験値 +$points")
                if (grantedTotal > 0) append("・継承ポイント +$grantedTotal")
            }
            mutableState.update { it.copy(tournament = null, battle = null, message = message) }
            refresh()
        }
    }

    fun resetAll() {
        viewModelScope.launch {
            runCatching { profiles.clearAll() }
            runCatching { foods.clearAll() }
            store.reset()
            mutableState.value = GameUiState()
            refresh()
        }
    }

    fun clearMessage() {
        mutableState.update { it.copy(message = null) }
    }
}
