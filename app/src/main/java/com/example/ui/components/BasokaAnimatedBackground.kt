package com.example.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.example.model.AssistantState
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.PurpleGlow
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

private data class NeuralNode(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val radius: Float,
    val color: Color
)

private data class EnergyPulse(
    val fromNodeIdx: Int,
    val toNodeIdx: Int,
    var progress: Float,
    val speed: Float
)

@Composable
fun BasokaAnimatedBackground(
    assistantState: AssistantState = AssistantState.IDLE,
    audioRms: Float = 0f,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "basoka_bg_anim")

    // Slow continuous rotation for HUD rings
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 35000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_rotation"
    )

    // Counter-rotation for secondary ring
    val counterRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "counter_rotation"
    )

    // Breathing glow animation
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow"
    )

    // Ripple wave for active assistant states
    val rippleWave by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (assistantState) {
                    AssistantState.THINKING -> 1500
                    AssistantState.LISTENING -> 2000
                    AssistantState.SPEAKING -> 1800
                    else -> 4000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_wave"
    )

    // Initialize floating nodes for the neural constellation mesh
    val nodes = remember {
        mutableStateListOf<NeuralNode>().apply {
            val random = Random(42)
            val colors = listOf(ElectricCyan, CyanGlow, NeonPurple, PurpleGlow)
            for (i in 0 until 22) {
                add(
                    NeuralNode(
                        x = random.nextFloat(),
                        y = random.nextFloat(),
                        vx = (random.nextFloat() - 0.5f) * 0.0007f,
                        vy = (random.nextFloat() - 0.5f) * 0.0007f,
                        radius = 2.5f + random.nextFloat() * 2.5f,
                        color = colors[i % colors.size]
                    )
                )
            }
        }
    }

    // Energy pulses moving between nodes
    val pulses = remember {
        mutableStateListOf<EnergyPulse>().apply {
            for (i in 0 until 5) {
                add(
                    EnergyPulse(
                        fromNodeIdx = (i * 4) % 22,
                        toNodeIdx = (i * 4 + 3) % 22,
                        progress = i * 0.2f,
                        speed = 0.008f + (i * 0.002f)
                    )
                )
            }
        }
    }

    // Frame-based physics update for the floating neural nodes
    val speedMultiplier = when (assistantState) {
        AssistantState.THINKING -> 2.2f
        AssistantState.LISTENING -> 1.6f
        AssistantState.SPEAKING -> 1.8f
        else -> 1.0f
    }

    LaunchedEffect(speedMultiplier) {
        var lastFrameNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                if (lastFrameNanos != 0L) {
                    for (node in nodes) {
                        node.x += node.vx * speedMultiplier
                        node.y += node.vy * speedMultiplier

                        // Bounce off boundaries smoothly
                        if (node.x <= 0.02f || node.x >= 0.98f) {
                            node.vx = -node.vx
                        }
                        if (node.y <= 0.02f || node.y >= 0.98f) {
                            node.vy = -node.vy
                        }
                    }

                    // Advance energy pulses
                    for (pulse in pulses) {
                        pulse.progress += pulse.speed * speedMultiplier
                        if (pulse.progress >= 1f) {
                            pulse.progress = 0f
                        }
                    }
                }
                lastFrameNanos = frameNanos
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val center = Offset(width / 2f, height / 2f)

        // 1. Deep Space Dark Gradient Canvas
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF0C1425),
                    CyberBlack,
                    Color(0xFF030508)
                ),
                center = center,
                radius = hypot(width, height) * 0.65f
            )
        )

        // 2. Intelligent Digital Grid Lines (subtle cyber grid)
        val gridSpacing = 64f
        val gridAlpha = 0.035f * pulseGlow
        val gridColor = ElectricCyan.copy(alpha = gridAlpha)

        var gx = 0f
        while (gx <= width) {
            drawLine(
                color = gridColor,
                start = Offset(gx, 0f),
                end = Offset(gx, height),
                strokeWidth = 1f
            )
            gx += gridSpacing
        }

        var gy = 0f
        while (gy <= height) {
            drawLine(
                color = gridColor,
                start = Offset(0f, gy),
                end = Offset(width, gy),
                strokeWidth = 1f
            )
            gy += gridSpacing
        }

        // 3. Central Ambient Aura behind "basoka"
        val centralGlowColor = when (assistantState) {
            AssistantState.THINKING -> NeonPurple
            AssistantState.LISTENING -> ElectricCyan
            AssistantState.SPEAKING -> CyanGlow
            else -> ElectricCyan
        }

        val auraRadius = (width * 0.42f) * (0.95f + pulseGlow * 0.1f + (audioRms * 0.2f))
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    centralGlowColor.copy(alpha = 0.12f * pulseGlow),
                    NeonPurple.copy(alpha = 0.06f * pulseGlow),
                    Color.Transparent
                ),
                center = center,
                radius = auraRadius
            ),
            center = center,
            radius = auraRadius
        )

        // 4. Expanding Intelligent Ripple Waves from the center
        val maxWaveRadius = width * 0.65f
        val currentWaveRadius = maxWaveRadius * rippleWave
        val waveAlpha = (1f - rippleWave) * 0.25f
        drawCircle(
            color = centralGlowColor.copy(alpha = waveAlpha),
            radius = currentWaveRadius,
            center = center,
            style = Stroke(width = 2f)
        )

        // 5. HUD Cyber Rings revolving around "basoka"
        val innerRingRadius = width * 0.32f
        val outerRingRadius = width * 0.38f

        // Outer HUD ring with dashed tick marks
        rotate(ringRotation, pivot = center) {
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        ElectricCyan.copy(alpha = 0.16f),
                        Color.Transparent,
                        NeonPurple.copy(alpha = 0.16f),
                        Color.Transparent,
                        ElectricCyan.copy(alpha = 0.16f)
                    )
                ),
                radius = outerRingRadius,
                center = center,
                style = Stroke(width = 1.5f)
            )

            // Orbital node points on ring
            for (i in 0 until 4) {
                val angle = (i * 90.0) * (Math.PI / 180.0)
                val nodePos = Offset(
                    center.x + (outerRingRadius * cos(angle)).toFloat(),
                    center.y + (outerRingRadius * sin(angle)).toFloat()
                )
                drawCircle(
                    color = ElectricCyan.copy(alpha = 0.35f * pulseGlow),
                    radius = 3f,
                    center = nodePos
                )
            }
        }

        // Inner counter-rotating decorative ring
        rotate(counterRotation, pivot = center) {
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.Transparent,
                        CyanGlow.copy(alpha = 0.12f),
                        Color.Transparent,
                        PurpleGlow.copy(alpha = 0.12f)
                    )
                ),
                radius = innerRingRadius,
                center = center,
                style = Stroke(width = 1.2f)
            )
        }

        // 6. Neural Network Synaptic Lines and Floating Nodes
        val maxConnectionDist = width * 0.32f
        val nodePoints = nodes.map { Offset(it.x * width, it.y * height) }

        // Draw synaptic connections between close nodes
        for (i in 0 until nodePoints.size) {
            val p1 = nodePoints[i]
            for (j in i + 1 until nodePoints.size) {
                val p2 = nodePoints[j]
                val dist = hypot(p1.x - p2.x, p1.y - p2.y)
                if (dist < maxConnectionDist) {
                    val lineAlpha = (1f - (dist / maxConnectionDist)) * 0.18f * pulseGlow
                    val lineColor = if (i % 2 == 0) ElectricCyan else NeonPurple
                    drawLine(
                        color = lineColor.copy(alpha = lineAlpha),
                        start = p1,
                        end = p2,
                        strokeWidth = 1.2f
                    )
                }
            }
        }

        // Draw glowing energy pulses traveling between nodes
        for (pulse in pulses) {
            if (pulse.fromNodeIdx < nodePoints.size && pulse.toNodeIdx < nodePoints.size) {
                val from = nodePoints[pulse.fromNodeIdx]
                val to = nodePoints[pulse.toNodeIdx]
                val currentPulsePos = Offset(
                    x = from.x + (to.x - from.x) * pulse.progress,
                    y = from.y + (to.y - from.y) * pulse.progress
                )
                drawCircle(
                    color = ElectricCyan.copy(alpha = 0.7f * pulseGlow),
                    radius = 3.5f,
                    center = currentPulsePos
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.9f),
                    radius = 1.8f,
                    center = currentPulsePos
                )
            }
        }

        // Draw individual neural node dots
        for (i in 0 until nodes.size) {
            val node = nodes[i]
            val pt = nodePoints[i]
            drawCircle(
                color = node.color.copy(alpha = 0.45f * pulseGlow),
                radius = node.radius * 1.5f,
                center = pt
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.7f),
                radius = node.radius * 0.6f,
                center = pt
            )
        }

        // 7. Stylized Intelligent Watermark Branding: "basoka"
        // Rendered using nativeCanvas with glowing shadow and high-tech typography
        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas

            // Main "basoka" Text Paint
            val textPaint = Paint().apply {
                isAntiAlias = true
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textSize = width * 0.16f // Dynamic proportional size
                textAlign = Paint.Align.CENTER
                letterSpacing = 0.28f
                // Glowing text color with breathing alpha
                val alphaInt = (52 * pulseGlow).toInt().coerceIn(30, 85)
                color = android.graphics.Color.argb(
                    alphaInt,
                    0x00,
                    0xE5,
                    0xFF
                )
                // High-tech drop shadow glow
                setShadowLayer(
                    32f * pulseGlow,
                    0f,
                    0f,
                    android.graphics.Color.argb((70 * pulseGlow).toInt(), 0x00, 0xE5, 0xFF)
                )
            }

            // Subtitle Paint "INTELLIGENT AI CORE"
            val subPaint = Paint().apply {
                isAntiAlias = true
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
                textSize = width * 0.032f
                textAlign = Paint.Align.CENTER
                letterSpacing = 0.42f
                val subAlpha = (40 * pulseGlow).toInt().coerceIn(25, 75)
                color = android.graphics.Color.argb(
                    subAlpha,
                    0xA8,
                    0x55,
                    0xF7
                )
            }

            // Draw "basoka" in the optical center of the background
            val textY = center.y + (textPaint.textSize * 0.35f)
            nativeCanvas.drawText("basoka", center.x, textY, textPaint)

            // Draw subtitle under "basoka"
            val subY = textY + (textPaint.textSize * 0.45f)
            nativeCanvas.drawText("NEURAL AI ENGINE", center.x, subY, subPaint)
        }
    }
}
