package com.github.vladignatyev.fleek.reviews

import android.app.Activity
import android.content.Context
import android.widget.FrameLayout
import android.widget.RatingBar
import android.view.View
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class ReviewFlowTest {

    private fun buildParent(activity: Activity): FrameLayout {
        val parent = FrameLayout(activity)
        val bottomSheet = FrameLayout(activity).apply { id = R.id.bottomSheet }
        val skipBtn = View(activity).apply { id = R.id.skipBtn }
        val ratingBar = RatingBar(activity).apply { id = R.id.ratingBar }
        bottomSheet.addView(skipBtn)
        bottomSheet.addView(ratingBar)
        parent.addView(bottomSheet)
        return parent
    }

    @Test
    fun cleanResetsSavedState() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val parent = buildParent(activity)
        val prefs = activity.getPreferences(Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_review_saved", true).apply()

        val flow = ReviewFlow(activity, parent)
        flow.clean()

        assertFalse(prefs.getBoolean("is_review_saved", true))
    }

    @Test
    fun toggleReviewSkipsWhenAlreadyReviewed() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val parent = buildParent(activity)
        val prefs = activity.getPreferences(Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_review_saved", true).apply()

        val flow = ReviewFlow(activity, parent)
        val skipBtn = parent.findViewById<View>(R.id.skipBtn)
        val ratingBar = parent.findViewById<RatingBar>(R.id.ratingBar)

        flow.toggleReview()

        assertNull(Shadows.shadowOf(skipBtn).onClickListener)
        assertNull(Shadows.shadowOf(ratingBar).onRatingBarChangeListener)
    }

    @Test
    fun toggleReviewSetsListenersAndSavesOnSkip() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val parent = buildParent(activity)
        val prefs = activity.getPreferences(Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_review_saved", false).apply()

        val flow = ReviewFlow(activity, parent)
        val skipBtn = parent.findViewById<View>(R.id.skipBtn)
        val ratingBar = parent.findViewById<RatingBar>(R.id.ratingBar)

        flow.toggleReview()

        assertNotNull(Shadows.shadowOf(skipBtn).onClickListener)
        assertNotNull(Shadows.shadowOf(ratingBar).onRatingBarChangeListener)

        skipBtn.performClick()

        assertTrue(prefs.getBoolean("is_review_saved", false))
    }
}
