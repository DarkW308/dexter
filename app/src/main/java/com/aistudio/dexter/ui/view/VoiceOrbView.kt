package com.aistudio.dexter.ui.view

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import com.aistudio.dexter.engine.OrbVisualState
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin

/**
 * High-performance, lightweight custom Voice Orb View for Dexter.
 * Uses hardware-accelerated Canvas drawing and ValueAnimators with zero Compose runtime overhead.
 * Smoothly renders 5 visual states: IDLE, LISTENING, PROCESSING, SPEAKING, ERROR.
 */
class VoiceOrbView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class State {
        IDLE,
        LISTENING,
        PROCESSING,
        SPEAKING,
        ERROR
    }

    private var currentState: State = State.IDLE

    // Listeners for touch gestures
    var onTap: (() -> Unit)? = null
    var onLongPress: (() -> Unit)? = null

    // Colors according to DEXTER_APP_UIUX.md specifications
    private val colorIdlePrimary = Color.parseColor("#4FC3F7")       // Electric blue
    private val colorListeningGlow = Color.parseColor("#00E5FF")     // Glowing cyan
    private val colorProcessing = Color.parseColor("#4FC3F7")        // Processing cyan
    private val colorSpeaking = Color.parseColor("#69F0AE")          // Emerald green
    private val colorError = Color.parseColor("#CF6679")             // Error red
    private val colorCoreDark = Color.parseColor("#121224")

    // Paints (reused to avoid GC during animation frames)
    private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val arcBounds = RectF()

    // Animation values
    private var idlePulseFraction = 0f
    private var waveProgress = 0f
    private var spinnerAngle = 0f
    private var processingRippleProgress = 0f
    private var processingGlowFraction = 0f
    private var speakingPhase = 0f
    private var errorFlashFraction = 0f
    private var touchScale = 1.0f

    // Animators
    private var idleAnimator: ValueAnimator? = null
    private var waveAnimator: ValueAnimator? = null
    private var spinnerAnimator: ValueAnimator? = null
    private var processingRippleAnimator: ValueAnimator? = null
    private var processingGlowAnimator: ValueAnimator? = null
    private var speakingAnimator: ValueAnimator? = null
    private var errorAnimator: ValueAnimator? = null

    // Gesture detector
    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            touchScale = 0.94f
            invalidate()
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            touchScale = 1.0f
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            invalidate()
            onTap?.invoke()
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            touchScale = 1.0f
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            invalidate()
            onLongPress?.invoke()
        }
    })

    init {
        contentDescription = "Dexter voice input. Tap to speak."
        isClickable = true
        isFocusable = true
        startIdleAnimation()
    }

    fun setState(state: State) {
        if (currentState == state) return
        currentState = state
        stopAllAnimations()

        when (state) {
            State.IDLE -> startIdleAnimation()
            State.LISTENING -> startListeningAnimation()
            State.PROCESSING -> startProcessingAnimation()
            State.SPEAKING -> startSpeakingAnimation()
            State.ERROR -> startErrorAnimation()
        }
        invalidate()
    }

    fun setState(orbVisualState: OrbVisualState) {
        val mappedState = when (orbVisualState) {
            OrbVisualState.IDLE -> State.IDLE
            OrbVisualState.LISTENING -> State.LISTENING
            OrbVisualState.PROCESSING -> State.PROCESSING
            OrbVisualState.SPEAKING -> State.SPEAKING
            OrbVisualState.ERROR -> State.ERROR
        }
        setState(mappedState)
    }

    fun getState(): State = currentState

    private fun stopAllAnimations() {
        idleAnimator?.cancel()
        waveAnimator?.cancel()
        spinnerAnimator?.cancel()
        processingRippleAnimator?.cancel()
        processingGlowAnimator?.cancel()
        speakingAnimator?.cancel()
        errorAnimator?.cancel()
    }

    private fun startIdleAnimation() {
        idleAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2000L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                idlePulseFraction = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun startListeningAnimation() {
        waveAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1000L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                waveProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun startProcessingAnimation() {
        spinnerAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 1200L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                spinnerAngle = it.animatedValue as Float
                invalidate()
            }
            start()
        }

        // Subtle cognitive expanding ripple wave
        processingRippleAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1400L
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                processingRippleProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }

        // Ambient cognitive glow luminescence pulse
        processingGlowAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 850L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                processingGlowFraction = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun startSpeakingAnimation() {
        speakingAnimator = ValueAnimator.ofFloat(0f, (2 * PI).toFloat()).apply {
            duration = 600L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                speakingPhase = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun startErrorAnimation() {
        errorAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 600L
            repeatCount = 2
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener {
                errorFlashFraction = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val defaultSize = (200 * resources.displayMetrics.density).toInt()
        val width = resolveSize(defaultSize, widthMeasureSpec)
        val height = resolveSize(defaultSize, heightMeasureSpec)
        val size = min(width, height)
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (width <= 0 || height <= 0) return

        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f
        val maxRadius = (min(w, h) / 2f * 0.90f).coerceAtLeast(1f)
        val baseCoreRadius = (maxRadius * 0.44f).coerceAtLeast(1f)

        canvas.save()
        canvas.scale(touchScale, touchScale, cx, cy)

        try {
            when (currentState) {
                State.IDLE -> drawIdleState(canvas, cx, cy, baseCoreRadius, maxRadius)
                State.LISTENING -> drawListeningState(canvas, cx, cy, baseCoreRadius, maxRadius)
                State.PROCESSING -> drawProcessingState(canvas, cx, cy, baseCoreRadius, maxRadius)
                State.SPEAKING -> drawSpeakingState(canvas, cx, cy, baseCoreRadius, maxRadius)
                State.ERROR -> drawErrorState(canvas, cx, cy, baseCoreRadius, maxRadius)
            }
        } catch (e: Exception) {
            // Guard against any canvas shader exceptions during orientation change
        }

        canvas.restore()
    }

    private fun drawIdleState(canvas: Canvas, cx: Float, cy: Float, coreRadius: Float, maxRadius: Float) {
        // Slow gentle blue pulse (2000ms cycle)
        val breathingRadius = (coreRadius * (0.92f + 0.12f * idlePulseFraction)).coerceAtLeast(1f)
        val haloRadius = (maxRadius * 0.75f).coerceAtLeast(1f)
        val glowAlpha = (60 + 120 * idlePulseFraction).toInt()

        // Soft outer ambient halo
        corePaint.shader = RadialGradient(
            cx, cy, haloRadius,
            intArrayOf(Color.argb(glowAlpha / 2, 79, 195, 247), Color.TRANSPARENT),
            floatArrayOf(0.4f, 1.0f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, haloRadius, corePaint)

        // Core Glowing Orb
        corePaint.shader = RadialGradient(
            cx, cy, breathingRadius,
            intArrayOf(colorIdlePrimary, Color.argb(220, 26, 26, 46)),
            floatArrayOf(0.3f, 1.0f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, breathingRadius, corePaint)

        // Crisp inner circle outline
        ringPaint.strokeWidth = 2.5f * resources.displayMetrics.density
        ringPaint.color = Color.argb((180 + 75 * idlePulseFraction).toInt(), 79, 195, 247)
        canvas.drawCircle(cx, cy, breathingRadius, ringPaint)
    }

    private fun drawListeningState(canvas: Canvas, cx: Float, cy: Float, coreRadius: Float, maxRadius: Float) {
        // Fast expanding cyan wave rings (3 rings staggered)
        val numRings = 3
        val density = resources.displayMetrics.density
        ringPaint.strokeWidth = 3f * density

        for (i in 0 until numRings) {
            val ringPhase = (waveProgress + i.toFloat() / numRings) % 1.0f
            val ringRadius = coreRadius + (maxRadius - coreRadius) * ringPhase
            val alpha = ((1.0f - ringPhase) * 255).toInt().coerceIn(0, 255)

            ringPaint.color = Color.argb(alpha, 0, 229, 255)
            canvas.drawCircle(cx, cy, ringRadius, ringPaint)
        }

        // Expanded, brighter Core
        val listeningCoreRadius = (coreRadius * 1.08f).coerceAtLeast(1f)
        corePaint.shader = RadialGradient(
            cx, cy, listeningCoreRadius,
            intArrayOf(Color.WHITE, colorListeningGlow, Color.argb(240, 0, 150, 200)),
            floatArrayOf(0.1f, 0.6f, 1.0f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, listeningCoreRadius, corePaint)

        ringPaint.strokeWidth = 3.5f * density
        ringPaint.color = Color.argb(255, 0, 229, 255)
        canvas.drawCircle(cx, cy, listeningCoreRadius, ringPaint)
    }

    private fun drawProcessingState(canvas: Canvas, cx: Float, cy: Float, coreRadius: Float, maxRadius: Float) {
        val safeRadius = coreRadius.coerceAtLeast(1f)
        val density = resources.displayMetrics.density

        // 1. Subtle Outer Cosmic Glow Aura (Violet & Cyan synthesis shimmer)
        val glowRadius = (maxRadius * 0.85f * (0.95f + 0.1f * processingGlowFraction)).coerceAtLeast(1f)
        val glowAlpha = (50 + 40 * processingGlowFraction).toInt()
        corePaint.shader = RadialGradient(
            cx, cy, glowRadius,
            intArrayOf(
                Color.argb(glowAlpha, 0, 229, 255),
                Color.argb((glowAlpha * 0.7f).toInt(), 124, 77, 255),
                Color.TRANSPARENT
            ),
            floatArrayOf(0.35f, 0.70f, 1.0f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, glowRadius, corePaint)

        // 2. Subtle Cognitive Expanding Ripple Waves
        val rippleRadius = (safeRadius + (maxRadius * 0.85f - safeRadius) * processingRippleProgress).coerceAtLeast(1f)
        val rippleAlpha = ((1.0f - processingRippleProgress) * (130 + 60 * processingGlowFraction)).toInt().coerceIn(0, 255)
        ringPaint.strokeWidth = 2.5f * density
        ringPaint.color = Color.argb(rippleAlpha, 0, 229, 255)
        canvas.drawCircle(cx, cy, rippleRadius, ringPaint)

        // Secondary phase-shifted subtle cognitive echo ripple
        val echoProgress = (processingRippleProgress + 0.5f) % 1.0f
        val echoRadius = (safeRadius + (maxRadius * 0.85f - safeRadius) * echoProgress).coerceAtLeast(1f)
        val echoAlpha = ((1.0f - echoProgress) * 85).toInt().coerceIn(0, 255)
        ringPaint.strokeWidth = 1.6f * density
        ringPaint.color = Color.argb(echoAlpha, 124, 77, 255)
        canvas.drawCircle(cx, cy, echoRadius, ringPaint)

        // 3. Cognitive Center Core (Luminous White -> Electric Cyan -> Cosmic Violet -> Dark)
        corePaint.shader = RadialGradient(
            cx, cy, safeRadius,
            intArrayOf(
                Color.parseColor("#E1F5FE"),
                Color.parseColor("#00E5FF"),
                Color.parseColor("#311B92"),
                colorCoreDark
            ),
            floatArrayOf(0.15f, 0.45f, 0.82f, 1.0f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, safeRadius, corePaint)

        // 4. Outer Rotating Arcs with Dual Accents (Cyan + Violet)
        val arcRadius = (safeRadius * 1.35f).coerceAtLeast(1f)
        arcBounds.set(cx - arcRadius, cy - arcRadius, cx + arcRadius, cy + arcRadius)

        arcPaint.strokeWidth = 3.5f * density
        arcPaint.color = Color.parseColor("#00E5FF")
        canvas.drawArc(arcBounds, spinnerAngle, 120f, false, arcPaint)

        arcPaint.strokeWidth = 2.5f * density
        arcPaint.color = Color.parseColor("#7C4DFF")
        canvas.drawArc(arcBounds, spinnerAngle + 180f, 65f, false, arcPaint)
    }

    private fun drawSpeakingState(canvas: Canvas, cx: Float, cy: Float, coreRadius: Float, maxRadius: Float) {
        val safeRadius = coreRadius.coerceAtLeast(1f)
        // Core in Emerald Green
        corePaint.shader = RadialGradient(
            cx, cy, safeRadius,
            intArrayOf(Color.argb(240, 105, 240, 174), Color.argb(220, 20, 60, 40)),
            floatArrayOf(0.3f, 1.0f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, safeRadius, corePaint)

        ringPaint.strokeWidth = 2.5f * resources.displayMetrics.density
        ringPaint.color = colorSpeaking
        canvas.drawCircle(cx, cy, safeRadius, ringPaint)

        // Bouncing vertical amplitude sound bars (5 bars)
        val density = resources.displayMetrics.density
        barPaint.color = Color.parseColor("#1A1A2E")
        val numBars = 5
        val barWidth = 4.5f * density
        val barSpacing = 6f * density
        val totalWidth = numBars * barWidth + (numBars - 1) * barSpacing
        var startX = cx - totalWidth / 2f

        for (i in 0 until numBars) {
            val freqOffset = i * 1.25f
            val amplitude = abs(sin((speakingPhase + freqOffset).toDouble())).toFloat()
            val barHeight = (10f * density) + (26f * density * amplitude)

            val left = startX
            val top = cy - barHeight / 2f
            val right = left + barWidth
            val bottom = cy + barHeight / 2f

            canvas.drawRoundRect(left, top, right, bottom, barWidth / 2f, barWidth / 2f, barPaint)
            startX += barWidth + barSpacing
        }
    }

    private fun drawErrorState(canvas: Canvas, cx: Float, cy: Float, coreRadius: Float, maxRadius: Float) {
        val errorRadius = (coreRadius * (1.0f + 0.15f * errorFlashFraction)).coerceAtLeast(1f)
        corePaint.shader = RadialGradient(
            cx, cy, errorRadius,
            intArrayOf(colorError, Color.argb(240, 60, 20, 25)),
            floatArrayOf(0.4f, 1.0f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, errorRadius, corePaint)

        ringPaint.strokeWidth = 3f * resources.displayMetrics.density
        ringPaint.color = colorError
        canvas.drawCircle(cx, cy, errorRadius, ringPaint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
            touchScale = 1.0f
            invalidate()
        }
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAllAnimations()
    }
}
