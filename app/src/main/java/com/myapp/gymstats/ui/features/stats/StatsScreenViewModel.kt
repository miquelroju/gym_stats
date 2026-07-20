package com.myapp.gymstats.ui.features.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    userId: String,
    onBack: () -> Unit,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(userId) {
        if (userId.isNotBlank()) viewModel.load(userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis estadísticas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }
            uiState.bestsByMuscleGroup.isEmpty() -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Entrena para ver tus estadísticas",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // Resumen rápido
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "${uiState.totalSessions}",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        "sesiones",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "${uiState.totalSets}",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        "series totales",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }

                    // Tabs grupos musculares
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.bestsByMuscleGroup.keys.toList()) { group ->
                                FilterChip(
                                    selected = group == uiState.selectedGroup,
                                    onClick = { viewModel.selectGroup(group) },
                                    label = { Text(group) }
                                )
                            }
                        }
                        HorizontalDivider()
                    }

                    // Cards de ejercicios con gráfica desplegable
                    val bests = uiState.bestsByMuscleGroup[uiState.selectedGroup] ?: emptyList()
                    items(bests.sortedByDescending { it.bestScore }) { best ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            onClick = {
                                if (uiState.selectedExerciseId == best.exercise.id)
                                    viewModel.clearProgress()
                                else
                                    viewModel.loadProgress(best.exercise.id)
                            }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(best.exercise.name, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "${best.bestKg} kg × ${best.bestReps} reps",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "Score: ${String.format("%.1f", best.bestScore)} pts",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                if (uiState.selectedExerciseId == best.exercise.id) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        "Evolución del score",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    ProgressChart(uiState.progressPoints)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProgressChart(points: List<ProgressPoint>) {
    if (points.size < 2) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Necesitas al menos 2 sesiones para ver el progreso",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val minScore = points.minOf { it.score }
    val maxScore = points.maxOf { it.score }
    val range = (maxScore - minScore).coerceAtLeast(1.0)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        val stepX = size.width / (points.size - 1).toFloat()
        val path = Path()

        points.forEachIndexed { index, point ->
            val x = index * stepX
            val y = size.height - ((point.score - minScore) / range * size.height).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(path, color = primaryColor, style = Stroke(width = 4f))

        points.forEachIndexed { index, point ->
            val x = index * stepX
            val y = size.height - ((point.score - minScore) / range * size.height).toFloat()
            drawCircle(color = primaryColor, radius = 7f, center = Offset(x, y))
        }
    }

    // Etiquetas eje X
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        points.forEach { point ->
            Text(
                point.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}