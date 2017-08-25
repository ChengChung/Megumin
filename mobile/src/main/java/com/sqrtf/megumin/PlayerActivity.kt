package com.sqrtf.megumin

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.sqrtf.common.SSLCompat.SSLSocketFactoryCompat
import com.sqrtf.common.SSLCompat.SSLVals
import com.sqrtf.common.activity.BaseActivity
import com.sqrtf.common.api.ApiHelper
import com.sqrtf.common.player.MeguminExoPlayer
import com.sqrtf.common.view.CheckableImageButton
import com.sqrtf.common.view.FastForwardBar

import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.X509TrustManager

class PlayerActivity : BaseActivity() {
    val playerController by lazy { findViewById(R.id.play_controller) }
    val playerView by lazy { findViewById(R.id.player_content) as MeguminExoPlayer }
    val root by lazy { findViewById(R.id.root) }

    val mHidePart2Runnable = Runnable {
        playerView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LOW_PROFILE or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }

    val mShowPart2Runnable = Runnable {
        supportActionBar?.show()
    }


    val mHideHandler = Handler()
    var lastPlayWhenReady = false
    var controllerVisibility = View.VISIBLE

    companion object {
        fun intent(context: Context, url: String, id: String, id2: String): Intent {
            val intent = Intent(context, PlayerActivity::class.java)
            intent.putExtra(INTENT_KEY_URL, url)
            intent.putExtra(RESULT_KEY_ID, id)
            intent.putExtra(RESULT_KEY_ID_2, id2)
            return intent
        }

        private val INTENT_KEY_URL = "INTENT_KEY_URL"
        private val UI_ANIMATION_DELAY = 100

        val RESULT_KEY_ID = "PlayerActivity:RESULT_KEY_ID"
        val RESULT_KEY_ID_2 = "PlayerActivity:RESULT_KEY_ID_2"
        val RESULT_KEY_POSITION = "PlayerActivity:RESULT_KEY_POSITION"
        val RESULT_KEY_DURATION = "PlayerActivity:RESULT_KEY_DURATION"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
//        playerView.postDelayed({
//            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
//        }, 1000)
//        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
//        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED

        val url = intent.getStringExtra(INTENT_KEY_URL)
        if (TextUtils.isEmpty(url)) throw IllegalArgumentException("Required url")

        val fixedUrl = Uri.encode(ApiHelper.fixHttpUrl(url), "@#&=*+-_.,:!?()/~'%")
        Log.i(this.localClassName, "playing:" + fixedUrl)

        checkMultiWindowMode()
        findViewById(R.id.play_close).setOnClickListener { onBackPressed() }
        (findViewById(R.id.fast_forward_bar) as FastForwardBar).callback = object : FastForwardBar.FastForwardEventCallback {
            override fun onFastForward(range: Int) {
                playerView.seekOffsetTo(range * 1000)
            }

            override fun onClick(view: View) {
                playerView.performClick()
            }

        }

        playerView.setControllerView(playerController,
                MeguminExoPlayer.ControllerViews(
                        findViewById(R.id.play_button) as CheckableImageButton,
                        findViewById(R.id.play_screen) as CheckableImageButton?,
                        findViewById(R.id.play_progress) as SeekBar,
                        findViewById(R.id.play_position) as TextView,
                        findViewById(R.id.play_duration) as TextView))

        playerView.setControllerCallback(object : MeguminExoPlayer.ControllerCallback {
            override fun onControllerVisibilityChange(visible: Boolean) {
                if (visible) {
                    show()
                } else {
                    hide()
                }
            }
        })

        val sslSocketFactory = SSLSocketFactoryCompat(SSLVals.trustAllCerts[0] as X509TrustManager)
        HttpsURLConnection.setDefaultSSLSocketFactory(sslSocketFactory)
        HttpsURLConnection.setDefaultHostnameVerifier(SSLVals.hostnameVerifier)

        val dataSourceFactory = DefaultDataSourceFactory(this, Util.getUserAgent(this, BuildConfig.APPLICATION_ID))
        val extractorsFactory = DefaultExtractorsFactory()
        val videoSource = ExtractorMediaSource(Uri.parse(fixedUrl), dataSourceFactory, extractorsFactory, null, null)

        playerView.setSource(videoSource)

        playerView.setPlayWhenReady(true)
        lastPlayWhenReady = true
    }

    private fun checkMultiWindowMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            root.fitsSystemWindows = isInMultiWindowMode
        }
    }

    override fun onBackPressed() {
        if (controllerVisibility == View.VISIBLE) {
            hide()
        } else {
            finish()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {

        if (controllerVisibility != View.VISIBLE) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                    || keyCode == KeyEvent.KEYCODE_ENTER
                    || keyCode == KeyEvent.KEYCODE_SPACE
                    || keyCode == KeyEvent.KEYCODE_DPAD_UP
                    || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                playerView.showController()
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                playerView.seekOffsetTo(-5 * 1000)
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                playerView.seekOffsetTo(5 * 1000)
            } else {
                return super.onKeyDown(keyCode, event)
            }
            return true
        } else {
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                    || keyCode == KeyEvent.KEYCODE_DPAD_UP
                    || keyCode == KeyEvent.KEYCODE_DPAD_DOWN
                    || keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                    || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                playerView.showController()
            } else if (keyCode == KeyEvent.KEYCODE_BACK) {
                playerView.dismissController()
                return true
            }
            return super.onKeyDown(keyCode, event)
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        playerView.setPlayWhenReady(lastPlayWhenReady)
    }

    override fun onStop() {
        super.onStop()
        lastPlayWhenReady = playerView.getPlayWhenReady()
        playerView.setPlayWhenReady(false)
    }

    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean) {
        super.onMultiWindowModeChanged(isInMultiWindowMode)
        checkMultiWindowMode()
    }

    override fun onDestroy() {
        super.onDestroy()
        playerView.setPlayWhenReady(false)
        playerView.release()
    }

    override fun finish() {
        val resultCode = if (playerView.player.currentPosition > 0) Activity.RESULT_OK else Activity.RESULT_CANCELED
        val i = intent
        i.putExtra(RESULT_KEY_POSITION, playerView.player.currentPosition)
        i.putExtra(RESULT_KEY_DURATION, playerView.player.duration)

        setResult(resultCode, i)
        super.finish()
    }

    private fun hide() {
        supportActionBar?.hide()
        controllerVisibility = View.INVISIBLE

        mHideHandler.removeCallbacks(mShowPart2Runnable)
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    private fun show() {
        playerView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        controllerVisibility = View.VISIBLE
        mHideHandler.removeCallbacks(mHidePart2Runnable)
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY.toLong())
    }
}
