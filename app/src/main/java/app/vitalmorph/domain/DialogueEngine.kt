package app.vitalmorph.domain

/** 会話の時間帯。 */
enum class TimeOfDay(val label: String) {
    MORNING("朝"),
    DAYTIME("昼"),
    EVENING("夕方"),
    NIGHT("夜"),
    ;

    companion object {
        fun fromHour(hour: Int): TimeOfDay = when (hour) {
            in 5..10 -> MORNING
            in 11..16 -> DAYTIME
            in 17..21 -> EVENING
            else -> NIGHT
        }
    }
}

/**
 * 会話生成に使うコンテキスト(DATA_MODEL.mdのDialogueContext)。
 * 表示のたびに計算し、会話文自体は永続化しない。
 */
data class DialogueContext(
    val trainerName: String?,
    val stage: MonsterStage,
    val mood: Int,
    val bond: Int,
    val timeOfDay: TimeOfDay,
    val seasonDay: Int,
    val recordedToday: Boolean,
    val stepsToday: Long = 0,
    val stepGoal: Int = 0,
    val exerciseMinutesToday: Long = 0,
    val lastTournamentWon: Boolean? = null,
)

/** 会話の選択肢。選ぶと機嫌・絆が変化する(1日の反映上限あり)。 */
data class DialogueChoice(
    val id: String,
    val text: String,
    val reply: String,
    val moodDelta: Int,
    val bondDelta: Int,
)

data class DialogueLine(
    val text: String,
    val choices: List<DialogueChoice>,
)

/**
 * 端末内テンプレートによるローカル会話。サーバーも生成AIも使わない。
 * 文体ルール: 責めない、命令しない、医療的な断定をしない。
 */
object DialogueEngine {
    const val FALLBACK_TRAINER_NAME = "トレーナー"
    private const val NAME = "{name}"

    /**
     * コンテキストとシードから今日の一言を決める。
     * 同じコンテキストとシードなら同じ文になる(テスト可能・再表示で変わらない)。
     */
    fun greeting(context: DialogueContext, seed: Int): DialogueLine {
        val pool = buildPool(context)
        val text = pool[Math.floorMod(seed, pool.size)]
        return DialogueLine(
            text = render(text, context.trainerName),
            choices = choicesFor(context).map { it.copy(reply = render(it.reply, context.trainerName)) },
        )
    }

    private fun buildPool(context: DialogueContext): List<String> {
        val lines = mutableListOf<String>()
        lines += when (context.timeOfDay) {
            TimeOfDay.MORNING -> listOf(
                "$NAME、おはよう!今日も少しずつ進もうね。",
                "おはよう、$NAME。朝の光は気持ちいいね。",
            )
            TimeOfDay.DAYTIME -> listOf(
                "$NAME、調子はどう?水分補給も忘れずにね。",
                "$NAME、お昼の時間はゆっくりできた?",
            )
            TimeOfDay.EVENING -> listOf(
                "$NAME、今日もおつかれさま。",
                "夕方の風が気持ちいいね、$NAME。",
            )
            TimeOfDay.NIGHT -> listOf(
                "$NAME、そろそろ休む時間だね。",
                "夜ふかしはほどほどにね、$NAME。ぼくも眠くなってきた…。",
            )
        }
        when (MoodEngine.moodBand(context.mood)) {
            MoodBand.GREAT -> lines += "$NAME、今日は体が軽いんだ!何でもできそうな気分!"
            MoodBand.GOOD -> lines += "いい調子だよ、$NAME。この感じ、続けたいな。"
            MoodBand.NORMAL -> {}
            MoodBand.LOW -> lines += "今日はちょっとおとなしめかも。$NAME がいると安心する。"
            MoodBand.BAD -> lines += "今日は元気が出ないみたい…。$NAME、そばにいてくれる?"
        }
        if (context.stepGoal > 0 && context.stepsToday >= context.stepGoal) {
            lines += "$NAME、今日の歩数目標を達成したんだね!いっしょに歩けてうれしいよ。"
        } else if (context.exerciseMinutesToday >= 20) {
            lines += "今日はしっかり体を動かしたね、$NAME。ぼくにも力が湧いてくるよ。"
        }
        if (!context.recordedToday) {
            lines += "$NAME、今日の記録はこれからかな?あとで少しだけ教えてね。"
        }
        when (context.lastTournamentWon) {
            true -> lines += "この前の大会、優勝したんだよね。$NAME のおかげだよ!"
            false -> lines += "大会は残念だったけど、$NAME と一緒ならまた強くなれるよ。"
            null -> {}
        }
        if (context.seasonDay >= 22 && context.stage == MonsterStage.FINAL) {
            lines += "この姿になれたのは $NAME のおかげ。残りの日々も楽しもうね。"
        }
        return lines
    }

    private fun choicesFor(context: DialogueContext): List<DialogueChoice> =
        when (MoodEngine.bondBand(context.bond)) {
            BondBand.DISTANT -> listOf(
                DialogueChoice("greet", "よろしくね", "うん…!よろしくね、$NAME。", 1, 1),
                DialogueChoice("watch", "見守っているよ", "ありがとう。少しずつ慣れていくね。", 0, 1),
            )
            BondBand.FRIENDLY -> listOf(
                DialogueChoice("cheer", "今日もいっしょにがんばろう", "うん!$NAME とならがんばれるよ!", 2, 1),
                DialogueChoice("rest", "ゆっくり休もうね", "そうだね。休むのも大事だもんね。", 1, 1),
            )
            BondBand.DEVOTED -> listOf(
                DialogueChoice("cheer", "今日もいっしょにがんばろう", "もちろん!$NAME はぼくの相棒だからね!", 2, 1),
                DialogueChoice("thanks", "いつもありがとう", "こちらこそ、いつもありがとう…!", 2, 1),
            )
        }

    /** テンプレートの {name} をトレーナー名(未設定なら「トレーナー」)へ置き換える。 */
    fun render(template: String, trainerName: String?): String =
        template.replace(NAME, trainerName?.takeIf { it.isNotBlank() } ?: FALLBACK_TRAINER_NAME)
}
