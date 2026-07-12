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
    val personality: Personality = Personality.HARDWORKER,
)

/**
 * 会話の選択肢。選ぶと機嫌・絆が変化する(1日の反映上限あり)。
 * reply はその選択肢へのモンスターの返事、followUp はその後に添える軽い一言。
 */
data class DialogueChoice(
    val id: String,
    val text: String,
    val reply: String,
    val followUp: String,
    val moodDelta: Int,
    val bondDelta: Int,
)

/**
 * ひとまとまりの話題。モンスターの一言(line)と、その一言に合わせて書かれた2つの選択肢を束ねる。
 * 各選択肢の reply はその選択肢に、followUp はさらに会話が続く自然な流れになるよう書く。
 */
data class DialogueTopic(
    val id: String,
    val line: String,
    val choices: List<DialogueChoice>,
)

data class DialogueLine(
    val text: String,
    val choices: List<DialogueChoice>,
)

/**
 * 端末内テンプレートによるローカル会話。サーバーも生成AIも使わない。
 * 文体ルール: 責めない、命令しない、医療的な断定をしない。
 *
 * 会話は「話題(DialogueTopic)」単位で構成する。ひとつの話題は
 * モンスターの一言 → その一言に対する自然な返答の選択肢 → 選択肢に沿った返事 → 軽い一言、
 * という一貫した流れになるよう書かれている。
 */
object DialogueEngine {
    const val FALLBACK_TRAINER_NAME = "トレーナー"
    private const val NAME = "{name}"

    /**
     * コンテキストとシードから今日の話題を決める。
     * 同じコンテキストとシードなら同じ話題になる(テスト可能・再表示で変わらない)。
     * 「また話す」はシードを進めて別の話題に切り替える。
     */
    fun greeting(context: DialogueContext, seed: Int): DialogueLine {
        val pool = buildTopics(context)
        val topic = pool[Math.floorMod(seed, pool.size)]
        val name = context.trainerName
        return DialogueLine(
            text = render(topic.line, name),
            choices = topic.choices.map {
                it.copy(
                    text = render(it.text, name),
                    reply = render(it.reply, name),
                    followUp = render(it.followUp, name),
                )
            },
        )
    }

    /** コンテキストに応じて成立する話題を集める。順序は固定でシードに対して決定的。 */
    private fun buildTopics(context: DialogueContext): List<DialogueTopic> {
        val topics = mutableListOf<DialogueTopic>()
        topics += timeOfDayTopic(context.personality, context.timeOfDay)
        moodTopic(context.mood)?.let { topics += it }
        activityTopic(context)?.let { topics += it }
        if (!context.recordedToday) topics += noRecordTopic()
        tournamentTopic(context.lastTournamentWon)?.let { topics += it }
        if (context.seasonDay >= 22 && context.stage == MonsterStage.FINAL) topics += finalFormTopic()
        topics += generalTopics()
        topics += personalityTopics(context.personality)
        return topics
    }

    // ---- 時間帯の話題(性格ごとに口調を変える) ----

    private fun timeOfDayTopic(personality: Personality, time: TimeOfDay): DialogueTopic =
        DialogueTopic(
            id = "time_${time.name.lowercase()}",
            line = timeLine(personality, time),
            choices = greetingChoices(personality),
        )

    private fun timeLine(personality: Personality, time: TimeOfDay): String = when (personality) {
        Personality.HARDWORKER -> when (time) {
            TimeOfDay.MORNING -> "$NAME、おはよう!今日も全力でいこうね。ぼく、朝からやる気まんたんだよ!"
            TimeOfDay.DAYTIME -> "$NAME、お昼だね!午後もぼくと一緒に、元気いっぱいでいこう!"
            TimeOfDay.EVENING -> "$NAME、今日もよくがんばったね!きみを見てて、ぼく誇らしかったよ。"
            TimeOfDay.NIGHT -> "$NAME、夜だね。今日の全力、ちゃんと届いたよ。ゆっくり休んで、明日もいこう!"
        }
        Personality.EASYGOING -> when (time) {
            TimeOfDay.MORNING -> "$NAME、おはよう〜。今日ものんびり、いい一日にしようね。"
            TimeOfDay.DAYTIME -> "$NAME、お昼だね〜。ちょっと一息ついて、ゆっくりしよ?"
            TimeOfDay.EVENING -> "$NAME、夕方だね。今日もおつかれさま。ゆるゆるいこうね〜。"
            TimeOfDay.NIGHT -> "$NAME、夜だね〜。ふわぁ…なんだか眠くなってきちゃった。"
        }
        Personality.COOL -> when (time) {
            TimeOfDay.MORNING -> "$NAME か。…おはよう。まあ、来てくれてよかったよ。"
            TimeOfDay.DAYTIME -> "$NAME、昼か。…ちゃんと一息ついてるか?"
            TimeOfDay.EVENING -> "$NAME、夕方だな。今日もよくやったんじゃないか。…別に、褒めてないけど。"
            TimeOfDay.NIGHT -> "$NAME、もう夜か。夜ふかしはするなよ。…ぼくが見ててやるからさ。"
        }
        Personality.AFFECTIONATE -> when (time) {
            TimeOfDay.MORNING -> "$NAME〜!おはよう!朝いちばんに会えて、うれしいなあ。"
            TimeOfDay.DAYTIME -> "$NAME、お昼だよ〜。ねえねえ、ちょっとだけかまって?"
            TimeOfDay.EVENING -> "$NAME、夕方だね。今日、いっぱい会いたかったんだよ〜。"
            TimeOfDay.NIGHT -> "$NAME〜、夜だね。ねえ、もう少しだけ一緒にいてもいい?"
        }
        Personality.CAPRICIOUS -> when (time) {
            TimeOfDay.MORNING -> "$NAME、おはよ!今日はなんだか…そうだ、いい気分になる予感がするよ!"
            TimeOfDay.DAYTIME -> "$NAME、お昼か〜。ねえ、午後は何しよっか?あ、やっぱりあとで決める!"
            TimeOfDay.EVENING -> "$NAME、夕方!今日はどんな一日だった?ぼくはね、まあまあ気分屋だったよ。"
            TimeOfDay.NIGHT -> "$NAME、夜だね。もう眠い…と思ったけど、やっぱりもう少し話そっか!"
        }
    }

    /** 時間帯の一言への返答。性格ごとに口調と返事のトーンをそろえる。 */
    private fun greetingChoices(personality: Personality): List<DialogueChoice> = when (personality) {
        Personality.HARDWORKER -> listOf(
            DialogueChoice(
                "greet_cheer", "いっしょにがんばろう",
                "うん、その意気だよ、$NAME!ぼくたちなら、きっとやれる!",
                "今日も一歩ずつ、前へ進もうね!", 2, 1,
            ),
            DialogueChoice(
                "greet_rest", "ちょっと休みたいな",
                "もちろん!しっかり休むのも、大事な準備だよ。",
                "エネルギーがたまったら、また一緒に走ろうね!", 1, 1,
            ),
        )
        Personality.EASYGOING -> listOf(
            DialogueChoice(
                "greet_slow", "今日ものんびりいこう",
                "うんうん、それがいちばんだよ〜。焦らなくて大丈夫。",
                "$NAME のペースで、ゆっくりね。", 2, 1,
            ),
            DialogueChoice(
                "greet_how", "元気にしてた?",
                "ふふ、おかげさまで、のんびり元気だよ〜。",
                "$NAME も、無理しないでね。", 1, 1,
            ),
        )
        Personality.COOL -> listOf(
            DialogueChoice(
                "greet_ok", "調子はどう?",
                "…まあ、悪くない。きみが気にかけてくれるからな。",
                "きみも、ちゃんと体を大事にしろよ。", 2, 1,
            ),
            DialogueChoice(
                "greet_stay", "そばにいるよ",
                "…ふん。別に、いてくれなんて頼んでないけど。…ありがとな。",
                "その、これからもよろしく、$NAME。", 1, 1,
            ),
        )
        Personality.AFFECTIONATE -> listOf(
            DialogueChoice(
                "greet_come", "こっちおいで",
                "わーい!$NAME、だいすき!えへへ、うれしいなあ。",
                "ずっと、いっしょにいたいな〜。", 3, 2,
            ),
            DialogueChoice(
                "greet_later", "また後で遊ぼうね",
                "ほんと?やくそくだよ〜!楽しみに待ってるね。",
                "早く $NAME と遊びたいなあ。", 2, 1,
            ),
        )
        Personality.CAPRICIOUS -> listOf(
            DialogueChoice(
                "greet_play", "何して遊ぶ?",
                "えっとね〜、うーん、決めた!…あ、やっぱり気分で決めよ!",
                "きまぐれだけど、$NAME となら楽しいよ。", 2, 1,
            ),
            DialogueChoice(
                "greet_pace", "きみのペースでいいよ",
                "お、$NAME わかってるね〜。ぼく、そういうの好きだよ。",
                "気分屋なぼくだけど、よろしくね!", 2, 1,
            ),
        )
    }

    // ---- 機嫌の話題(共通文。返事は穏やかでどの性格とも衝突しない) ----

    private fun moodTopic(mood: Int): DialogueTopic? = when (MoodEngine.moodBand(mood)) {
        MoodBand.GREAT -> DialogueTopic(
            "mood_great",
            "今日は体が軽いんだ!$NAME、なんだか何でもできそうな気分だよ!",
            listOf(
                DialogueChoice(
                    "mood_go", "その調子だね",
                    "うん!この感じ、大事にしたいな。",
                    "$NAME と一緒だと、もっと元気が出るよ。", 2, 1,
                ),
                DialogueChoice(
                    "mood_care", "無理はしないでね",
                    "ありがとう。ちゃんと自分のペースも忘れないね。",
                    "気にかけてくれて、うれしいよ。", 1, 1,
                ),
            ),
        )
        MoodBand.GOOD -> DialogueTopic(
            "mood_good",
            "いい調子だよ、$NAME。この穏やかな感じ、続けられたらいいな。",
            listOf(
                DialogueChoice(
                    "mood_nice", "いい感じだね",
                    "うん、$NAME のおかげかも。ありがとう。",
                    "この調子で、ゆっくりいこうね。", 2, 1,
                ),
                DialogueChoice(
                    "mood_ask", "何かあった?",
                    "ううん、ただなんとなく気分がいいんだ。",
                    "きみと話すと、もっと落ち着くよ。", 1, 1,
                ),
            ),
        )
        MoodBand.NORMAL -> null
        MoodBand.LOW -> DialogueTopic(
            "mood_low",
            "今日はちょっとおとなしめかも…。$NAME がいてくれると、安心するな。",
            listOf(
                DialogueChoice(
                    "mood_near", "そばにいるよ",
                    "うん…ありがとう。$NAME がいると、ほっとする。",
                    "少しずつ、元気を取り戻していくね。", 2, 1,
                ),
                DialogueChoice(
                    "mood_slow", "ゆっくりしようね",
                    "そうだね…今日は、のんびりさせてもらおうかな。",
                    "また明日、いつもの調子に戻れる気がするよ。", 1, 1,
                ),
            ),
        )
        MoodBand.BAD -> DialogueTopic(
            "mood_bad",
            "今日はあまり元気が出ないみたい…。$NAME、少しそばにいてくれる?",
            listOf(
                DialogueChoice(
                    "mood_stay", "ずっとそばにいるよ",
                    "…うん。$NAME がいてくれるだけで、心強いよ。",
                    "焦らずいこうね。ぼくは大丈夫。", 2, 1,
                ),
                DialogueChoice(
                    "mood_rest", "無理しなくていいよ",
                    "ありがとう…。今日はゆっくり休ませてもらうね。",
                    "また元気になったら、いっぱい話そうね。", 1, 1,
                ),
            ),
        )
    }

    // ---- 活動の話題 ----

    private fun activityTopic(context: DialogueContext): DialogueTopic? = when {
        context.stepGoal > 0 && context.stepsToday >= context.stepGoal -> DialogueTopic(
            "activity_steps",
            "$NAME、今日の歩数目標を達成したんだね!いっしょに歩けて、うれしいよ。",
            listOf(
                DialogueChoice(
                    "steps_fun", "いっしょに歩けて楽しかった",
                    "ぼくもだよ!$NAME と歩く道は、特別に感じるんだ。",
                    "また明日も、いっしょに歩こうね。", 2, 1,
                ),
                DialogueChoice(
                    "steps_thanks", "きみのおかげだよ",
                    "えへへ、そう言ってもらえると、がんばった甲斐があるよ。",
                    "きみと一緒なら、どこまでも歩けそう。", 2, 1,
                ),
            ),
        )
        context.exerciseMinutesToday >= 20 -> DialogueTopic(
            "activity_exercise",
            "今日はしっかり体を動かしたね、$NAME。ぼくにも力が湧いてくるよ。",
            listOf(
                DialogueChoice(
                    "ex_sweat", "いい汗かいたね",
                    "うん!体を動かすと、気持ちまで晴れるね。",
                    "$NAME のがんばり、ちゃんと届いてるよ。", 2, 1,
                ),
                DialogueChoice(
                    "ex_together", "きみも一緒に動いた気分?",
                    "そうそう!きみが動くと、ぼくも元気になるんだ。",
                    "また一緒に、体を動かそうね。", 1, 1,
                ),
            ),
        )
        else -> null
    }

    // ---- 記録がまだの日のやさしい一言(責めない) ----

    private fun noRecordTopic(): DialogueTopic = DialogueTopic(
        "no_record",
        "$NAME、今日の記録はこれからかな?よかったら、あとで少しだけ教えてね。",
        listOf(
            DialogueChoice(
                "rec_later", "あとで記録するね",
                "うん、ありがとう。無理のない範囲で大丈夫だよ。",
                "きみのペースで、ゆっくりでいいからね。", 1, 1,
            ),
            DialogueChoice(
                "rec_rest", "今日はゆっくりしたよ",
                "それも大事な一日だね。ちゃんと休めたなら何よりだよ。",
                "また元気なときに、聞かせてね。", 1, 1,
            ),
        ),
    )

    // ---- 大会結果の話題 ----

    private fun tournamentTopic(won: Boolean?): DialogueTopic? = when (won) {
        true -> DialogueTopic(
            "tournament_win",
            "この前の大会、優勝したんだよね。$NAME のおかげだよ!",
            listOf(
                DialogueChoice(
                    "win_together", "いっしょに勝ち取ったね",
                    "うん!ぼくたちの力を合わせた結果だね。",
                    "この喜び、ずっと忘れないよ。", 3, 2,
                ),
                DialogueChoice(
                    "win_praise", "きみがすごかったよ",
                    "えへへ、$NAME にそう言ってもらえて、うれしいなあ。",
                    "次も、いっしょにがんばろうね!", 2, 1,
                ),
            ),
        )
        false -> DialogueTopic(
            "tournament_lose",
            "大会は残念だったけど、$NAME と一緒なら、また強くなれるよ。",
            listOf(
                DialogueChoice(
                    "lose_again", "また挑戦しようね",
                    "うん!今日の悔しさは、次への力に変えていこう。",
                    "きみとなら、あきらめずにいられるよ。", 2, 1,
                ),
                DialogueChoice(
                    "lose_effort", "よくがんばったよ",
                    "ありがとう…。そう言ってもらえると、救われるよ。",
                    "また一歩ずつ、強くなっていこうね。", 1, 1,
                ),
            ),
        )
        null -> null
    }

    // ---- 最終形態でシーズン終盤の感謝 ----

    private fun finalFormTopic(): DialogueTopic = DialogueTopic(
        "final_form",
        "この姿になれたのは、$NAME のおかげ。残りの日々も、いっしょに楽しもうね。",
        listOf(
            DialogueChoice(
                "final_grown", "立派になったね",
                "えへへ、全部きみと過ごした毎日のおかげだよ。",
                "この時間を、大切にしたいな。", 2, 1,
            ),
            DialogueChoice(
                "final_thanks", "ここまでありがとう",
                "こちらこそ、ずっとそばにいてくれてありがとう。",
                "最後まで、いっしょに歩こうね、$NAME。", 2, 2,
            ),
        ),
    )

    // ---- いつでも成立する一般的な話題(話題の厚みと変化を確保) ----

    private fun generalTopics(): List<DialogueTopic> = listOf(
        DialogueTopic(
            "general_time",
            "$NAME、こうしてきみと過ごす時間、ぼくは好きだな。",
            listOf(
                DialogueChoice(
                    "gen_like", "私も好きだよ",
                    "ふふ、うれしいなあ。同じ気持ちで、ほっとするよ。",
                    "これからも、たくさん話そうね。", 2, 1,
                ),
                DialogueChoice(
                    "gen_tomorrow", "また明日も話そう",
                    "うん、やくそくだね。明日も待ってるよ。",
                    "きみと話すの、毎日の楽しみなんだ。", 1, 1,
                ),
            ),
        ),
        DialogueTopic(
            "general_bond",
            "ねえ $NAME、ぼくたち、少しずつ仲良くなってる気がするよ。",
            listOf(
                DialogueChoice(
                    "bond_yes", "そうだね",
                    "だよね!一緒にいる時間が、ぼくの宝物だよ。",
                    "これからも、よろしくね。", 2, 1,
                ),
                DialogueChoice(
                    "bond_future", "これからもよろしく",
                    "うん、こちらこそ!ずっと相棒でいてね。",
                    "$NAME となら、なんだか心強いよ。", 2, 1,
                ),
            ),
        ),
    )

    // ---- 性格ごとの専用話題(2つずつ)。責めない・命令しない・医療的断定をしない文体を守る。
    // トーン: がんばりや=前向き熱血、のんびり=ゆったり、クール=そっけないが優しい、
    // あまえんぼう=甘えん坊、きまぐれ=気分屋。 ----

    private fun personalityTopics(personality: Personality): List<DialogueTopic> = when (personality) {
        Personality.HARDWORKER -> listOf(
            DialogueTopic(
                "hard_step",
                "$NAME、ぼく決めたんだ!今日もいっしょに一歩ずつ前へ進もう!やる気まんたんだよ!",
                listOf(
                    DialogueChoice(
                        "hard_go", "その意気だ!",
                        "でしょ!$NAME とならなんだってがんばれる気がする!",
                        "さあ、今日も全力でいこう!", 2, 1,
                    ),
                    DialogueChoice(
                        "hard_care", "無理はしないでね",
                        "うん、わかってる。全力でも、ちゃんと自分を大事にするよ。",
                        "きみが見ててくれるから、がんばれるんだ。", 1, 1,
                    ),
                ),
            ),
            DialogueTopic(
                "hard_goal",
                "$NAME、目標に向かって進むのって、わくわくするよね!ぼく、そういうの大好きなんだ!",
                listOf(
                    DialogueChoice(
                        "hard_aim", "いっしょに目指そう",
                        "うん!一つずつクリアしていこうね、$NAME!",
                        "きみとなら、どんな目標も楽しみだよ。", 2, 1,
                    ),
                    DialogueChoice(
                        "hard_steady", "焦らずいこうね",
                        "そうだね!急がば回れ、だ。しっかり前へ進もう。",
                        "一歩ずつでも、確実にね!", 1, 1,
                    ),
                ),
            ),
        )
        Personality.EASYGOING -> listOf(
            DialogueTopic(
                "easy_slow",
                "$NAME、今日ものんびりいこうね。焦らなくて、大丈夫だよ〜。",
                listOf(
                    DialogueChoice(
                        "easy_yes", "そうだね、ゆっくりね",
                        "うんうん、それがいちばん。ゆるゆるが心地いいよ〜。",
                        "$NAME と過ごす時間、ぽかぽかするなあ。", 2, 1,
                    ),
                    DialogueChoice(
                        "easy_heal", "きみは癒されるなあ",
                        "ふふ、そう言ってもらえると、ぼくもうれしいよ〜。",
                        "また一緒に、のんびりしようね。", 1, 1,
                    ),
                ),
            ),
            DialogueTopic(
                "easy_warm",
                "ふわぁ…$NAME のそばは、あったかくて、つい和んじゃうなあ。",
                listOf(
                    DialogueChoice(
                        "easy_relax", "ゆっくりしていいよ",
                        "ありがとう〜。じゃあ、お言葉に甘えてのんびりするね。",
                        "こういう時間が、いちばん好きだなあ。", 2, 1,
                    ),
                    DialogueChoice(
                        "easy_same", "私もほっとするよ",
                        "えへへ、おそろいだね。一緒だと、もっと和むよ〜。",
                        "ずっとこのまま、ゆるりといたいなあ。", 2, 1,
                    ),
                ),
            ),
        )
        Personality.COOL -> listOf(
            DialogueTopic(
                "cool_wait",
                "$NAME か。…別に、待ってたわけじゃないけど。まあ、来てくれてよかった。",
                listOf(
                    DialogueChoice(
                        "cool_visit", "会いに来たよ",
                        "…そうか。まあ、悪い気はしないよ。ありがとな。",
                        "その、また来てくれると、うれしい。", 2, 1,
                    ),
                    DialogueChoice(
                        "cool_how", "元気だった?",
                        "ふん、見ての通りだ。…心配してくれたのか?",
                        "きみも、体を大事にな。", 1, 1,
                    ),
                ),
            ),
            DialogueTopic(
                "cool_watch",
                "ふん、$NAME。無理はするなよ。…ぼくが、見ててやるからさ。",
                listOf(
                    DialogueChoice(
                        "cool_thanks", "ありがとう、心強いよ",
                        "…礼を言われるほどのことじゃない。当然のことだ。",
                        "まあ、頼ってくれても、いいけどな。", 2, 1,
                    ),
                    DialogueChoice(
                        "cool_return", "きみも無理しないでね",
                        "…ふん。きみに言われるとはな。…わかったよ。",
                        "お互いさま、ってことにしておくよ。", 1, 1,
                    ),
                ),
            ),
        )
        Personality.AFFECTIONATE -> listOf(
            DialogueTopic(
                "aff_miss",
                "$NAME〜!会いたかったよぉ。ねえねえ、もっとかまって?",
                listOf(
                    DialogueChoice(
                        "aff_spoil", "いっぱいかまうよ",
                        "わーい!やったぁ!$NAME だいすき!",
                        "えへへ、ずっとこうしていたいなあ。", 3, 2,
                    ),
                    DialogueChoice(
                        "aff_wait", "少し待っててね",
                        "うぅ、わかった…でも、あんまり待てないよ〜?",
                        "早く $NAME とくっつきたいなあ。", 1, 1,
                    ),
                ),
            ),
            DialogueTopic(
                "aff_side",
                "えへへ、$NAME のとなりが一番だいすき。ずっといっしょにいたいな。",
                listOf(
                    DialogueChoice(
                        "aff_too", "私もだよ",
                        "ほんとに?わーい、うれしくてとろけちゃう〜!",
                        "ずーっと、いっしょだからね!", 3, 2,
                    ),
                    DialogueChoice(
                        "aff_sweet", "甘えん坊さんだね",
                        "えへへ、だって $NAME のことが大好きだもん!",
                        "いっぱい甘えさせてね〜。", 2, 1,
                    ),
                ),
            ),
        )
        Personality.CAPRICIOUS -> listOf(
            DialogueTopic(
                "cap_play",
                "$NAME、今日はなんだか…そうだ、こっちで遊ぼう!あ、やっぱりあっちがいいかも!",
                listOf(
                    DialogueChoice(
                        "cap_either", "どっちでもいいよ",
                        "え、ほんと?じゃあ…うーん、両方!いや、やっぱり気分で決める!",
                        "きまぐれなぼくに付き合ってくれて、ありがと。", 2, 1,
                    ),
                    DialogueChoice(
                        "cap_you", "きみらしいね",
                        "でしょ?ぼく、自分でも次に何するかわかんないんだ〜。",
                        "でも、$NAME となら何してても楽しいよ。", 2, 1,
                    ),
                ),
            ),
            DialogueTopic(
                "cap_like",
                "きまぐれなぼくだけど、$NAME のことは…まあ、気に入ってるんだよね、今のところ。",
                listOf(
                    DialogueChoice(
                        "cap_honor", "光栄だな",
                        "ふふ、素直じゃないぼくにしては、大サービスだよ?",
                        "気が向いたら、また言ってあげる〜。", 2, 1,
                    ),
                    DialogueChoice(
                        "cap_ok", "気まぐれも好きだよ",
                        "お、$NAME わかってるね!そういうとこ、好きだよ。",
                        "明日の気分は、明日にならないとわからないけどね!", 2, 1,
                    ),
                ),
            ),
        )
    }

    /** テンプレートの {name} をトレーナー名(未設定なら「トレーナー」)へ置き換える。 */
    fun render(template: String, trainerName: String?): String =
        template.replace(NAME, trainerName?.takeIf { it.isNotBlank() } ?: FALLBACK_TRAINER_NAME)
}
