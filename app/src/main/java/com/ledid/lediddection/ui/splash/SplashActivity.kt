package com.ledid.lediddection.ui.splash

import android.annotation.SuppressLint
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import com.ledid.lediddection.BuildConfig
import com.ledid.lediddection.R
import com.ledid.lediddection.activity.MainActivity
import com.ledid.lediddection.base.BaseActivity
import com.ledid.lediddection.databinding.LedSplashActivityBinding
import com.ledid.lediddection.extensions.runDelay

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity<LedSplashActivityBinding>() {
    override fun getLayoutRes() = R.layout.led_splash_activity
    override fun setup() {
        setFullScreen()

        runDelay(800) {
            val mAnimation = AnimationUtils.loadAnimation(this@SplashActivity, R.anim.fade_in)
            binding.tvBy.text = getString(R.string.txt_version, BuildConfig.VERSION_NAME)
            binding.tvScanPromotion.visibility = View.VISIBLE
            binding.tvScanPromotion.startAnimation(mAnimation)
        }

        val animation = AnimationUtils.loadAnimation(this, R.anim.tmty_splash)
        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(p0: Animation?) {
            }

            override fun onAnimationEnd(p0: Animation?) {
                runDelay(1000) {
                    MainActivity.start(this@SplashActivity)
                    finish()
                }
            }

            override fun onAnimationRepeat(p0: Animation?) {
            }
        })
        binding.imgLogo.startAnimation(animation)
    }

    fun setFullScreen() {
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }
    }
}