package com.mohammed.xvrviewer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mohammed.xvrviewer.ui.*
import com.mohammed.xvrviewer.viewmodel.AppViewModelFactory
import com.mohammed.xvrviewer.viewmodel.CameraViewModel
import com.mohammed.xvrviewer.viewmodel.RecordingViewModel
import java.net.URLDecoder
import java.net.URLEncoder

@Composable
fun XvrNavHost(repository: com.mohammed.xvrviewer.data.CameraRepository) {
    val navController = rememberNavController()
    val factory = AppViewModelFactory(repository)
    val cameraViewModel: CameraViewModel = viewModel(factory = factory)
    val recordingViewModel: RecordingViewModel = viewModel(factory = factory)

    val cameras by cameraViewModel.cameras.collectAsState(initial = emptyList())
    val recordings by recordingViewModel.recordings.collectAsState(initial = emptyList())

    NavHost(navController = navController, startDestination = "cameras") {
        composable("cameras") {
            CameraListScreen(
                cameras = cameras,
                onAddCamera = { navController.navigate("add_camera") },
                onOpenCamera = { camera -> navController.navigate("live/${camera.id}") },
                onOpenRecordings = { navController.navigate("recordings") }
            )
        }
        composable("add_camera") {
            AddCameraScreen(
                onBack = { navController.popBackStack() },
                onSave = { camera ->
                    cameraViewModel.addCamera(camera) {
                        navController.popBackStack()
                    }
                }
            )
        }
        composable("live/{cameraId}") { backStackEntry ->
            val cameraId = backStackEntry.arguments?.getString("cameraId")?.toLongOrNull()
            val camera = cameras.find { it.id == cameraId }
            camera?.let {
                LiveViewScreen(camera = it, onBack = { navController.popBackStack() })
            }
        }
        composable("recordings") {
            RecordingsScreen(
                recordings = recordings,
                onBack = { navController.popBackStack() },
                onPlay = { rec ->
                    val encoded = URLEncoder.encode(rec.filePath, "UTF-8")
                    navController.navigate("playback/$encoded")
                },
                onDelete = { rec -> recordingViewModel.deleteRecording(rec) }
            )
        }
        composable("playback/{path}") { backStackEntry ->
            val encoded = backStackEntry.arguments?.getString("path") ?: ""
            val path = URLDecoder.decode(encoded, "UTF-8")
            PlaybackScreen(filePath = path, onBack = { navController.popBackStack() })
        }
    }
}
