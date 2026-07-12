package app.vitalmorph.data

import android.content.Context
import app.vitalmorph.domain.TrainerProgress
import app.vitalmorph.domain.UserGoals
import app.vitalmorph.domain.WorkoutTag
import org.json.JSONObject
import java.time.LocalDate

data class StoredGameState(
    val onboardingComplete: Boolean,
    val goals: UserGoals,
    val seasonStart: LocalDate,
    val trainerProgress: TrainerProgress,
    val demoMode: Boolean,
    val demoDayOffset: Int,
    val tournamentPoints: Int,
)

class GameStore(context: Context) {
    private val preferences = context.getSharedPreferences("vitalmorph_game", Context.MODE_PRIVATE)

    fun load(): StoredGameState = StoredGameState(
        onboardingComplete = preferences.getBoolean("onboarding_complete", false),
        goals = UserGoals(
            calories = preferences.getInt("goal_calories", 2_000),
            proteinGrams = preferences.getInt("goal_protein", 80),
            carbsGrams = preferences.getInt("goal_carbs", 250),
            fatGrams = preferences.getInt("goal_fat", 60),
            dailySteps = preferences.getInt("goal_steps", 8_000),
            weeklyExerciseMinutes = preferences.getInt("goal_exercise", 150),
        ),
        seasonStart = preferences.getString("season_start", null)?.let(LocalDate::parse) ?: LocalDate.now(),
        trainerProgress = TrainerProgress(
            xp = preferences.getInt("trainer_xp", 0),
            completedSeasons = preferences.getInt("completed_seasons", 0),
        ),
        demoMode = preferences.getBoolean("demo_mode", false),
        demoDayOffset = preferences.getInt("demo_day_offset", 0),
        tournamentPoints = preferences.getInt("tournament_points", 0),
    )

    fun completeOnboarding(goals: UserGoals, demoMode: Boolean) {
        preferences.edit()
            .putBoolean("onboarding_complete", true)
            .putInt("goal_calories", goals.calories)
            .putInt("goal_protein", goals.proteinGrams)
            .putInt("goal_carbs", goals.carbsGrams)
            .putInt("goal_fat", goals.fatGrams)
            .putInt("goal_steps", goals.dailySteps)
            .putInt("goal_exercise", goals.weeklyExerciseMinutes)
            .putBoolean("demo_mode", demoMode)
            .putString("season_start", LocalDate.now().toString())
            .apply()
    }

    fun saveTournamentPoints(points: Int) {
        preferences.edit().putInt("tournament_points", points).apply()
    }

    /**
     * 進行中の大会状態(JSONスナップショット)を保存する。nullで消去する。
     * これは一時的なゲーム状態のため `StoredGameState`/`load()` には含めない。
     */
    fun saveBattleState(json: String?) {
        preferences.edit().apply {
            if (json == null) remove("battle_state") else putString("battle_state", json)
        }.apply()
    }

    /** 保存済みの進行中大会状態のJSON。無ければnull。 */
    fun battleState(): String? = preferences.getString("battle_state", null)

    fun setTodayWorkoutTag(date: LocalDate, tag: WorkoutTag) {
        val json = JSONObject(preferences.getString("workout_tags", "{}") ?: "{}")
        json.put(date.toString(), tag.name)
        preferences.edit().putString("workout_tags", json.toString()).apply()
    }

    fun workoutTags(): Map<LocalDate, WorkoutTag> {
        val json = JSONObject(preferences.getString("workout_tags", "{}") ?: "{}")
        return json.keys().asSequence().mapNotNull { key ->
            runCatching { LocalDate.parse(key) to WorkoutTag.valueOf(json.getString(key)) }.getOrNull()
        }.toMap()
    }

    fun advanceDemo(days: Int) {
        preferences.edit().putInt("demo_day_offset", preferences.getInt("demo_day_offset", 0) + days).apply()
    }

    fun completeSeason(points: Int) {
        preferences.edit()
            .putInt("trainer_xp", preferences.getInt("trainer_xp", 0) + points)
            .putInt("completed_seasons", preferences.getInt("completed_seasons", 0) + 1)
            .putString("season_start", LocalDate.now().toString())
            .putInt("demo_day_offset", 0)
            .putInt("tournament_points", 0)
            .apply()
    }

    fun reset() {
        preferences.edit().clear().apply()
    }
}
