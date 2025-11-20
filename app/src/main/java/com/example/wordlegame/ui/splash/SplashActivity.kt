package com.example.wordlegame.ui.splash

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.wordlegame.databinding.ActivitySplashBinding
import com.example.wordlegame.data.remote.AuthRepository
import com.example.wordlegame.ui.auth.LoginActivity
import com.example.wordlegame.ui.main.MainActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide action bar
        supportActionBar?.hide()

        startAnimations()

        lifecycleScope.launch {
            delay(3000) // Show splash for 3 seconds
            navigateToNextScreen()
        }
    }

    private fun startAnimations() {
        // Fade in WORDIX text
        binding.tvWordix.alpha = 0f
        binding.tvWordix.animate()
            .alpha(1f)
            .setDuration(1000)
            .start()

        // Scale animation
        binding.tvWordix.scaleX = 0.5f
        binding.tvWordix.scaleY = 0.5f
        binding.tvWordix.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(1000)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        // Tagline animation
        binding.tvTagline.alpha = 0f
        binding.tvTagline.translationY = 50f
        binding.tvTagline.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(1000)
            .setStartDelay(500)
            .start()

        // Progress bar animation
        binding.progressBar.alpha = 0f
        binding.progressBar.animate()
            .alpha(1f)
            .setDuration(500)
            .setStartDelay(1000)
            .start()
    }

    private fun navigateToNextScreen() {
        val currentUser = authRepository.getCurrentUser()
        val intent = if (currentUser != null) {
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, LoginActivity::class.java)
        }
        startActivity(intent)
        finish()

        // Add transition animation
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}