package app.vitalmorph

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.health.connect.client.PermissionController
import app.vitalmorph.data.HealthConnectRepository
import app.vitalmorph.ui.GameViewModel
import app.vitalmorph.ui.VitaMorphApp

class MainActivity : ComponentActivity() {
    private val viewModel: GameViewModel by viewModels()
    private val permissionLauncher = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract(),
    ) { granted ->
        viewModel.onPermissionResult(granted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VitaMorphApp(
                viewModel = viewModel,
                onRequestHealthPermissions = { permissionLauncher.launch(HealthConnectRepository.permissions) },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }
}
