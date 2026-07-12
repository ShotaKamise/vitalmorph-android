# VitaMorph Claude Code Instructions

このリポジトリでは、Claude Codeはゲームロジック、データ保存、Health Connect、食事管理、交流機能、ミニゲーム、画面統合を担当する。
モンスターデザイン、画像生成、画像アセット、キャラクターアニメーション内部実装はCodex担当である。

## 作業開始時に読むもの

1. `README.md`
2. `docs/HANDOFF_STATUS.md`
3. `docs/IMPLEMENTATION_PLAN.md`
4. `docs/DATA_MODEL.md`
5. `docs/MONSTER_ROSTER.md`
6. `docs/MONSTER_ASSET_CONTRACT.md`

仕様が矛盾する場合は、実装を始める前にユーザーへ確認する。推測でキャラクターIDや進化ルートを確定しない。

## Claudeの担当範囲

- `app/src/main/java/app/vitalmorph/domain/**`
- `app/src/main/java/app/vitalmorph/data/**`
- `app/src/main/java/app/vitalmorph/ui/**` のうち、下記のCodex専有ファイルを除く部分
- `app/src/test/**`
- Room、Health Connect、食事記録、会話、機嫌、絆、ミニゲーム、バトル、世代継承
- 必要なAndroidリソース。ただしモンスター画像は除く

## Codex専有範囲

次のファイルは、ユーザーから明示的な許可がない限り変更・削除・再生成しない。

- `art/**`
- `app/src/main/res/drawable-nodpi/monster_*`
- `app/src/main/java/app/vitalmorph/ui/MonsterArtwork.kt`
- `docs/MONSTER_ROSTER.md` の承認済みキャラクター名、ID、外見仕様
- `docs/MONSTER_ASSET_CONTRACT.md` の公開API仕様

Claudeは `MonsterVisual`、`MonsterSprite`、`MonsterMotion` の公開APIを呼び出して画面へ統合できる。必要なモーションが存在しない場合は、勝手に代替アニメーションを実装せず、`docs/HANDOFF_STATUS.md` の「Codexへの依頼」に追記する。

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

- Claude用ブランチ `claude/core-implementation` で作業する。
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

