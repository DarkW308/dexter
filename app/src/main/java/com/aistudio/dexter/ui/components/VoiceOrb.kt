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
import androidx.compose.ui.geometry.Size
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Custom Composable VoiceOrb component that visualizes microphone input intensity
 * using a dynamic growing and shrinking circular gradient animation.
 *
 * Designed for Dexter's Deep Dark aesthetic (#1A1A2E canvas, #4FC3F7 electric blue, #00E5FF cyan glow).
 *
 * @param state The current visual operational state (IDLE, LISTENING, PROCESSING, SPEAKING, ERROR).
 * @param audioIntensity The real-time microphone input intensity level (0.0f to 1.0f or dB level).
 *                       Drives the growing and shrinking circular gradient animation.
 * @param modifier Optional Compose Modifier.
 * @param size Outer visual dimension of the orb.
 * @param onTap Callback invoked when user taps the orb to activate/stop speech.
 */
@Composable
fun VoiceOrb(
    state: OrbVisualState = OrbVisualState.IDLE,
    audioIntensity: Float = 0f,
    modifier: Modifier = Modifier,
    size: Dp = 190.dp,
    onTap: () -> Unit = {}
) {
    // Normalize audio intensity (handles 0..1 range or raw dB scale up to ~12dB from Android SpeechRecognizer)
    val normalizedIntensity = when {
        audioIntensity <= 0f -> 0f
        audioIntensity in 0f..1f -> audioIntensity
        else -> (audioIntensity / 10f).coerceIn(0f, 1f)
    }

    // Smoothly animate the intensity to prevent abrupt visual jumps
    val animatedIntensity by animateFloatAsState(
        targetValue = normalizedIntensity,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "AudioIntensityGrowth"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "OrbTransitions")

    // Continuous ambient breathing pulse
    val idlePulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "IdleBreathing"
    )

    // Dynamic wave expansion for listening ripple effect
    val rippleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RippleProgress"
    )

    // Rotating orbital spinner for processing state
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ProcessingRotation"
    )

    // Voice response speaking bounce
    val speakingBounce by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SpeakingBounce"
    )

    // Color palette mapping based on current state
    val primaryColor = when (state) {
        OrbVisualState.IDLE -> DexterPrimary
        OrbVisualState.LISTENING -> DexterPrimaryGlow
        OrbVisualState.PROCESSING -> DexterPrimary
        OrbVisualState.SPEAKING -> DexterSuccess
        OrbVisualState.ERROR -> DexterError
    }

    val glowColor = when (state) {
        OrbVisualState.IDLE -> DexterPrimary.copy(alpha = 0.45f)
        OrbVisualState.LISTENING -> DexterPrimaryGlow.copy(alpha = 0.75f)
        OrbVisualState.PROCESSING -> DexterPrimary.copy(alpha = 0.60f)
        OrbVisualState.SPEAKING -> DexterSuccess.copy(alpha = 0.75f)
        OrbVisualState.ERROR -> DexterError.copy(alpha = 0.60f)
    }

    Box(
        modifier = modifier
            .size(size)
            .testTag("voice_orb")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTap
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(size)
                .testTag("voice_orb_canvas")
        ) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = this.size.width * 0.28f

            // Dynamic scale factor derived from microphone intensity (grows and shrinks)
            // Combined with idle breathing when sound is quiescent
            val dynamicScale = (1.0f + animatedIntensity * 0.42f) * (if (state == OrbVisualState.IDLE) idlePulse else 1.0f)

            // 1. OUTER ATMOSPHERIC RADIAL GRADIENT (Grows and shrinks with mic intensity)
            val outerHaloRadius = (baseRadius * 1.55f * dynamicScale + animatedIntensity * baseRadius * 0.8f).coerceAtLeast(10f)
            val haloAlpha = (0.28f + animatedIntensity * 0.55f).coerceIn(0.1f, 0.95f)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glowColor.copy(alpha = haloAlpha),
                        glowColor.copy(alpha = haloAlpha * 0.4f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = outerHaloRadius
                ),
                radius = outerHaloRadius,
                center = center
            )

            // 2. CONCENTRIC REACTIVE PULSE RINGS (Expands outwards dynamically)
            when (state) {
                OrbVisualState.LISTENING -> {
                    // Acoustic ripples expanding outward proportional to mic intensity
                    val r1 = baseRadius + (this.size.width * 0.46f - baseRadius) * rippleProgress * (0.8f + animatedIntensity * 0.4f)
                    val alpha1 = ((1f - rippleProgress) * (0.4f + animatedIntensity * 0.6f)).coerceIn(0f, 1f)
                    drawCircle(
                        color = glowColor.copy(alpha = alpha1),
                        radius = r1,
                        center = center,
                        style = Stroke(width = (2.5f + animatedIntensity * 3.5f).dp.toPx())
                    )

                    val r2Progress = (rippleProgress + 0.5f) % 1f
                    val r2 = baseRadius + (this.size.width * 0.46f - baseRadius) * r2Progress * (0.8f + animatedIntensity * 0.4f)
                    val alpha2 = ((1f - r2Progress) * (0.35f + animatedIntensity * 0.55f)).coerceIn(0f, 1f)
                    drawCircle(
                        color = glowColor.copy(alpha = alpha2),
                        radius = r2,
                        center = center,
                        style = Stroke(width = (1.5f + animatedIntensity * 2.5f).dp.toPx())
                    )

                    // Secondary close aura ring reacting to vocal pressure
                    val micRingRadius = baseRadius * (1.18f + animatedIntensity * 0.28f)
                    drawCircle(
                        color = primaryColor.copy(alpha = (0.30f + animatedIntensity * 0.55f).coerceIn(0f, 0.9f)),
                        radius = micRingRadius,
                        center = center,
                        style = Stroke(width = (2.0f + animatedIntensity * 2.0f).dp.toPx())
                    )
                }

                OrbVisualState.IDLE -> {
                    // Gentle quiescent ring
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.32f),
                        radius = baseRadius * 1.25f * idlePulse,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                OrbVisualState.PROCESSING -> {
                    // Rotating quantum arc ring
                    rotate(rotationAngle, pivot = center) {
                        drawArc(
                            color = primaryColor,
                            startAngle = 0f,
                            sweepAngle = 100f,
                            useCenter = false,
                            topLeft = Offset(center.x - baseRadius * 1.35f, center.y - baseRadius * 1.35f),
                            size = Size(baseRadius * 2.7f, baseRadius * 2.7f),
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = primaryColor.copy(alpha = 0.45f),
                            startAngle = 180f,
                            sweepAngle = 90f,
                            useCenter = false,
                            topLeft = Offset(center.x - baseRadius * 1.35f, center.y - baseRadius * 1.35f),
                            size = Size(baseRadius * 2.7f, baseRadius * 2.7f),
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }

                OrbVisualState.SPEAKING -> {
                    // Speaking acoustic amplitude bars
                    val barCount = 7
                    val barWidth = 4.dp.toPx()
                    val totalWidth = barCount * barWidth * 2.2f
                    val startX = center.x - totalWidth / 2f

                    for (i in 0 until barCount) {
                        val factor = sin((i.toFloat() / barCount) * PI).toFloat()
                        val barHeight = (baseRadius * 0.45f) + (baseRadius * 0.95f * factor * speakingBounce)
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
                        color = DexterSuccess.copy(alpha = 0.45f),
                        radius = baseRadius * (1.35f + speakingBounce * 0.15f),
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                OrbVisualState.ERROR -> {
                    drawCircle(
                        color = DexterError.copy(alpha = 0.4f),
                        radius = baseRadius * 1.35f,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }

            // 3. CORE CIRCULAR GRADIENT SPHERE (The glowing growing & shrinking orb)
            val coreRadius = (baseRadius * dynamicScale).coerceAtLeast(10f)
            val highlightOffset = Offset(
                center.x - coreRadius * 0.20f,
                center.y - coreRadius * 0.20f
            )

            val coreColors = when (state) {
                OrbVisualState.LISTENING -> listOf(
                    Color.White,
                    DexterPrimaryGlow,
                    primaryColor,
                    Color(0xFF003852)
                )
                OrbVisualState.SPEAKING -> listOf(
                    Color.White,
                    Color(0xFFB9F6CA),
                    DexterSuccess,
                    Color(0xFF004D2E)
                )
                OrbVisualState.ERROR -> listOf(
                    Color.White,
                    Color(0xFFFF8A80),
                    DexterError,
                    Color(0xFF4A101A)
                )
                else -> listOf(
                    Color.White,
                    DexterPrimaryGlow.copy(alpha = 0.9f),
                    primaryColor,
                    Color(0xFF0E2238)
                )
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = coreColors,
                    center = highlightOffset,
                    radius = coreRadius * 1.05f
                ),
                radius = coreRadius,
                center = center
            )

            // 4. INNER SPECULAR HIGHLIGHT (Gives 3D glass spherical depth)
            val innerHighlightRadius = coreRadius * 0.45f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.65f),
                        Color.White.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = highlightOffset,
                    radius = innerHighlightRadius
                ),
                radius = innerHighlightRadius,
                center = highlightOffset
            )

            // 5. VOCAL ENERGY RAYS (Subtle radiating light bursts during high mic input intensity)
            if (animatedIntensity > 0.08f && state == OrbVisualState.LISTENING) {
                val rayCount = 12
                val minRayDist = coreRadius * 1.02f
                val maxRayDist = coreRadius * (1.12f + animatedIntensity * 0.35f)
                val rayColor = Color.White.copy(alpha = (animatedIntensity * 0.6f).coerceIn(0f, 0.75f))

                for (i in 0 until rayCount) {
                    val angleRad = (i.toFloat() / rayCount) * (2f * PI.toFloat())
                    val cosA = cos(angleRad)
                    val sinA = sin(angleRad)

                    val startPoint = Offset(center.x + cosA * minRayDist, center.y + sinA * minRayDist)
                    val endPoint = Offset(center.x + cosA * maxRayDist, center.y + sinA * maxRayDist)

                    drawLine(
                        color = rayColor,
                        start = startPoint,
                        end = endPoint,
                        strokeWidth = (1.5f + animatedIntensity * 2f).dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}
