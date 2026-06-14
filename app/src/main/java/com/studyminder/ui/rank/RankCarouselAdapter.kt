package com.studyminder.ui.rank

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView
import com.studyminder.R
import com.studyminder.data.model.RankInfo
import com.studyminder.data.model.RankSystem
import com.studyminder.databinding.ItemRankCarouselBinding

class RankCarouselAdapter(
    private val ranks: List<RankInfo>,
    private val totalPoints: Int
) : RecyclerView.Adapter<RankCarouselAdapter.VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemRankCarouselBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun getItemCount() = ranks.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(ranks[position], position)
    }

    inner class VH(private val b: ItemRankCarouselBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(rank: RankInfo, position: Int) {
            val unlocked = totalPoints >= rank.requiredPoints
            val isCurrent = RankSystem.getCurrentRank(totalPoints).name == rank.name

            b.tvCarouselName.text = rank.name
            b.tvCarouselQuote.text = "\"${rank.quote}\""
            b.tvCarouselPoints.text = if (unlocked) "✓ Unlocked" else "${rank.requiredPoints} pts"

            // Lock
            b.viewCarouselLock.visibility = if (unlocked) View.GONE else View.VISIBLE
            b.ivCarouselLockIcon.visibility = if (unlocked) View.GONE else View.VISIBLE

            // Badge image
            try {
                val stream = b.root.context.assets.open("ranks/${rank.assetFileName}")
                val bmp = BitmapFactory.decodeStream(stream)
                b.ivCarouselBadge.setImageBitmap(bmp)
                stream.close()
            } catch (e: Exception) { /* ignore */ }

            b.ivCarouselBadge.alpha = if (unlocked) 1f else 0.2f

            // Overflow for Sovereign+
            if (rank.isOverflowBadge) {
                val offset = b.root.context.resources.getDimension(R.dimen.badge_overflow_offset)
                b.ivCarouselBadge.translationY = -offset
                b.carouselBadgeContainer.clipChildren = false
                b.carouselBadgeContainer.clipToPadding = false
                b.root.clipChildren = false
            } else {
                b.ivCarouselBadge.translationY = 0f
            }

            // Current rank
            b.tvCarouselCurrentLabel.visibility = if (isCurrent) View.VISIBLE else View.GONE
            b.cardCarousel.cardElevation = when {
                isCurrent -> 18f
                unlocked  -> 8f
                else      -> 3f
            }

            // Float animation on current
            if (isCurrent) {
                val floatAnim = AnimationUtils.loadAnimation(b.root.context, R.anim.badge_float)
                b.ivCarouselBadge.startAnimation(floatAnim)
            } else {
                b.ivCarouselBadge.clearAnimation()
            }

            // Shine sweep
            if (unlocked) {
                b.viewCarouselShine.visibility = View.VISIBLE
                val shine = AnimationUtils.loadAnimation(b.root.context, R.anim.badge_shine)
                shine.startOffset = (position * 350L) % 4000L
                b.viewCarouselShine.startAnimation(shine)
            } else {
                b.viewCarouselShine.clearAnimation()
                b.viewCarouselShine.visibility = View.GONE
            }
        }
    }
}
