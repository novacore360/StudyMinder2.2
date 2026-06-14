package com.studyminder.util

import android.graphics.Color
import android.graphics.drawable.GradientDrawable

/**
 * Helper for the per-subject color picker.
 *
 * Hue range is restricted to avoid red (~0deg / 360deg) and green (~120deg),
 * since those colors are reserved for the "Missed" and "Done" status indicators.
 *
 * The usable hue range loops through:
 *   30 (orange) -> 60 (yellow) -> 90 (yellow-green, kept short) -> 150 (spring green edge)
 *   -> 180 (cyan) -> 240 (blue) -> 300 (magenta) -> 330 (pink) -> back to 30 (orange)
 *
 * To keep things simple and safe, we use two contiguous safe arcs:
 *   Arc A: 20  -> 100   (orange, yellow, yellow-green)   -- avoids red (0) and pure green (120)
 *   Arc B: 140 -> 380   (mint, cyan, blue, purple, magenta, pink, back to orange ~20)
 *
 * We concatenate Arc A and Arc B into a single gradient so the seek bar feels continuous
 * while never landing on red or green.
 */
object SubjectColors {

    /** Hue stops (in degrees) defining the gradient, in order. Red (0/360) and green (120) are skipped. */
    private val HUE_STOPS = listOf(
        20f,   // orange
        50f,   // amber/yellow
        90f,   // yellow-green (chartreuse, distinct from pure green)
        150f,  // mint / spring green
        180f,  // cyan
        210f,  // sky blue
        240f,  // blue
        270f,  // indigo
        300f,  // purple
        330f,  // magenta/pink
        380f   // wraps back toward orange (20 + 360)
    )

    private const val SATURATION = 0.55f
    private const val LIGHTNESS = 0.50f

    /**
     * Builds a [GradientDrawable] spanning the safe hue range, suitable for use as a
     * SeekBar background to visually represent the pickable color spectrum.
     */
    fun createGradientDrawable(): GradientDrawable {
        val colors = HUE_STOPS.map { hueToColor(it) }.toIntArray()
        return GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors).apply {
            cornerRadius = 18f
        }
    }

    /** Preset swatches users can tap for a quick pick. None of these are red or green. */
    val PRESET_COLORS: List<String> = listOf(
        "#FF9800", // orange
        "#FFC107", // amber
        "#CDDC39", // lime (yellow-green, not pure green)
        "#26A69A", // teal
        "#00BCD4", // cyan
        "#2196F3", // blue
        "#3F51B5", // indigo
        "#9C27B0", // purple
        "#E91E63", // pink
        "#795548"  // brown
    )

    /**
     * Converts a SeekBar progress (0..maxProgress) into a hex color string along the safe gradient.
     */
    fun progressToColor(progress: Int, maxProgress: Int): String {
        val fraction = progress.toFloat() / maxProgress.toFloat()
        val totalSegments = HUE_STOPS.size - 1
        val pos = fraction * totalSegments
        val segment = pos.toInt().coerceIn(0, totalSegments - 1)
        val segFraction = pos - segment

        val hueStart = HUE_STOPS[segment]
        val hueEnd = HUE_STOPS[segment + 1]
        var hue = hueStart + (hueEnd - hueStart) * segFraction
        if (hue >= 360f) hue -= 360f

        return colorToHex(hueToColor(hue))
    }

    /**
     * Finds the closest SeekBar progress value (0..maxProgress) for a given hex color.
     * Used to restore the seek bar position when editing an existing subject.
     */
    fun colorToProgress(hex: String, maxProgress: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(parseHexSafe(hex), hsv)
        var hue = hsv[0]

        // Find the closest point along HUE_STOPS
        var bestDist = Float.MAX_VALUE
        var bestFraction = 0f
        val totalSegments = HUE_STOPS.size - 1

        for (segment in 0 until totalSegments) {
            val hueStart = HUE_STOPS[segment]
            val hueEnd = HUE_STOPS[segment + 1]
            val span = hueEnd - hueStart
            // sample along this segment
            val steps = 20
            for (i in 0..steps) {
                val segFraction = i.toFloat() / steps
                var sampleHue = hueStart + span * segFraction
                if (sampleHue >= 360f) sampleHue -= 360f
                var dist = Math.abs(sampleHue - hue)
                if (dist > 180f) dist = 360f - dist
                if (dist < bestDist) {
                    bestDist = dist
                    bestFraction = (segment + segFraction) / totalSegments
                }
            }
        }
        return (bestFraction * maxProgress).toInt().coerceIn(0, maxProgress)
    }

    /**
     * Ensures the given hex color is not within the reserved red or green ranges.
     * If it falls in a reserved range, nudges it to the nearest safe hue.
     */
    fun sanitizeColor(hex: String): String {
        val hsv = FloatArray(3)
        Color.colorToHSV(parseHexSafe(hex), hsv)
        val hue = hsv[0]

        val isRed = hue < 15f || hue > 345f
        val isGreen = hue in 100f..140f

        if (!isRed && !isGreen) return colorToHex(parseHexSafe(hex))

        // Nudge to nearest safe hue
        val safeHue = when {
            isRed -> 20f   // push toward orange
            else -> if (hue < 120f) 90f else 150f // push toward yellow-green or mint
        }
        return colorToHex(hueToColor(safeHue))
    }

    private fun hueToColor(hue: Float): Int {
        val normalizedHue = if (hue >= 360f) hue - 360f else hue
        return Color.HSVToColor(floatArrayOf(normalizedHue, SATURATION, LIGHTNESS + 0.35f))
    }

    private fun colorToHex(color: Int): String {
        return String.format("#%06X", 0xFFFFFF and color)
    }

    private fun parseHexSafe(hex: String): Int {
        return try {
            Color.parseColor(hex)
        } catch (e: Exception) {
            Color.parseColor("#FF9800")
        }
    }
}
