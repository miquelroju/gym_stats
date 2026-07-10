package com.myapp.gymstats.ui.features.session

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun RestTimerCard(
    remainingSeconds: Int,
    totalSeconds: Int,
    isRunning: Boolean,
    onStart: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(remainingSeconds, isRunning) {
        if (remainingSeconds == 0 && !isRunning) {
            // se dispara solo cuando termina de forma natural, no al cancelar manualmente
        }
    }
    LaunchedEffect(isRunning) {
        if (!isRunning && remainingSeconds == 0) {
            vibrate(context)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRunning)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Timer, contentDescription = null)
                Column {
                    Text(
                        text = if (isRunning) formatTime(remainingSeconds)
                        else "Descanso: ${formatTime(totalSeconds)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (isRunning) {
                        LinearProgressIndicator(
                            progress = { remainingSeconds.toFloat() / totalSeconds.toFloat() },
                            modifier = Modifier
                                .width(120.dp)
                                .padding(top = 4.dp)
                        )
                    }
                }
            }

            if (isRunning) {
                IconButton(onClick = onCancel) {
                    Icon(Icons.Default.Close, contentDescription = "Cancelar")
                }
            } else {
                Button(onClick = onStart) {
                    Text("Iniciar descanso")
                }
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%d:%02d", minutes, secs)
}

private fun vibrate(context: Context) {
    val vibrator = context.getSystemService(Vibrator::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator?.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator?.vibrate(400)
    }
}

















