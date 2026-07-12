package app.vitalmorph.domain

import java.time.LocalDate

/**
 * 交流の永続状態(DATA_MODEL.mdのInteractionState)。
 * lastInteractionAt はタッチと会話の両方で更新する(旧名 lastTouchedAt)。
 * 端末時刻を過去に戻しても回数が復活しないよう、日付の巻き戻りではリセットしない。
 */
data class InteractionState(
    val lastInteractionAt: Long = 0L,
    val consecutiveTouches: Int = 0,
    val touchRewardCountToday: Int = 0,
    val conversationCountToday: Int = 0,
    val miniGameRewardCountToday: Int = 0,
    val lastDailyResetDate: LocalDate? = null,
)

/** タッチした部位。座標からの判定はUI側で行い、ロジックはここに置く。 */
enum class TouchArea {
    HEAD,
    BODY,
}

/**
 * タッチへの反応。表示モーションへの対応:
 * HAPPY→TOUCH_HAPPY、SHY→TOUCH_SHY、ANNOYED→TOUCH_ANNOYED(Codex制作待ち。
 * それまでは既存モーションから選択する)。NONEはクールダウン中で無反応。
 */
enum class TouchReactionType {
    HAPPY,
    SHY,
    ANNOYED,
    NONE,
}

data class TouchResult(
    val reaction: TouchReactionType,
    val moodDelta: Int,
    val bondDelta: Int,
    /** 1日の上限内で機嫌・絆へ反映されたか。上限超過でも反応は表示する。 */
    val rewarded: Boolean,
    val state: InteractionState,
)

data class ConversationRewardResult(
    val rewarded: Boolean,
    val state: InteractionState,
)

/**
 * タッチ・会話の反応と報酬上限を決める純ロジック。
 * 時刻は必ず引数で受け取り、Androidに依存しない。
 */
object InteractionEngine {
    /** この間隔より速い連続タッチは無反応(連打対策の第一段階)。 */
    const val TOUCH_COOLDOWN_MS = 800L

    /** この間隔以内のタッチを「連続タッチ」として数える。 */
    const val RAPID_WINDOW_MS = 5_000L

    /** 連続タッチがこの回数を超えると嫌がる。 */
    const val RAPID_TOUCH_LIMIT = 6

    /**
     * 性格ごとの連続タッチ許容回数(v0.11)。能力差はなく、ふれあいの手触りだけを変える。
     * あまえんぼうは構われるのが好きで+2、クールはしつこいのが苦手で-2、その他は既定。
     */
    fun rapidTouchLimitFor(personality: Personality): Int = RAPID_TOUCH_LIMIT + when (personality) {
        Personality.AFFECTIONATE -> 2
        Personality.COOL -> -2
        else -> 0
    }

    /**
     * 日付が変わっていれば1日ごとの回数をリセットする。
     * 端末時刻が過去に戻った場合(today < lastDailyResetDate)はリセットしない。
     */
    fun resetIfNewDay(state: InteractionState, today: LocalDate): InteractionState {
        val last = state.lastDailyResetDate
        if (last != null && !today.isAfter(last)) return state
        return state.copy(
            touchRewardCountToday = 0,
            conversationCountToday = 0,
            miniGameRewardCountToday = 0,
            lastDailyResetDate = today,
        )
    }

    fun onTouch(
        state: InteractionState,
        now: Long,
        today: LocalDate,
        area: TouchArea,
        mood: Int,
        personality: Personality = Personality.HARDWORKER,
    ): TouchResult {
        val fresh = resetIfNewDay(state, today)
        val sinceLast = now - fresh.lastInteractionAt
        if (fresh.lastInteractionAt > 0 && sinceLast in 0 until TOUCH_COOLDOWN_MS) {
            return TouchResult(TouchReactionType.NONE, 0, 0, rewarded = false, state = fresh)
        }

        val consecutive = if (fresh.lastInteractionAt > 0 && sinceLast in 0..RAPID_WINDOW_MS) {
            fresh.consecutiveTouches + 1
        } else {
            1
        }
        val updated = fresh.copy(lastInteractionAt = now, consecutiveTouches = consecutive)

        if (consecutive > rapidTouchLimitFor(personality)) {
            // しつこいタッチ。機嫌は下がるが、報酬上限とは無関係に1回だけ小さく下げる。
            return TouchResult(
                reaction = TouchReactionType.ANNOYED,
                moodDelta = MoodEngine.TOUCH_ANNOYED_MOOD,
                bondDelta = 0,
                rewarded = false,
                state = updated,
            )
        }

        val reaction = when (area) {
            TouchArea.HEAD -> if (mood >= 60) TouchReactionType.HAPPY else TouchReactionType.SHY
            TouchArea.BODY -> if (mood >= 40) TouchReactionType.HAPPY else TouchReactionType.SHY
        }
        val rewarded = updated.touchRewardCountToday < MoodEngine.TOUCH_REWARDS_PER_DAY
        val moodDelta = when {
            !rewarded -> 0
            reaction == TouchReactionType.HAPPY -> MoodEngine.TOUCH_HAPPY_MOOD
            else -> MoodEngine.TOUCH_SHY_MOOD
        }
        return TouchResult(
            reaction = reaction,
            moodDelta = moodDelta,
            bondDelta = if (rewarded) MoodEngine.TOUCH_BOND else 0,
            rewarded = rewarded,
            state = if (rewarded) updated.copy(touchRewardCountToday = updated.touchRewardCountToday + 1) else updated,
        )
    }

    /** 会話の選択肢を選んだときの報酬判定。反映は1日の上限まで。 */
    fun onConversation(state: InteractionState, now: Long, today: LocalDate): ConversationRewardResult {
        val fresh = resetIfNewDay(state, today).copy(lastInteractionAt = now, consecutiveTouches = 0)
        val rewarded = fresh.conversationCountToday < MoodEngine.DIALOGUE_REWARDS_PER_DAY
        return ConversationRewardResult(
            rewarded = rewarded,
            state = if (rewarded) fresh.copy(conversationCountToday = fresh.conversationCountToday + 1) else fresh,
        )
    }

    /**
     * ミニゲームを1回終えたときの報酬判定。反映は1日 [MiniGameRules.REWARDS_PER_DAY] 回まで。
     * プレイ自体は何回でもできる(報酬が付かないだけ)。
     */
    fun onMiniGame(state: InteractionState, now: Long, today: LocalDate): ConversationRewardResult {
        val fresh = resetIfNewDay(state, today).copy(lastInteractionAt = now, consecutiveTouches = 0)
        val rewarded = fresh.miniGameRewardCountToday < MiniGameRules.REWARDS_PER_DAY
        return ConversationRewardResult(
            rewarded = rewarded,
            state = if (rewarded) fresh.copy(miniGameRewardCountToday = fresh.miniGameRewardCountToday + 1) else fresh,
        )
    }
}
