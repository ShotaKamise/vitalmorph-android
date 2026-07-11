package app.vitalmorph.ui

import android.app.Application
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.vitalmorph.data.DemoDataFactory
import app.vitalmorph.data.GameStore
import app.vitalmorph.data.HealthConnectRepository
import app.vitalmorph.domain.BattleEngine
import app.vitalmorph.domain.DailyHealthData
import app.vitalmorph.domain.EvolutionEngine
import app.vitalmorph.domain.EvolutionResult
import app.vitalmorph.domain.TournamentResult
import app.vitalmorph.domain.TrainerProgress
import app.vitalmorph.domain.TrainerRank
import app.vitalmorph.domain.TrainerRankEngine
import app.vitalmorph.domain.UserGoals
import app.vitalmorph.domain.WorkoutTag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

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
    val tournamentPoints: Int = 0,
    val selectedTab: AppTab = AppTab.HOME,
    val message: String? = null,
)

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val store = GameStore(application)
    private val health = HealthConnectRepository(application)
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
            val evolution = if (stored.onboardingComplete) {
                EvolutionEngine.evaluate(rawDays, stored.goals, stored.seasonStart, today)
            } else null
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

    fun setWorkoutTag(tag: WorkoutTag) {
        store.setTodayWorkoutTag(mutableState.value.today, tag)
        refresh()
    }

    fun runTournament() {
        val current = mutableState.value
        val evolution = current.evolution ?: return
        val result = BattleEngine.runTournament(
            evolution.form,
            evolution.metrics,
            seed = current.seasonStart.toEpochDay().toInt() + evolution.seasonDay + current.tournamentPoints,
        )
        store.saveTournamentPoints(result.tournamentPoints)
        mutableState.update { it.copy(tournament = result, tournamentPoints = result.tournamentPoints) }
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
        store.completeSeason(points)
        mutableState.update { it.copy(tournament = null, message = "シーズン完了: トレーナー経験値 +$points") }
        refresh()
    }

    fun resetAll() {
        store.reset()
        mutableState.value = GameUiState()
        refresh()
    }

    fun clearMessage() {
        mutableState.update { it.copy(message = null) }
    }
}
