package app.vitalmorph

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.vitalmorph.ui.VitaMorphTheme

class PermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PrivacyPolicyScreen() }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun PrivacyPolicyScreen() {
    VitaMorphTheme {
        Scaffold(
            topBar = { TopAppBar(title = { Text("健康データとプライバシー") }) },
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("VitaMorphは、モンスターの育成と進化判定のために、歩数・運動・消費カロリー・栄養データを読み取ります。", style = MaterialTheme.typography.bodyLarge)
                Text("健康データは端末内で処理され、外部サーバー、広告事業者、第三者へ送信しません。アプリが保存するのは目標値、進化結果、トレーナー経験値などのゲーム情報だけです。")
                Text("権限はAndroidのヘルスコネクト設定からいつでも取り消せます。権限を取り消しても、デモモードでゲームを確認できます。")
                Text("本アプリは医療診断や治療を目的としたものではありません。", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
