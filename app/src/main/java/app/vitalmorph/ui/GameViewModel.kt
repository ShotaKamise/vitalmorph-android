package app.vitalmorph.ui

import android.app.Application
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.vitalmorph.data.DemoDataFactory
import app.vitalmorph.data.GameStore
import app.vitalmorph.data.HealthConnectRepository
import app.vitalmorph.data.ProfileRepository
import app.vitalmorph.data.db.VitaMorphDatabase
import app.vitalmorph.domain.BattleEngine
import app.vitalmorph.domain.DailyHealthData
import app.vitalmorph.domain.DialogueChoice
import app.vitalmorph.domain.DialogueContext
import app.vitalmorph.domain.DialogueEngine
import app.vitalmorph.domain.DialogueLine
import app.vitalmorph.domain.EvolutionEngine
import app.vitalmorph.domain.EvolutionResult
import app.vitalmorph.domain.InteractionEngine
import app.vitalmorph.domain.InteractionState
import app.vitalmorph.domain.LegacyStats
import app.vitalmorph.domain.MiniGameKind
import app.vitalmorph.domain.MiniGameRules
import app.vitalmorph.domain.MonsterGeneration
import app.vitalmorph.domain.MoodEngine
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
    val legacyStats: LegacyStats = LegacyStats(),
    val interaction: InteractionState = InteractionState(),
    val dialogue: DialogueLine? = null,
    val dialogueReply: String? = null,
    val touchReaction: TouchReactionType? = null,
    val activeMiniGame: MiniGameKind? = null,
    val miniGameSeed: Int = 0,
    val selectedTab: AppTab = AppTab.HOME,
    val message: String? = null,
)

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val store = GameStore(application)
    private val health = HealthConnectRepository(application)
    private val profiles = ProfileRepository(VitaMorphDatabase.getInstance(application))
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
            val rawDays = when {
                !stored.onboardingComplete -> emptyList()
                stored.demoMode -> DemoDataFactory.create(stored.seasonStart, today, stored.goals)
                granted -> runCatching {
                    health.readDays(stored.seasonStart, minOf(today, stored.seasonStart.plusDays(27)), store.workoutTags())
                }.getOrElse { error ->
                    mutableState.update { it.copy(message = "同期できませんでした: ${error.message ?: "不明なエラー"}") }
                    emptyList()
                }
                else -> emptyList()
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
                )
            } else null
            val trainerName = runCatching { profiles.trainerProfile()?.name }.getOrNull()
            val legacyStats = runCatching { profiles.legacyStats() }.getOrDefault(LegacyStats())
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
                    legacyStats = legacyStats,
                    interaction = interaction,
                    dialogue = dialogue,
                    dialogueReply = null,
                    touchReaction = null,
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
                interaction = reward.state,
                generation = updated ?: it.generation,
                message = message,
            )
        }
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
        viewModelScope.launch {
            // 現世代を終了日付きで閉じる。次のrefreshで新世代がランダムな性別で孵化する。
            runCatching { profiles.closeCurrentGeneration(current.today) }
            store.completeSeason(points)
            mutableState.update { it.copy(tournament = null, battle = null, message = "シーズン完了: トレーナー経験値 +$points") }
            refresh()
        }
    }

    fun resetAll() {
        viewModelScope.launch {
            runCatching { profiles.clearAll() }
            store.reset()
            mutableState.value = GameUiState()
            refresh()
        }
    }

    fun clearMessage() {
        mutableState.update { it.copy(message = null) }
    }
}
