# VitaMorph Claude Code Instructions

このリポジトリでは、Claude Codeが全実装を担当する(2026-07-12ユーザー決定。旧Codex担当分を引き取り)。
残作業は `docs/COMPLETION_PLAN.md` で管理する。新規画像アセットの生成は行わず、既存画像の範囲で実装する。

## 作業開始時に読むもの

1. `README.md`
2. `docs/HANDOFF_STATUS.md`
3. `docs/IMPLEMENTATION_PLAN.md`
4. `docs/DATA_MODEL.md`
5. `docs/MONSTER_ROSTER.md`
6. `docs/MONSTER_ASSET_CONTRACT.md`

仕様が矛盾する場合は、実装を始める前にユーザーへ確認する。推測でキャラクターIDや進化ルートを確定しない。

## Claudeの担当範囲

- リポジトリ全体(2026-07-12より。`MonsterArtwork.kt` のモーション実装、ロスター文書の構成記述を含む)
- Room、Health Connect、食事記録、会話、機嫌、絆、ミニゲーム、バトル、世代継承、図鑑

## アセットに関する不変ルール(旧Codex契約から継続)

- 公開済み `formId` はセーブデータキーであり、変更・削除・改名しない。
- 既存モンスター画像(`art/**`、`app/src/main/res/drawable-nodpi/monster_*`)の上書き・削除・色変換をしない(図鑑のシルエット表示など描画時の演出は可)。
- `docs/MONSTER_ROSTER.md` に存在しないキャラクターIDをセーブデータへ保存しない。
- キャラクター名・外見仕様の変更はユーザー確認を経る。

## 開発ルール

- Kotlin、Jetpack Compose、Androidの既存構成を維持する。
- 既存ユーザーのSharedPreferencesデータを壊さない。データ構造変更には移行処理とテストを追加する。
- 健康・栄養データを外部へ送信しない。外部通信を追加する場合は目的、送信項目、保存期間をユーザーへ先に説明する。
- Health Connectの栄養記録はData OriginとClient Record IDを使い、あすけんとの二重計上を避ける。
- 食事評価や会話でユーザーを責めたり、医療診断のような断定をしたりしない。
- オス・メスは能力差を持たず、進化先と外見だけを分ける。
- 女性人型は明確な成人型として設計される。幼生、子供に見える形態、動物型を性的に扱わない。
- 機嫌によるバトル補正は原則として最大プラスマイナス5%に収める。
- 世代継承には上限と逓減を設け、長期プレイで無制限に能力が膨張しないようにする。
- 秘密鍵、パスワード、トークン、署名ファイルをコミットしない。
- 無関係なリファクタリングを同じ変更へ混ぜない。

## 推奨ワークフロー

- `main` 上で直接作業し、マイルストーンごとに検証してプッシュする(2026-07-12ユーザー決定)。
- マイルストーン単位で小さくコミットする。
- 作業前後に `git status` と差分を確認する。
- 完了時は変更内容、テスト結果、未解決事項を `docs/HANDOFF_STATUS.md` に反映する。

## 検証コマンド

```bash
./gradlew test assembleDebug lintDebug
```

生成APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

