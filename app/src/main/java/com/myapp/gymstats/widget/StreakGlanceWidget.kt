package com.myapp.gymstats.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import com.myapp.gymstats.MainActivity

class StreakGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: android.content.Context, id: GlanceId) {
        val repository = WidgetEntryPoint.getRepository(context)
        val userId = WidgetEntryPoint.getCurrentUserId(context)

        val state = if (userId.isNotBlank()) {
            try {
                val streak = repository.getUserStreak(userId)
                val checkedIn = repository.hasCheckedInToday(userId)
                StreakWidgetState(streak = streak, checkedInToday = checkedIn, isLoading = false)
            } catch (e: Exception) {
                StreakWidgetState(isLoading = false)
            }
        } else {
            StreakWidgetState(isLoading = false)
        }

        provideContent {
            StreakWidgetContent(state)
        }
    }
}

@Composable
fun StreakWidgetContent(state: StreakWidgetState) {
    val bgColor = if (state.checkedInToday)
        Color(0xFF01696F)
    else
        Color(0xFF2A2A2A)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(bgColor)
            .padding(12.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🔥",
            style = TextStyle(fontSize = 28.sp)
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = "${state.streak}",
            style = TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = ColorProvider(Color.White)
            )
        )
        Text(
            text = if (state.streak == 1) "día" else "días",
            style = TextStyle(fontSize = 12.sp, color = ColorProvider(Color.White))
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = if (state.checkedInToday) "✅ Hoy hecho" else "Toca para entrenar",
            style = TextStyle(fontSize = 11.sp, color = ColorProvider(Color.White))
        )
    }
}