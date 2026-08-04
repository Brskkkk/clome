package com.idk.gamecloner

import android.content.res.Resources
import android.util.DisplayMetrics
import android.view.Display
import android.view.Choreographer

/**
 * Hook library for injected DisplayMetrics spoofing
 * Embedded in patched APK to intercept resolution/DPI calls
 */
object MetricsHook {

    private const val TARGET_WIDTH = 360
    private const val TARGET_HEIGHT = 806
    private const val TARGET_DPI = 160
    private const val TARGET_DENSITY = 1.0f
    private const val TARGET_FPS = 60

    private val frameRateLimiter = FrameRateLimiter()

    /**
     * Create a spoofed DisplayMetrics object
     */
    fun createSpoofedMetrics(): DisplayMetrics {
        return DisplayMetrics().apply {
            widthPixels = TARGET_WIDTH
            heightPixels = TARGET_HEIGHT
            densityDpi = TARGET_DPI
            density = TARGET_DENSITY
            scaledDensity = TARGET_DENSITY
            xdpi = TARGET_DPI.toFloat()
            ydpi = TARGET_DPI.toFloat()
        }
    }

    /**
     * Apply spoofed metrics to a Resources object
     */
    fun applySpoofedMetrics(resources: Resources?) {
        resources?.let {
            it.displayMetrics.apply {
                widthPixels = TARGET_WIDTH
                heightPixels = TARGET_HEIGHT
                densityDpi = TARGET_DPI
                density = TARGET_DENSITY
                scaledDensity = TARGET_DENSITY
                xdpi = TARGET_DPI.toFloat()
                ydpi = TARGET_DPI.toFloat()
            }
        }
    }

    /**
     * Frame rate limiter - caps rendering to 60fps
     */
    class FrameRateLimiter {
        private val frameTimeNanos = 1_000_000_000L / TARGET_FPS
        private var lastFrameTime = System.nanoTime()
        private val choreographer = try {
            Choreographer.getInstance()
        } catch (e: Exception) {
            null
        }

        fun limitFrame() {
            val currentTime = System.nanoTime()
            val timeSinceLastFrame = currentTime - lastFrameTime

            if (timeSinceLastFrame < frameTimeNanos) {
                val sleepTime = (frameTimeNanos - timeSinceLastFrame) / 1_000_000L
                if (sleepTime > 0) {
                    try {
                        Thread.sleep(sleepTime)
                    } catch (e: InterruptedException) {
                        // Continue
                    }
                }
            }

            lastFrameTime = System.nanoTime()
        }

        fun getTargetFps(): Int = TARGET_FPS
    }

    /**
     * Get frame rate limiter instance
     */
    fun getFrameLimiter(): FrameRateLimiter = frameRateLimiter
}
