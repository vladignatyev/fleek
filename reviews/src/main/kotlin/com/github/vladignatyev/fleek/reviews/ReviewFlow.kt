package com.github.vladignatyev.fleek.reviews

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import com.google.android.play.core.review.ReviewManagerFactory

interface IReviewFlow {
    fun toggleReview()
}

class ReviewFlow(private val activity: Activity, parentView: View) {
    private var bottomSheet: ViewGroup = parentView.findViewById(R.id.bottomSheet)
    private var skipBtn: View = parentView.findViewById(R.id.skipBtn)
    private var ratingBar: RatingBar = parentView.findViewById(R.id.ratingBar)

    init {
        bottomSheet.post {
            bottomSheet.translationY = bottomSheet.height.toFloat() * 2.0f
        }
    }

    public fun clean() {
        val prefs = activity.getPreferences(Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_review_saved", false).apply()
    }

    private fun isReviewSaved(): Boolean {
        val prefs = activity.getPreferences(Context.MODE_PRIVATE)
        return prefs.getBoolean("is_review_saved", false)
    }

    private fun saveReviewed() {
        val prefs = activity.getPreferences(Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_review_saved", true).apply()
    }

    fun toggleReview() {
        if (isReviewSaved()) {
            return
        }

        skipBtn.setOnClickListener {
            saveReviewed()
            hideReviewUI()
        }

        ratingBar.setOnRatingBarChangeListener { _, rating, _ ->
            if (rating >= POSITIVE_THRESHOLD) {
                ReviewManagerFactory.create(activity).let { rm ->
                    val r = rm.requestReviewFlow()
                    r.addOnCompleteListener { reviewInfo ->
                        rm.launchReviewFlow(activity, reviewInfo.result)
                    }
                }
            }

            saveReviewed()
            hideReviewUI()
        }

        showReviewUI()
    }

    private fun showReviewUI() {
        bottomSheet.post {
            bottomSheet.visibility = View.VISIBLE
            bottomSheet.bringToFront()
            bottomSheet.animate()
                .translationY(0.0f)
                .setDuration(ANIMATION_DURATION)
                .start()
        }
    }

    private fun hideReviewUI() {
        bottomSheet.post {
            bottomSheet.animate()
                .translationY(bottomSheet.height.toFloat() * 2.0f)
                .setDuration(ANIMATION_DURATION)
                .start()
        }
    }

    companion object {
        const val POSITIVE_THRESHOLD = 4.0f
        const val ANIMATION_DURATION = 600L
    }
}