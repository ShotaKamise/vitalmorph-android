# VitaMorph（仮）

食事と運動の記録から、28日間でモンスターが進化するAndroid育成ゲームです。

## 現在の機能

- Android Health Connectから歩数、活動カロリー、運動時間、栄養を読み取り
- あすけんのカロリー・PFC目標を初回設定
- 28日を1シーズンとして7日ごとに進化
- 6系統、12中間形態、24最終形態
- 第3週の過ごし方によって最終進化が2種類へ分岐
- 8体参加のCPUオートトーナメント
- 13シーズン（364日）と1,100XPでマスターになる年間ランク
- 実データなしで全サイクルを確認できるデモモード
- ゲーム状態を端末内へ保存

健康データは端末内で進化判定に使い、外部サーバーへ送信しません。本アプリは医療診断・治療を目的とするものではありません。

## データ連携

想定する連携経路は次のとおりです。

```text
あすけん ──────────────┐
                       ├→ Health Connect → VitaMorph
Amazfit T-Rex 3 → Zepp → Google Fit ┘
```

ZeppからGoogle Fit、Google FitからHealth Connectへの同期設定は端末側で行います。Zeppのバージョンや端末によって同期される項目が異なる可能性があります。

## 開発環境

- JDK 17以上
- Android SDK Platform 36
- Android SDK Build Tools 36.0.0
- Gradle 9.4.1
- Android Gradle Plugin 9.2.1

```bash
./gradlew test assembleDebug
```

APKは次に生成されます。

```text
app/build/outputs/apk/debug/app-debug.apk
```

## GitHubからAPKを取得する

`main`へpushするとGitHub ActionsがテストとAPKビルドを行います。Actionsの実行結果にある `vitalmorph-debug-apk` からインストール用APKを取得できます。

個人利用の初期確認ではdebug APKを使えます。ただしGitHub Actionsが毎回作るdebug署名は更新時に変わる可能性があるため、継続利用には署名済みrelease APKを使ってください。

### 継続更新用の署名設定

署名鍵は一度だけ作成し、安全な場所にバックアップします。鍵を失うと、インストール済みアプリを同じアプリとして更新できません。

```bash
keytool -genkeypair -v -keystore vitalmorph-release.jks \
  -alias vitalmorph -keyalg RSA -keysize 4096 -validity 10000
```

GitHubリポジトリの `Settings > Secrets and variables > Actions` に次の4件を登録します。

- `VITALMORPH_KEYSTORE_BASE64`: `base64 < vitalmorph-release.jks | tr -d '\n'` の結果
- `VITALMORPH_STORE_PASSWORD`: キーストアのパスワード
- `VITALMORPH_KEY_ALIAS`: `vitalmorph`
- `VITALMORPH_KEY_PASSWORD`: 鍵のパスワード

その後、タグをpushするとGitHub Releasesへ署名済みAPKが公開されます。

```bash
git tag v0.1.0
git push origin v0.1.0
```

署名鍵やパスワードをGitHubへ直接コミットしないでください。

## 注意

- 公開前にアプリ名、パッケージ名、アイコン、プライバシーポリシーURLを確定してください。
- Google Playへ公開する場合は、Health apps declarationとHealth Connectのデータ型申請が必要です。
- 現時点ではバックグラウンド同期を行わず、アプリを開いたときに同期します。
