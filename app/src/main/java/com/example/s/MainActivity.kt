package com.example.s

import android.content.Intent
import android.os.Bundle
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.ScaleAnimation
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.s.R

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

        btnExplore.setOnClickListener {
            startActivity(Intent(this, DLogin::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }
}