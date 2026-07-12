# VitaMorph 完成(v1.0)までの作業計画

2026-07-12策定。以降の全作業はClaudeが担当する(ユーザー決定。旧Codex担当分を引き取り)。
計画・方針の更新はメインセッション(Fable)、実装はOpusサブエージェントが行う。

## 運用ルール

- タスクは上から順に1ループ1タスクで実施する。
- 各タスクの完了条件: 単体テスト追加、`./gradlew test assembleDebug lintDebug` 成功、mainへプッシュ、CI成功、本書のステータス更新。
- 既存セーブデータ(SharedPreferences・Room)を壊さない。スキーマ変更は必ずマイグレーションを付ける。
- 公開済みformIdの変更・削除は禁止(従来どおり)。
- 画像アセットの新規生成は行わない。既存画像の範囲で実装する。

## タスク一覧

### T1: タッチ・会話・ミニゲーム用モーション6種の実装 — [ ] 未着手

旧Codex依頼分を引き取り。`MonsterMotion` へ `TOUCH_HAPPY` / `TOUCH_SHY` / `TOUCH_ANNOYED` / `TALK` / `SAD` / `MINIGAME_SUCCESS` を追加し、既存4種と同様の変形アニメーション(scale/translation/rotation/alphaのキーフレーム)として `MonsterArtwork.kt` に実装する。

- TOUCH_HAPPY: 小さく2回跳ねる+わずかに拡大
- TOUCH_SHY: 縮こまって左右に小さく揺れる
- TOUCH_ANNOYED: 素早く首を振るような左右回転+後退
- TALK: 前傾しつつ上下に小刻みに揺れる(会話カード表示中)
- SAD: 下方向に沈み、傾く(機嫌BAD帯のアイドル代替)
- MINIGAME_SUCCESS: 大きく1回跳ねて回転
- UI側の代用マッピング(VICTORY/HIT/IDLE)を新モーションへ差し替える: MonsterHero(タッチ3種+機嫌BAD時SAD)、DialogueCard表示中TALK、ミニゲーム結果でMINIGAME_SUCCESS。
- docs/MONSTER_ASSET_CONTRACT.md の「追加予定モーション」を実装済みへ更新。

### T2: 性格システム — [ ] 未着手

孵化時に5性格(がんばりや/のんびり/クール/あまえんぼう/きまぐれ)を抽選し世代へ永続化(Room v7)。能力差は付けない(規約)。

- `Personality` enum + `MonsterGeneration.personality`。移行は既存世代へ決定的割り当て(seasonStart+ソルト)。
- DialogueEngine: 性格ごとの語尾・追加テンプレート(各性格2文以上)。DialogueContextへpersonality追加。
- InteractionEngine: 性格で連続タッチ許容回数を±2変化(あまえんぼう+2、クール-2)。報酬上限は変えない。
- ホーム画面に性格表示、系譜カードにも表示。

### T3: 大会進行状態の永続化 — [ ] 未着手

既知の制約「アプリ終了で試合途中から再開できない」を解消する。

- `TurnBattleState` をJSONへシリアライズしSharedPreferencesへ保存(org.json使用、Roomは使わない)。
- 保存タイミング: 各アクション後。復元: refresh時に同一シーズン中なら battle へ復元。クリア: 大会終了・シーズン完了・リセット時。
- movesはformIdから再構築、itemsは残数を保存。シリアライズ往復の単体テスト必須。

### T4: 図鑑 — [ ] 未着手

全71体の図鑑。出会った(その形態になった)フォームを発見済みとして永続化(Room v8: discovered_form)。

- 記録タイミング: refreshで現在формが確定したとき、およびシーズン完了時のfinalFormId。
- 過去データの遡及: 系譜(finalFormId)から初回移行時に発見済みへ取り込む。
- UI: トレーナータブに「図鑑 N / 71」セクション。発見済みは画像+名前、未発見はシルエット(黒塗り)+「???」。性別割り振り(EvolutionEngine.animalFormSex)と系統・ルート別のグルーピング表示。

### T5: レシピ材料の個別保存 — [ ] 未着手

レシピを材料付きで保存・閲覧・削除できるようにする(Room v9: recipe / recipe_item)。

- 既存の「合計を自作食品として保存」も維持(レシピ保存時に自作食品も同時生成)。
- レシピ一覧から1タップで今日の食事へ記録(材料合計)。

### T6: ドキュメント整備とロスター更新 — [ ] 未着手

- docs/MONSTER_ROSTER.md を71体構成(EVOLUTION_TABLE.md)へ更新(ID・名前・外見仕様は不変)。
- README.md を現機能一覧へ更新。
- 幼生・成長体の性別差分は「共通画像+♂/♀マークを正式仕様」と明記(画像生成は行わないため)。
- docs/HANDOFF_STATUS.md のCodex依頼欄を「Claude引き取り済み」へ整理。

### T7: v1.0.0リリース準備 — [ ] 未着手

- バージョン 1.0.0 / versionCode 12 へバンプ。
- 全タスクの完了確認、デモモードでの4週進化サイクル確認手順をREADMEへ記載。
- タグ付け(署名済みAPK公開)はユーザー操作のため、手順の案内のみ。

## 対象外(ユーザー要望があれば再検討)

- 幼生・成長体の性別差分「画像」(画像生成が必要なため。マーク表示を正式仕様とする)
- 日本食品標準成分表の全量(約2,500品目)同梱
- 新規キャラクター・新規画像の追加
