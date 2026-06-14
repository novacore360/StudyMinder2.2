package com.studyminder.ui.rank

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.studyminder.R
import com.studyminder.data.model.RankSystem
import com.studyminder.data.repository.StudyRepository
import com.studyminder.databinding.FragmentRankBinding

class RankFragment : Fragment() {

    private var _binding: FragmentRankBinding? = null
    private val binding get() = _binding!!
    private lateinit var repo: StudyRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRankBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repo = StudyRepository.getInstance(requireContext())
        loadData()
    }

    private fun loadData() {
        val totalPoints = repo.getTotalDonePoints()
        val currentRank = RankSystem.getCurrentRank(totalPoints)
        val nextRank    = RankSystem.getNextRank(totalPoints)
        val pointsIn    = RankSystem.getPointsInCurrentRank(totalPoints)
        val pointsNeeded = RankSystem.getPointsRequiredForCurrentRank(totalPoints)

        // ── Header ──────────────────────────────────────────────────────────
        binding.tvCurrentRankLabel.text = currentRank.name
        binding.tvRankQuote.text        = "\"${currentRank.quote}\""
        binding.tvTotalPoints.text      = "Total: $totalPoints mark${if (totalPoints != 1) "s" else ""} as done"

        val rankIdx = RankSystem.ranks.indexOf(currentRank)
        binding.tvRankStars.text = when (currentRank.tier) {
            com.studyminder.data.model.RankTier.STAR     -> "⭐".repeat(currentRank.starCount)
            com.studyminder.data.model.RankTier.MOON     -> "🌕 Moon ${listOf("I","II","III","IV","V").getOrElse(rankIdx - 3) { "" }}"
            com.studyminder.data.model.RankTier.DIAMOND  -> "💎 Diamond ${listOf("I","II","III","IV").getOrElse(rankIdx - 8) { "" }}"
            com.studyminder.data.model.RankTier.CROWN    -> "👑 Crown"
            com.studyminder.data.model.RankTier.TRIDENT  -> "🔱 Trident"
            com.studyminder.data.model.RankTier.INFINITY -> "♾️ Infinity"
            com.studyminder.data.model.RankTier.COSMOS   -> "🌌 Cosmos"
        }

        // Progress
        if (nextRank != null) {
            binding.progressRank.max      = pointsNeeded
            binding.progressRank.progress = pointsIn
            binding.tvProgressText.text   = "$pointsIn / $pointsNeeded → ${nextRank.name}"
        } else {
            binding.progressRank.max      = 1
            binding.progressRank.progress = 1
            binding.tvProgressText.text   = "🏆 Maximum rank achieved!"
        }

        // Header badge
        try {
            val s   = requireContext().assets.open("ranks/${currentRank.assetFileName}")
            val bmp = BitmapFactory.decodeStream(s)
            binding.ivCurrentRankBadge.setImageBitmap(bmp)
            s.close()
        } catch (e: Exception) { /* ignore */ }

        binding.ivCurrentRankBadge.startAnimation(
            AnimationUtils.loadAnimation(requireContext(), R.anim.badge_float)
        )

        // ── Carousel ────────────────────────────────────────────────────────
        setupCarousel(totalPoints)

        // ── Grid ────────────────────────────────────────────────────────────
        binding.rvRanks.apply {
            layoutManager        = GridLayoutManager(requireContext(), 3)
            adapter              = RankBadgeAdapter(RankSystem.ranks, totalPoints)
            itemAnimator         = null
            isNestedScrollingEnabled = false
        }
    }

    private fun setupCarousel(totalPoints: Int) {
        val ranks = RankSystem.ranks

        binding.vpRanks.apply {
            adapter            = RankCarouselAdapter(ranks, totalPoints)
            offscreenPageLimit = 2   // keep neighbours rendered so they peek
            setPageTransformer(RankCarouselTransformer())

            // The inner RecyclerView inside ViewPager2 must NOT clip children
            // so that scaled-down side cards are visible in the padding zone
            (getChildAt(0) as? androidx.recyclerview.widget.RecyclerView)?.apply {
                overScrollMode = View.OVER_SCROLL_NEVER
                clipChildren   = false
                clipToPadding  = false
            }
        }

        // Jump straight to current rank
        val currentIdx = ranks.indexOfFirst {
            it.name == RankSystem.getCurrentRank(totalPoints).name
        }.coerceAtLeast(0)
        binding.vpRanks.setCurrentItem(currentIdx, false)

        // Build dot indicators
        buildDots(ranks.size, currentIdx)

        binding.vpRanks.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = updateDots(position)
        })
    }

    // ── Dot indicators ──────────────────────────────────────────────────────
    private val dotViews = mutableListOf<ImageView>()

    private fun buildDots(count: Int, selected: Int) {
        binding.llDots.removeAllViews()
        dotViews.clear()
        val dp4 = (4 * resources.displayMetrics.density).toInt()
        val dp8 = dp4 * 2

        // Only show up to 17 dots but scale them tiny so they fit
        val dotSizePx = if (count > 10) dp4 else dp8

        repeat(count) { i ->
            val dot = ImageView(requireContext()).apply {
                layoutParams = ViewGroup.MarginLayoutParams(dotSizePx, dotSizePx).also {
                    it.marginStart = dp4 / 2
                    it.marginEnd   = dp4 / 2
                }
                setImageDrawable(
                    if (i == selected)
                        ContextCompat.getDrawable(requireContext(), R.drawable.dot_current_rank)
                    else
                        ContextCompat.getDrawable(requireContext(), R.drawable.dot_inactive_rank)
                )
            }
            binding.llDots.addView(dot)
            dotViews.add(dot)
        }
    }

    private fun updateDots(selected: Int) {
        dotViews.forEachIndexed { i, dot ->
            dot.setImageDrawable(
                if (i == selected)
                    ContextCompat.getDrawable(requireContext(), R.drawable.dot_current_rank)
                else
                    ContextCompat.getDrawable(requireContext(), R.drawable.dot_inactive_rank)
            )
        }
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
