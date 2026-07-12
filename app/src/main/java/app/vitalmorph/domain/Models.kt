package app.vitalmorph.domain

import java.time.LocalDate

enum class WorkoutTag {
    NONE,
    CARDIO,
    STRENGTH,
    OTHER,
}

data class DailyHealthData(
    val date: LocalDate,
    val calories: Double = 0.0,
    val proteinGrams: Double = 0.0,
    val carbsGrams: Double = 0.0,
    val fatGrams: Double = 0.0,
    val steps: Long = 0,
    val activeCalories: Double = 0.0,
    val exerciseMinutes: Long = 0,
    val workoutTag: WorkoutTag = WorkoutTag.NONE,
    val hasNutrition: Boolean = false,
    val hasActivity: Boolean = false,
)

data class UserGoals(
    val calories: Int = 2_000,
    val proteinGrams: Int = 80,
    val carbsGrams: Int = 250,
    val fatGrams: Int = 60,
    val dailySteps: Int = 8_000,
    val weeklyExerciseMinutes: Int = 150,
)

enum class MonsterStage(val label: String) {
    BABY("幼生"),
    FAMILY("成長体"),
    INTERMEDIATE("成熟体"),
    FINAL("最終形態"),
}

enum class MonsterFamily(val label: String) {
    BALANCE("調和系"),
    POWER("筋力系"),
    SPEED("俊足系"),
    STORAGE("蓄積系"),
    REST("静養系"),
    OVERDRIVE("過活動系"),
}

data class MonsterForm(
    val id: String,
    val name: String,
    val stage: MonsterStage,
    val family: MonsterFamily,
    val role: String,
    val description: String,
    val accent: Long,
)

data class EvolutionMetrics(
    val nutritionScore: Int = 0,
    val activityScore: Int = 0,
    val consistencyScore: Int = 0,
    val calorieRatio: Double = 0.0,
    val proteinRatio: Double = 0.0,
    val carbsRatio: Double = 0.0,
    val fatRatio: Double = 0.0,
    val stepGoalDays: Int = 0,
    val strengthDays: Int = 0,
    val cardioDays: Int = 0,
    val highActivityDays: Int = 0,
    val restDays: Int = 0,
    val activityTrend: Double = 0.0,
    val calorieTrend: Double = 0.0,
    val weekdayWeekendVariance: Double = 0.0,
    val recordedDays: Int = 0,
)

data class EvolutionResult(
    val seasonDay: Int,
    val form: MonsterForm,
    val path: List<MonsterForm>,
    val finalCandidates: List<MonsterForm>,
    val metrics: EvolutionMetrics,
    val nextEvolutionDay: Int?,
)

data class TrainerProgress(
    val xp: Int = 0,
    val completedSeasons: Int = 0,
)

data class TrainerRank(
    val name: String,
    val minimumXp: Int,
    val minimumSeasons: Int,
    val nextRankXp: Int?,
)

data class BattleMatch(
    val round: String,
    val opponent: String,
    val playerScore: Int,
    val opponentScore: Int,
    val won: Boolean,
)

data class TournamentResult(
    val placement: Int,
    val tournamentPoints: Int,
    val matches: List<BattleMatch>,
)

enum class BattleMoveKind {
    ATTACK,
    GUARD,
    RECOVERY,
}

data class BattleMove(
    val id: String,
    val name: String,
    val description: String,
    val kind: BattleMoveKind,
    val power: Int,
    val energyCost: Int,
    val priority: Int = 0,
    val heal: Int = 0,
    val recoil: Int = 0,
)

data class BattleItem(
    val id: String,
    val name: String,
    val description: String,
)

data class BattleItemStock(
    val item: BattleItem,
    val remaining: Int,
)

enum class BattleOutcome {
    IN_PROGRESS,
    ROUND_WON,
    TOURNAMENT_WON,
    PLAYER_LOST,
}

/** ターン内の行動主体。プレイヤー側かCPU側かを示す。 */
enum class BattleActor { PLAYER, OPPONENT }

/** 順次演出のためのイベント種別。 */
enum class BattleEventKind { MOVE, ITEM, GUARD, HEAL, DAMAGE_DEALT, ANNOUNCE }

/**
 * 1ターン内で発生した個々の出来事(演出専用データ)。
 *
 * ゲームロジックには影響せず、UIが技→ダメージの順で段階的に再生するための派生情報。
 * `resolveTurn` が確定後のHP等と矛盾しないように記録する。保存(Codec)には含めない。
 */
data class TurnEvent(
    val actor: BattleActor,
    val kind: BattleEventKind,
    val label: String,          // 技名・アイテム名・メッセージ
    val damage: Int = 0,        // 相手に与えたダメージ(DAMAGE_DEALT時)
    val heal: Int = 0,
    val targetHpAfter: Int = -1, // このイベント適用後の被弾側HP(UIの段階的HPバー用)
    val actorHpAfter: Int = -1,  // 行動側HP(回復・反動用)
)

data class TurnBattleState(
    val roundIndex: Int,
    val roundLabel: String,
    val playerName: String,
    val opponentName: String,
    /** 相手フォームのID(姿の描画に使う)。旧スナップショットとの互換のため既定は空。 */
    val opponentFormId: String = "",
    /** 大会の週(1..4)。相手能力の週係数と週ポイント記録に使う。既定は第4週。 */
    val week: Int = 4,
    /** 練習試合(同週の再挑戦)かどうか。trueならポイントを加算しない。 */
    val practice: Boolean = false,
    val playerHp: Int,
    val playerMaxHp: Int,
    val opponentHp: Int,
    val opponentMaxHp: Int,
    val playerEnergy: Int,
    val opponentEnergy: Int,
    val playerAttack: Int,
    val playerDefense: Int,
    val playerSpeed: Int,
    val opponentAttack: Int,
    val opponentDefense: Int,
    val opponentSpeed: Int,
    val playerGuarding: Boolean,
    val opponentGuarding: Boolean,
    val opponentPotions: Int,
    val moves: List<BattleMove>,
    val items: List<BattleItemStock>,
    val turn: Int,
    val seed: Int,
    val outcome: BattleOutcome,
    val log: List<String>,
    val completedMatches: List<BattleMatch>,
    /** 機嫌による各試合の開始エネルギー(通常3、不調時2)。 */
    val playerStartEnergy: Int = 3,
    /** 絶好調時に各試合の開始時へ張られる小さなシールド。 */
    val playerStartShield: Boolean = false,
    /**
     * 直近のターンで発生したイベント列(演出専用・保存対象外)。
     * ターンごとに置き換え、ラウンド遷移時は空になる。
     */
    val lastTurnEvents: List<TurnEvent> = emptyList(),
)
