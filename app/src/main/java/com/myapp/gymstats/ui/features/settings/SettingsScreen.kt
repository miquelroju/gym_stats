package com.myapp.gymstats.ui.features.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

val AVATAR_OPTIONS = listOf(
    "💪", "🔥", "🦍", "🏋️", "⚡", "🥇", "🐉", "🦁",
    "🚀", "🎯", "🧠", "🥊", "🏆", "💎", "🌊", "🐺"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userId: String,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAvatarPicker by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        if (userId.isNotBlank()) viewModel.load(userId)
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            kotlinx.coroutines.delay(1500)
            viewModel.onSavedDismissed()
        }
    }

    if (showAvatarPicker) {
        AlertDialog(
            onDismissRequest = { showAvatarPicker = false },
            title = { Text("Elige tu avatar") },
            text = {
                LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.height(220.dp)) {
                    items(AVATAR_OPTIONS) { emoji ->
                        TextButton(onClick = {
                            viewModel.updateAvatarEmoji(emoji)
                            showAvatarPicker = false
                        }) {
                            Text(emoji, style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAvatarPicker = false }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // --- Perfil --------------------------------------------------------
            Text("Perfil", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilledTonalButton(
                    onClick = { showAvatarPicker = true },
                    modifier = Modifier.size(64.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(uiState.avatarEmoji, style = MaterialTheme.typography.headlineMedium)
                }
                OutlinedTextField(
                    value = uiState.username,
                    onValueChange = { viewModel.updateUsername(it) },
                    label = { Text("Nombre de usuario") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { viewModel.saveProfile() },
                enabled = uiState.username.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (uiState.profileSaved)  "✅ Perfil guardado" else "Guardar perfil")
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            // --- Temporizador de descanso --------------------------------------
            Text("Temporizador de descanso", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "${uiState.settings.restTimerSeconds} segundos",
                style = MaterialTheme.typography.bodyLarge
            )
            Slider(
                value = uiState.settings.restTimerSeconds.toFloat(),
                onValueChange = { viewModel.updateRestTimer(it.toInt()) },
                valueRange = 15f..300f,
                steps = 18
            )

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            // --- Racha ---------------------------------------------------------
            Text("Racha de entrenamiento", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Define cada cuántos días esperas entrenar y cuánto margen te das",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("Entreno cada ${uiState.settings.expectedGapDays} día(s)")
            Slider(
                value = uiState.settings.expectedGapDays.toFloat(),
                onValueChange = { viewModel.updateExpectedGap(it.toInt()) },
                valueRange = 1f..7f,
                steps = 5
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text("Margent extra: ${uiState.settings.graceDays} día(s)")
            Slider(
                value = uiState.settings.graceDays.toFloat(),
                onValueChange = { viewModel.updateGraceDays(it.toInt()) },
                valueRange = 0f..7f,
                steps = 6
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Ejemplo: puedes estar hasta " +
                    "${uiState.settings.expectedGapDays + uiState.settings.graceDays} días sin entrenar " +
                    "sin perder la racha",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            // --- Notificaciones ---------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Notificaciones sociales", style = MaterialTheme.typography.titleMedium)
                Switch(
                    checked = uiState.settings.notificationsEnabled,
                    onCheckedChange = { viewModel.toggleNotifications(it) }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { viewModel.save() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (uiState.isSaved) "✅ Guardado" else "Guardar configuración")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}