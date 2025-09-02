package com.github.vladignatyev.fleek.reviews_example

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.vladignatyev.fleek.reviews.ReviewFlow
import com.github.vladignatyev.fleek.reviews_example.databinding.MainActivityBinding


class MainActivity : AppCompatActivity(R.layout.main_activity) {
    private lateinit var reviewFlow: ReviewFlow


    private lateinit var binding: MainActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupFleekReviews()
    }

    private fun setupFleekReviews() {
        reviewFlow = ReviewFlow(this, binding.root)

        reviewFlow.clean()  // Clean SharedPreferences state for demo.

        binding.toggleBtn.setOnClickListener {
            reviewFlow.toggleReview()
        }
    }
}
