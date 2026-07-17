package com.myapp.gymstats.ui.features.session

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.myapp.gymstats.domain.model.Exercise
import com.myapp.gymstats.domain.model.WorkoutSet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(
    userId: String,
    sessionId: String = "",
    onSessionSaved: () -> Unit,
    viewModel: SessionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var selectedExercise by remember { mutableStateOf<Exercise?>(null) }
    var reps by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        if (userId.isNotBlank()) viewModel.loadRestTimerSetting(userId)
    }

    LaunchedEffect(sessionId) {
        if (sessionId.isNotBlank()) viewModel.loadSession(sessionId)
    }

    LaunchedEffect(uiState.existingNotes) {
        if (uiState.existingNotes.isNotBlank()) notes = uiState.existingNotes
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onSessionSaved()
    }

    uiState.motivationMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { viewModel.onMotivationDismissed() },
            title = { Text("\uD83C\uDFC6 ¡Nuevo récord!") },
            text = { Text(message) },
            confirmButton = {
                Button(onClick = { viewModel.onMotivationDismissed() }) {
                    Text("¡A por más!")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(if (uiState.isEditMode) "Editar sesión" else "Nueva sesión") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Ejercicio", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(4.dp))

                if (uiState.isSyncingExercises) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            "Cargando ejercicios...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedExercise?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Selecciona ejercicio") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(dropdownExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            uiState.exercises.forEach { exercise ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(exercise.name)
                                            Text(
                                                exercise.muscleGroup,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedExercise = exercise
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = reps,
                        onValueChange = { reps = it },
                        label = { Text("Reps") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it },
                        label = { Text("Kg") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        val exercise = selectedExercise ?: return@Button
                        val r = reps.toIntOrNull() ?: return@Button
                        val w = weight.toFloatOrNull() ?: return@Button
                        viewModel.addSet(exercise.id, exercise.name, r, w)
                        viewModel.startRestTimer()
                        reps = ""
                        weight = ""
                    },
                    enabled = !uiState.isSyncingExercises
                            && selectedExercise != null
                            && reps.isNotBlank()
                            && weight.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Añadir serie")
                }
            }

            if (uiState.isTimerRunning || uiState.currentSets.isNotEmpty()) {
                item {
                    RestTimerCard(
                        remainingSeconds = uiState.timerRemaining,
                        totalSeconds = uiState.restTimerSeconds,
                        isRunning = uiState.isTimerRunning,
                        onStart = { viewModel.startRestTimer() },
                        onCancel = { viewModel.cancelRestTimer() }
                    )
                }
            }

            if (uiState.currentSets.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Series añadidas", style = MaterialTheme.typography.titleSmall)
                }

                items(uiState.currentSets) { set ->
                    SetRow(set = set, onDelete = { viewModel.removeSet(set.id) })
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notas (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }

                item {
                    Button(
                        onClick = {
                            if (uiState.isEditMode) viewModel.updateSession(userId, notes)
                            else viewModel.saveSession(userId, notes)
                        },
                        enabled = !uiState.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(if (uiState.isEditMode) "Actualizar sesión" else "Guardar sesión")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun SetRow(set: WorkoutSet, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(set.exerciseName, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Serie ${set.setNumber} · ${set.reps} reps · ${set.weightKg} kg",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Eliminar serie",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}