package com.myapp.gymstats.ui.features.update

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun UpdateDialog(viewModel: UpdateViewModel = hiltViewModel()) {
    val updateInfo by viewModel.updateInfo.collectAsState()
    val isDownloading by viewModel.isDownloading.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.checkForUpdate() }
    updateInfo?.let { info ->
        if (!info.hasUpdate) return

        AlertDialog(
            onDismissRequest = { viewModel.dismiss() },
            title = { Text("Nueva versión disponible") },
            text = {
                Text("Versión ${info.latestVersion}\n\n${info.releaseNotes}")
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.downloadAndInstall(context, info.downloadUrl) },
                    enabled = !isDownloading
                ) {
                    if (isDownloading)
                        CircularProgressIndicator()
                    else
                        Text("Actualizar ahora")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismiss() }) {
                    Text("Más tarde")
                }
            }
        )
    }
}