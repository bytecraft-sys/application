package com.yourapp.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.example.application.ui.theme.ApplicationTheme
import kotlin.math.min

@Composable
fun AuraCircle(
    state: AuraState,
    amplitude: Float = 0f,
    modifier: Modifier,
) {
    val transition = rememberInfiniteTransition(label = "Aura breathing")
    val breathingScale by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = BREATHING_DURATION_MS,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "Aura scale",
    )
    val breathingAlpha by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = BREATHING_DURATION_MS,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "Aura alpha",
    )

    val normalizedAmplitude = amplitude.coerceIn(0f, 1f)
    val scale = when (state) {
        AuraState.Breathing -> breathingScale
        AuraState.Listening -> lerp(0.95f, 1.2f, normalizedAmplitude)
    }
    val alpha = when (state) {
        AuraState.Breathing -> breathingAlpha
        AuraState.Listening -> lerp(0.5f, 1f, normalizedAmplitude)
    }

    Canvas(modifier = modifier) {
        val maxRadius = min(size.width, size.height) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        // Outer glow layer
        drawCircle(
            color = GlowColor.copy(alpha = GlowColor.alpha * alpha * 0.4f),
            radius = maxRadius * scale * 1.15f,
            center = center,
        )
        
        // Main glow layer
        drawCircle(
            color = GlowColor.copy(alpha = GlowColor.alpha * alpha),
            radius = maxRadius * scale,
            center = center,
        )
        
        // Core layer
        drawCircle(
            color = CoreColor,
            radius = maxRadius * CORE_RADIUS_RATIO * scale,
            center = center,
        )
    }
}

private val CoreColor = Color(0xFF1DB954)
private val GlowColor = CoreColor.copy(alpha = 0.3f)
private const val BREATHING_DURATION_MS = 2200
private const val CORE_RADIUS_RATIO = 0.58f

@Preview(showBackground = true)
@Composable
fun AuraCircleBreathingPreview() {
    ApplicationTheme {
        AuraCircle(
            state = AuraState.Breathing,
            modifier = Modifier.size(220.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AuraCircleListeningPreview() {
    ApplicationTheme {
        AuraCircle(
            state = AuraState.Listening,
            amplitude = 0.8f,
            modifier = Modifier.size(220.dp),
        )
    }
}
