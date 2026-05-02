package com.example.s

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.ScaleAnimation
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val imgLogo = findViewById<ImageView>(R.id.imgLogoSplash)
        val tvAppName = findViewById<TextView>(R.id.tvAppNameSplash)
        val btnExplore = findViewById<Button>(R.id.btnExploreDriver)

        val fadeIn = AlphaAnimation(0f, 1f).apply { duration = 800 }
        val scaleIn = ScaleAnimation(
            0.5f, 1f, 0.5f, 1f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f
        ).apply { duration = 800 }

        val animSet = AnimationSet(true).apply {
            addAnimation(fadeIn)
            addAnimation(scaleIn)
        }

        imgLogo.startAnimation(animSet)
        tvAppName.startAnimation(fadeIn)

        // If user is already authenticated via Firebase, skip to dashboard
        if (FirebaseManager.isLoggedIn) {
            Handler(Looper.getMainLooper()).postDelayed({
                startActivity(Intent(this, DriverDashboard::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            }, 1500) // Show splash for 1.5s even if logged in
            return
        }

        btnExplore.setOnClickListener {
            startActivity(Intent(this, DLogin::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }
}