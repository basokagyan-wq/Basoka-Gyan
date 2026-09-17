package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.AssistantState
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.PurpleGlow
import kotlin.math.sin

@Composable
fun AssistantOrb(
    state: AssistantState,
    audioRms: Float,
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_anim")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val thinkingAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "thinking_spin"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(size)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
            val baseRadius = size.toPx() * 0.38f

            val dynamicScale = when (state) {
                AssistantState.LISTENING -> 1f + (audioRms * 0.45f)
                AssistantState.SPEAKING -> pulse * 1.15f
                AssistantState.THINKING -> pulse
                else -> pulse
            }

            val currentRadius = baseRadius * dynamicScale

            // Outer soft atmospheric glow
            val outerGlowBrush = Brush.radialGradient(
                colors = listOf(
                    ElectricCyan.copy(alpha = 0.35f),
                    NeonPurple.copy(alpha = 0.15f),
                    Color.Transparent
                ),
                center = center,
                radius = currentRadius * 1.6f
            )
            drawCircle(brush = outerGlowBrush, radius = currentRadius * 1.6f, center = center)

            // Dynamic rings
            when (state) {
                AssistantState.LISTENING -> {
                    // Audio reactive ripples
                    val rippleAlpha = (audioRms * 0.8f).coerceIn(0.2f, 0.9f)
                    drawCircle(
                        color = ElectricCyan.copy(alpha = rippleAlpha),
                        radius = currentRadius * (1.15f + audioRms * 0.3f),
                        center = center,
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                    drawCircle(
                        color = NeonPurple.copy(alpha = rippleAlpha * 0.6f),
                        radius = currentRadius * (1.35f + audioRms * 0.4f),
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
                AssistantState.THINKING -> {
                    // Rotating cyber particle ring
                    rotate(thinkingAngle, pivot = center) {
                        drawCircle(
                            brush = Brush.sweepGradient(
                                colors = listOf(ElectricCyan, NeonPurple, Color.Transparent, ElectricCyan)
                            ),
                            radius = currentRadius * 1.25f,
                            center = center,
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }
                }
                AssistantState.SPEAKING -> {
                    // Sine wave audio ring
                    rotate(rotation, pivot = center) {
                        drawCircle(
                            brush = Brush.sweepGradient(
                                colors = listOf(CyanGlow, PurpleGlow, ElectricCyan)
                            ),
                            radius = currentRadius * 1.2f,
                            center = center,
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }
                }
                else -> {
                    // Idle gentle orbit ring
                    rotate(rotation, pivot = center) {
                        drawCircle(
                            brush = Brush.sweepGradient(
                                colors = listOf(ElectricCyan.copy(alpha = 0.5f), Color.Transparent, NeonPurple.copy(alpha = 0.5f))
                            ),
                            radius = currentRadius * 1.2f,
                            center = center,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }
            }

            // Core sphere gradient
            val coreGradient = Brush.radialGradient(
                colors = when (state) {
                    AssistantState.LISTENING -> listOf(Color.White, ElectricCyan, NeonPurple)
                    AssistantState.THINKING -> listOf(Color.White, NeonPurple, ElectricCyan)
                    AssistantState.SPEAKING -> listOf(Color.White, CyanGlow, NeonPurple)
                    else -> listOf(CyanGlow, ElectricCyan, Color(0xFF0F172A))
                },
                center = center - Offset(currentRadius * 0.25f, currentRadius * 0.25f),
                radius = currentRadius
            )

            drawCircle(brush = coreGradient, radius = currentRadius, center = center)

            // High-tech inner nodes
            for (i in 0 until 4) {
                val angle = Math.toRadians((rotation + i * 90.0))
                val nodeOffset = Offset(
                    (center.x + (currentRadius * 0.6f * sin(angle))).toFloat(),
                    (center.y + (currentRadius * 0.6f * kotlin.math.cos(angle))).toFloat()
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.8f),
                    radius = 2.5.dp.toPx(),
                    center = nodeOffset
                )
            }
        }
    }
}
