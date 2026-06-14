package com.studyminder.ui.rank

import android.view.View
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

/**
 * Carousel effect:
 *  - Center (position 0)  → scale 1.0, full alpha, z=30 (pops forward)
 *  - Adjacent (position ±1) → scale 0.82, 55% alpha, z=0 (sinks back, peeks at sides)
 *  - Far (|pos| > 1)     → scale 0.68, 25% alpha
 *
 *  translationX nudges side cards inward so they actually peek into the viewport
 *  even though ViewPager2 clips them by default. Combined with
 *  offscreenPageLimit=2 + negative marginHorizontal on the ViewPager, this
 *  gives the classic "cards stack" carousel look.
 */
class RankCarouselTransformer : ViewPager2.PageTransformer {

    override fun transformPage(page: View, position: Float) {
        val absPos = abs(position)
        page.apply {
            when {
                absPos >= 1f -> {
                    // Off to the side — shrink hard, very dim
                    scaleX       = lerp(SIDE_SCALE, FAR_SCALE, (absPos - 1f).coerceIn(0f, 1f))
                    scaleY       = scaleX
                    alpha        = FAR_ALPHA
                    translationZ = 0f
                    // Nudge cards inward so they peek into the viewport
                    val nudge    = page.width * 0.18f
                    translationX = if (position > 0) -nudge else nudge
                }
                else -> {
                    // Transitioning between center and side
                    val scale    = lerp(CENTER_SCALE, SIDE_SCALE, absPos)
                    scaleX       = scale
                    scaleY       = scale
                    alpha        = lerp(CENTER_ALPHA, SIDE_ALPHA, absPos)
                    translationZ = lerp(30f, 0f, absPos)
                    // Reset nudge as it approaches center
                    val nudge    = page.width * 0.18f * absPos
                    translationX = if (position > 0) -nudge else nudge
                }
            }
        }
    }

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

    companion object {
        private const val CENTER_SCALE = 1.00f
        private const val SIDE_SCALE   = 0.82f
        private const val FAR_SCALE    = 0.68f
        private const val CENTER_ALPHA = 1.00f
        private const val SIDE_ALPHA   = 0.55f
        private const val FAR_ALPHA    = 0.25f
    }
}
