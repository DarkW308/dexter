package com.aistudio.dexter.ui.components

import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aistudio.dexter.engine.OrbVisualState
import com.aistudio.dexter.ui.theme.*
import kotlin.math.sin

@Composable
fun VoiceOrb(
    state: OrbVisualState,
    modifier: Modifier = Modifier,
    size: Dp = 190.dp,
    onTap: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbTransitions")

    // Idle breathing pulse
    val idlePulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "IdlePulse"
    )

    // Listening sonic ripple wave
    val rippleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RippleProgress"
    )

    // Processing rotation angle
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RotationAngle"
    )

    // Speaking bounce / frequency amplitude
    val speakingBounce by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SpeakingBounce"
    )

    val primaryColor = when (state) {
        OrbVisualState.IDLE -> DexterPrimary
        OrbVisualState.LISTENING -> DexterPrimaryGlow
        OrbVisualState.PROCESSING -> DexterPrimary
        OrbVisualState.SPEAKING -> DexterSuccess
        OrbVisualState.ERROR -> DexterError
    }

    val glowColor = when (state) {
        OrbVisualState.IDLE -> DexterPrimary.copy(alpha = 0.4f)
        OrbVisualState.LISTENING -> DexterPrimaryGlow.copy(alpha = 0.7f)
        OrbVisualState.PROCESSING -> DexterPrimary.copy(alpha = 0.6f)
        OrbVisualState.SPEAKING -> DexterSuccess.copy(alpha = 0.7f)
        OrbVisualState.ERROR -> DexterError.copy(alpha = 0.5f)
    }

    Box(
        modifier = modifier
            .size(size)
            .testTag("voice_orb_touch")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTap
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = this.size.width * 0.28f

            when (state) {
                OrbVisualState.IDLE -> {
                    // Outer subtle halo
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(glowColor, Color.Transparent),
                            center = center,
                            radius = baseRadius * 1.6f * idlePulse
                        ),
                        radius = baseRadius * 1.6f * idlePulse,
                        center = center
                    )

                    // Concentric delicate ring
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.35f),
                        radius = baseRadius * 1.25f * idlePulse,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // Core glowing sphere
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White, primaryColor, primaryColor.copy(alpha = 0.7f)),
                            center = center,
                            radius = baseRadius * idlePulse
                        ),
                        radius = baseRadius * idlePulse,
                        center = center
                    )
                }

                OrbVisualState.LISTENING -> {
                    // Fast expanding acoustic ripples
                    val r1 = baseRadius + (this.size.width * 0.45f - baseRadius) * rippleProgress
                    val alpha1 = (1f - rippleProgress).coerceIn(0f, 1f)
                    drawCircle(
                        color = glowColor.copy(alpha = alpha1),
                        radius = r1,
                        center = center,
                        style = Stroke(width = 3.dp.toPx())
                    )

                    val r2Progress = (rippleProgress + 0.5f) % 1f
                    val r2 = baseRadius + (this.size.width * 0.45f - baseRadius) * r2Progress
                    val alpha2 = (1f - r2Progress).coerceIn(0f, 1f)
                    drawCircle(
                        color = glowColor.copy(alpha = alpha2 * 0.8f),
                        radius = r2,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // Active energetic core
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White, DexterPrimaryGlow, primaryColor),
                            center = center,
                            radius = baseRadius * 1.15f
                        ),
                        radius = baseRadius * 1.15f,
                        center = center
                    )
                }

                OrbVisualState.PROCESSING -> {
                    // Rotating multi-arc quantum compass
                    rotate(rotationAngle, pivot = center) {
                        drawArc(
                            color = primaryColor,
                            startAngle = 0f,
                            sweepAngle = 100f,
                            useCenter = false,
                            topLeft = Offset(center.x - baseRadius * 1.35f, center.y - baseRadius * 1.35f),
                            size = androidx.compose.ui.geometry.Size(baseRadius * 2.7f, baseRadius * 2.7f),
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = primaryColor.copy(alpha = 0.5f),
                            startAngle = 180f,
                            sweepAngle = 90f,
                            useCenter = false,
                            topLeft = Offset(center.x - baseRadius * 1.35f, center.y - baseRadius * 1.35f),
                            size = androidx.compose.ui.geometry.Size(baseRadius * 2.7f, baseRadius * 2.7f),
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // Pulsing inner ring
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.25f),
                        radius = baseRadius * 1.1f,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White, primaryColor),
                            center = center,
                            radius = baseRadius
                        ),
                        radius = baseRadius,
                        center = center
                    )
                }

                OrbVisualState.SPEAKING -> {
                    // Pulsing green halo
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(glowColor, Color.Transparent),
                            center = center,
                            radius = baseRadius * (1.3f + speakingBounce * 0.35f)
                        ),
                        radius = baseRadius * (1.3f + speakingBounce * 0.35f),
                        center = center
                    )

                    // Vertical amplitude bars
                    val barCount = 7
                    val barWidth = 4.dp.toPx()
                    val totalWidth = barCount * barWidth * 2.2f
                    val startX = center.x - totalWidth / 2f

                    for (i in 0 until barCount) {
                        val factor = sin((i.toFloat() / barCount) * Math.PI).toFloat()
                        val barHeight = (baseRadius * 0.5f) + (baseRadius * 0.9f * factor * speakingBounce)
                        val x = startX + i * barWidth * 2.2f
                        drawLine(
                            color = DexterSuccess,
                            start = Offset(x, center.y - barHeight / 2f),
                            end = Offset(x, center.y + barHeight / 2f),
                            strokeWidth = barWidth,
                            cap = StrokeCap.Round
                        )
                    }

                    // Outer orbit ring
                    drawCircle(
                        color = DexterSuccess.copy(alpha = 0.4f),
                        radius = baseRadius * 1.35f,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                OrbVisualState.ERROR -> {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(DexterError.copy(alpha = 0.6f), Color.Transparent),
                            center = center,
                            radius = baseRadius * 1.5f
                        ),
                        radius = baseRadius * 1.5f,
                        center = center
                    )

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White, DexterError),
                            center = center,
                            radius = baseRadius
                        ),
                        radius = baseRadius,
                        center = center
                    )
                }
            }
        }
    }
}
