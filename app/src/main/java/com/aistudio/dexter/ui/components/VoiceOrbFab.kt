package com.aistudio.dexter.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
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
import kotlin.math.cos
import kotlin.math.sin

/**
 * Central Floating Action Button (FAB) that acts as Dexter's 'Voice Orb'.
 * Uses rich, animated Compose components to distinctly represent LISTENING,
 * PROCESSING, SPEAKING, and IDLE states.
 *
 * Designed with Material 3 floating depth, interactive press scale,
 * and high-fidelity Canvas animations.
 */
@Composable
fun VoiceOrbFab(
    state: OrbVisualState,
    audioIntensity: Float = 0f,
    modifier: Modifier = Modifier,
    fabSize: Dp = 64.dp,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Smooth press feedback scale
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "FabPressScale"
    )

    // Infinite transitions for ambient & state animations
    val transition = rememberInfiniteTransition(label = "VoiceOrbFabAnimations")

    // 1. Idle breathing pulse
    val idlePulse by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "IdlePulse"
    )

    // 2. Listening outward ripple waves
    val listeningRipple by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ListeningRipple"
    )

    // 3. Processing orbital spinner rotation (360 degrees)
    val processingRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ProcessingRotation"
    )

    // 4. Processing inner particle pulse
    val processingPulse by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ProcessingPulse"
    )

    // 4b. Processing subtle cognitive ripple wave (harmonic expansion)
    val processingRipple by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ProcessingRipple"
    )

    // 4c. Processing ambient glow luminescence pulse
    val processingGlowPulse by transition.animateFloat(
        initialValue = 0.50f,
        targetValue = 0.90f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ProcessingGlowPulse"
    )

    // 5. Speaking audio wave oscillations
    val speakingWave by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SpeakingWave"
    )

    // Dynamic state colors
    val primaryColor = when (state) {
        OrbVisualState.IDLE -> DexterPrimary
        OrbVisualState.LISTENING -> DexterPrimaryGlow
        OrbVisualState.PROCESSING -> Color(0xFF00E5FF)
        OrbVisualState.SPEAKING -> DexterSuccess
        OrbVisualState.ERROR -> DexterError
    }

    val glowColor = when (state) {
        OrbVisualState.IDLE -> DexterPrimary.copy(alpha = 0.40f)
        OrbVisualState.LISTENING -> DexterPrimaryGlow.copy(alpha = 0.70f)
        OrbVisualState.PROCESSING -> Color(0xFF80D8FF).copy(alpha = 0.65f)
        OrbVisualState.SPEAKING -> DexterSuccess.copy(alpha = 0.70f)
        OrbVisualState.ERROR -> DexterError.copy(alpha = 0.60f)
    }

    // Container size includes outer atmospheric glow aura
    val containerSize = fabSize + 24.dp

    Box(
        modifier = modifier
            .size(containerSize)
            .scale(pressScale)
            .testTag("voice_orb_fab"),
        contentAlignment = Alignment.Center
    ) {
        // --- 1. Canvas Layer: Atmospheric Halos, Orbiting Spinners & Expanding Wave Ripples ---
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = (fabSize.toPx() / 2f)

            // Outer atmospheric ambient glow
            val haloRadius = when (state) {
                OrbVisualState.IDLE -> baseRadius * 1.25f * idlePulse
                OrbVisualState.LISTENING -> baseRadius * (1.30f + audioIntensity * 0.40f)
                OrbVisualState.PROCESSING -> baseRadius * 1.32f * processingPulse
                OrbVisualState.SPEAKING -> baseRadius * (1.25f + speakingWave * 0.20f)
                OrbVisualState.ERROR -> baseRadius * 1.30f
            }

            // Atmospheric ambient glow (with distinctive cosmic cognitive shimmer during PROCESSING)
            if (state == OrbVisualState.PROCESSING) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF00E5FF).copy(alpha = 0.50f * processingGlowPulse),
                            Color(0xFF7C4DFF).copy(alpha = 0.35f * processingGlowPulse),
                            Color(0xFF00E5FF).copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = haloRadius * 1.15f
                    ),
                    radius = haloRadius * 1.15f,
                    center = center
                )
            } else {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor,
                            glowColor.copy(alpha = 0.35f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = haloRadius
                    ),
                    radius = haloRadius,
                    center = center
                )
            }

            // --- STATE SPECIFIC ANIMATIONS ---
            when (state) {
                OrbVisualState.LISTENING -> {
                    // Concentric ripple rings propagating outward
                    val r1 = baseRadius + (haloRadius - baseRadius) * listeningRipple
                    val a1 = ((1f - listeningRipple) * (0.6f + audioIntensity * 0.4f)).coerceIn(0f, 1f)
                    drawCircle(
                        color = DexterPrimaryGlow.copy(alpha = a1),
                        radius = r1,
                        center = center,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )

                    val r2Phase = (listeningRipple + 0.5f) % 1f
                    val r2 = baseRadius + (haloRadius - baseRadius) * r2Phase
                    val a2 = ((1f - r2Phase) * (0.5f + audioIntensity * 0.3f)).coerceIn(0f, 1f)
                    drawCircle(
                        color = DexterPrimaryGlow.copy(alpha = a2),
                        radius = r2,
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                OrbVisualState.PROCESSING -> {
                    // Distinct subtle cognitive ripple wave propagating outward
                    val procR1 = baseRadius + (haloRadius * 1.15f - baseRadius) * processingRipple
                    val procA1 = ((1f - processingRipple) * 0.55f * processingGlowPulse).coerceIn(0f, 1f)

                    // Ethereal radial gradient ripple ring
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF00E5FF).copy(alpha = procA1),
                                Color(0xFF7C4DFF).copy(alpha = procA1 * 0.65f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = procR1.coerceAtLeast(1f)
                        ),
                        radius = procR1.coerceAtLeast(1f),
                        center = center
                    )
                    drawCircle(
                        color = Color(0xFF80D8FF).copy(alpha = procA1 * 0.85f),
                        radius = procR1.coerceAtLeast(1f),
                        center = center,
                        style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Secondary phase-shifted subtle cognitive echo ripple (half-period delayed)
                    val procPhase2 = (processingRipple + 0.5f) % 1f
                    val procR2 = baseRadius + (haloRadius * 1.15f - baseRadius) * procPhase2
                    val procA2 = ((1f - procPhase2) * 0.35f * processingGlowPulse).coerceIn(0f, 1f)
                    drawCircle(
                        color = Color(0xFF7C4DFF).copy(alpha = procA2),
                        radius = procR2.coerceAtLeast(1f),
                        center = center,
                        style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Rotating orbital spinner ring around the FAB edge with electric cyan & violet accents
                    rotate(processingRotation, pivot = center) {
                        val arcRadius = baseRadius + 4.dp.toPx()
                        // 140-degree spinning celestial arc
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(
                                    Color.Transparent,
                                    Color(0xFF7C4DFF).copy(alpha = 0.35f),
                                    Color(0xFF00E5FF),
                                    Color.White
                                )
                            ),
                            startAngle = 0f,
                            sweepAngle = 140f,
                            useCenter = false,
                            topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
                            size = androidx.compose.ui.geometry.Size(arcRadius * 2f, arcRadius * 2f),
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Orbiting satellite dot
                        val dotAngleRad = Math.toRadians(140.0)
                        val dotX = center.x + arcRadius * cos(dotAngleRad).toFloat()
                        val dotY = center.y + arcRadius * sin(dotAngleRad).toFloat()
                        drawCircle(
                            color = Color.White,
                            radius = 3.5.dp.toPx(),
                            center = Offset(dotX, dotY)
                        )

                        // Secondary counter-arc in luminous violet
                        drawArc(
                            color = Color(0xFF7C4DFF).copy(alpha = 0.65f),
                            startAngle = 190f,
                            sweepAngle = 60f,
                            useCenter = false,
                            topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
                            size = androidx.compose.ui.geometry.Size(arcRadius * 2f, arcRadius * 2f),
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }

                OrbVisualState.SPEAKING -> {
                    // Pulsating acoustic concentric border
                    val speakR = baseRadius + 3.dp.toPx() * speakingWave
                    drawCircle(
                        color = DexterSuccess.copy(alpha = 0.6f * (1f - speakingWave * 0.4f)),
                        radius = speakR,
                        center = center,
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                }

                OrbVisualState.IDLE -> {
                    // Gentle ambient outline
                    drawCircle(
                        color = DexterPrimary.copy(alpha = 0.25f * idlePulse),
                        radius = baseRadius + 2.dp.toPx(),
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }

                OrbVisualState.ERROR -> {
                    drawCircle(
                        color = DexterError.copy(alpha = 0.6f),
                        radius = baseRadius + 4.dp.toPx(),
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }

        // --- 2. Floating Action Button Core ---
        Box(
            modifier = Modifier
                .size(fabSize)
                .shadow(
                    elevation = if (state == OrbVisualState.LISTENING || state == OrbVisualState.PROCESSING) 16.dp else 10.dp,
                    shape = CircleShape,
                    spotColor = primaryColor,
                    ambientColor = primaryColor
                )
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = when (state) {
                            OrbVisualState.IDLE -> listOf(
                                Color(0xFF4FC3F7),
                                Color(0xFF1E3A5F),
                                Color(0xFF121826)
                            )
                            OrbVisualState.LISTENING -> listOf(
                                Color(0xFFFFFFFF),
                                Color(0xFF00E5FF),
                                Color(0xFF0D47A1)
                            )
                            OrbVisualState.PROCESSING -> listOf(
                                Color(0xFFE1F5FE),
                                Color(0xFF00E5FF),
                                Color(0xFF311B92),
                                Color(0xFF0A0E1A)
                            )
                            OrbVisualState.SPEAKING -> listOf(
                                Color(0xFFB9F6CA),
                                Color(0xFF00C853),
                                Color(0xFF1B5E20)
                            )
                            OrbVisualState.ERROR -> listOf(
                                Color(0xFFFF8A80),
                                Color(0xFFD50000),
                                Color(0xFF260D0D)
                            )
                        }
                    )
                )
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.7f),
                            primaryColor.copy(alpha = 0.5f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            // Central Icon or Core Indicator
            when (state) {
                OrbVisualState.PROCESSING -> {
                    // Animated Processing Core: Inner Rotating Cog/Dot Grid
                    Canvas(modifier = Modifier.size(24.dp)) {
                        val c = Offset(size.width / 2f, size.height / 2f)
                        rotate(-processingRotation * 1.5f, pivot = c) {
                            drawCircle(color = Color.White, radius = 2.5.dp.toPx(), center = c)
                            for (i in 0 until 4) {
                                val a = Math.toRadians((i * 90.0))
                                val r = 7.dp.toPx()
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.85f),
                                    radius = 1.8.dp.toPx(),
                                    center = Offset(c.x + r * cos(a).toFloat(), c.y + r * sin(a).toFloat())
                                )
                            }
                        }
                    }
                }
                OrbVisualState.LISTENING -> {
                    // Pulsating Mic Graphic in Listening mode
                    FabMicVector(tint = Color.White, modifier = Modifier.size(26.dp).scale(1.0f + audioIntensity * 0.15f))
                }
                OrbVisualState.SPEAKING -> {
                    // Acoustic Audio Bars in Speaking mode
                    Canvas(modifier = Modifier.size(22.dp)) {
                        val barWidth = 3.dp.toPx()
                        val spacing = 3.dp.toPx()
                        val totalW = barWidth * 3 + spacing * 2
                        val startX = (size.width - totalW) / 2f
                        val midY = size.height / 2f

                        val h1 = (size.height * 0.35f + speakingWave * size.height * 0.3f)
                        val h2 = (size.height * 0.75f - speakingWave * size.height * 0.25f)
                        val h3 = (size.height * 0.45f + speakingWave * size.height * 0.25f)

                        drawLine(Color.White, Offset(startX, midY - h1 / 2), Offset(startX, midY + h1 / 2), barWidth, StrokeCap.Round)
                        drawLine(Color.White, Offset(startX + barWidth + spacing, midY - h2 / 2), Offset(startX + barWidth + spacing, midY + h2 / 2), barWidth, StrokeCap.Round)
                        drawLine(Color.White, Offset(startX + (barWidth + spacing) * 2, midY - h3 / 2), Offset(startX + (barWidth + spacing) * 2, midY + h3 / 2), barWidth, StrokeCap.Round)
                    }
                }
                else -> {
                    // Standard Crisp Mic Icon in IDLE/ERROR
                    FabMicVector(tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun FabMicVector(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f

        // Mic capsule dimensions
        val capsuleW = w * 0.36f
        val capsuleH = h * 0.52f
        val capsuleTop = h * 0.12f
        val strokeWidth = 2.dp.toPx()

        // 1. Center Mic Capsule
        drawRoundRect(
            color = tint,
            topLeft = Offset(cx - capsuleW / 2f, capsuleTop),
            size = androidx.compose.ui.geometry.Size(capsuleW, capsuleH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(capsuleW / 2f, capsuleW / 2f)
        )

        // 2. Outer Cradle U-bracket
        val cradleTop = capsuleTop + capsuleH * 0.35f
        val cradleBottom = capsuleTop + capsuleH + 2.dp.toPx()
        val cradleW = capsuleW + 8.dp.toPx()

        drawArc(
            color = tint,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(cx - cradleW / 2f, cradleTop),
            size = androidx.compose.ui.geometry.Size(cradleW, cradleBottom - cradleTop),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // 3. Stem and base foot
        val stemTop = cradleBottom + (cradleBottom - cradleTop) / 2f - strokeWidth
        val stemBottom = h * 0.88f
        drawLine(
            color = tint,
            start = Offset(cx, stemTop),
            end = Offset(cx, stemBottom),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        val footW = w * 0.36f
        drawLine(
            color = tint,
            start = Offset(cx - footW / 2f, stemBottom),
            end = Offset(cx + footW / 2f, stemBottom),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}
