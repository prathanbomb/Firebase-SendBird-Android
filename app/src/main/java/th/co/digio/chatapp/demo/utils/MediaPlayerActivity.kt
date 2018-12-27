/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package th.co.digio.chatapp.demo.utils

import android.app.Activity
import android.content.res.Configuration
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaPlayer.*
import android.os.Build
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewTreeObserver
import android.widget.ProgressBar
import co.th.digio.chatapp.demo.R

class MediaPlayerActivity : Activity(), OnBufferingUpdateListener, OnCompletionListener, OnPreparedListener, OnVideoSizeChangedListener, SurfaceHolder.Callback {
    private var mMediaPlayer: MediaPlayer? = null
    private var mSurfaceView: SurfaceView? = null
    private var mSurfaceHolder: SurfaceHolder? = null
    private var mUrl: String? = null
    private var mName: String? = null

    private var mProgressBar: ProgressBar? = null
    private var mContainer: View? = null

    private var mVideoWidth: Int = 0
    private var mVideoHeight: Int = 0

    private var mIsVideoReadyToBePlayed = false
    private var mIsVideoSizeKnown = false
    private var mIsContainerSizeKnown = false

    private var mIsPaused = false
    private var mCurrentPosition = -1

    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        setContentView(R.layout.activity_media_player)

        mSurfaceView = findViewById(R.id.surface)
        mProgressBar = findViewById(R.id.progress_bar)
        mSurfaceHolder = mSurfaceView!!.holder
        mSurfaceHolder!!.addCallback(this)
        mSurfaceHolder!!.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)

        val extras = intent.extras
        mUrl = extras!!.getString("url")
        mName = extras.getString("name")

        mProgressBar!!.visibility = View.VISIBLE
        initToolbar()
    }

    private fun initToolbar() {
        mContainer = findViewById(R.id.layout_media_player)
        setContainerLayoutListener(false)
    }

    private fun setContainerLayoutListener(screenRotated: Boolean) {
        mContainer!!.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (Build.VERSION.SDK_INT >= 16) {
                    mContainer!!.viewTreeObserver.removeOnGlobalLayoutListener(this)
                } else {
                    mContainer!!.viewTreeObserver.removeGlobalOnLayoutListener(this)
                }

                mIsContainerSizeKnown = true
                if (screenRotated) {
                    setVideoSize()
                } else {
                    tryToStartVideoPlayback()
                }
            }
        })
    }

    private fun playVideo() {
        mProgressBar!!.visibility = View.VISIBLE
        doCleanUp()
        try {
            mMediaPlayer = MediaPlayer()
            mMediaPlayer!!.setDataSource(mUrl)
            mMediaPlayer!!.setDisplay(mSurfaceHolder)
            mMediaPlayer!!.prepareAsync()
            mMediaPlayer!!.setOnBufferingUpdateListener(this)
            mMediaPlayer!!.setOnCompletionListener(this)
            mMediaPlayer!!.setOnPreparedListener(this)
            mMediaPlayer!!.setOnVideoSizeChangedListener(this)
            mMediaPlayer!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    override fun onBufferingUpdate(mp: MediaPlayer, percent: Int) {}

    override fun onCompletion(mp: MediaPlayer) {
        finish()
    }

    override fun onPrepared(mediaplayer: MediaPlayer) {
        mIsVideoReadyToBePlayed = true
        tryToStartVideoPlayback()
    }

    override fun onVideoSizeChanged(mp: MediaPlayer, width: Int, height: Int) {
        if (width == 0 || height == 0) {
            return
        }

        mVideoWidth = width
        mVideoHeight = height
        mIsVideoSizeKnown = true
        tryToStartVideoPlayback()
    }

    override fun surfaceChanged(surfaceholder: SurfaceHolder, i: Int, j: Int, k: Int) {}

    override fun surfaceDestroyed(surfaceholder: SurfaceHolder) {}

    override fun surfaceCreated(holder: SurfaceHolder) {
        playVideo()
    }

    override fun onPause() {
        super.onPause()
        if (mMediaPlayer!!.isPlaying) {
            mMediaPlayer!!.pause()
            mCurrentPosition = mMediaPlayer!!.currentPosition
            mIsPaused = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseMediaPlayer()
        doCleanUp()
    }

    private fun releaseMediaPlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer!!.release()
            mMediaPlayer = null
        }
    }

    private fun doCleanUp() {
        mVideoWidth = 0
        mVideoHeight = 0

        mIsVideoReadyToBePlayed = false
        mIsVideoSizeKnown = false
    }

    private fun tryToStartVideoPlayback() {
        if (mIsVideoReadyToBePlayed && mIsVideoSizeKnown && mIsContainerSizeKnown) {
            startVideoPlayback()
        }
    }

    private fun startVideoPlayback() {
        mProgressBar!!.visibility = View.INVISIBLE
        if (!mMediaPlayer!!.isPlaying) {
            mSurfaceHolder!!.setFixedSize(mVideoWidth, mVideoHeight)
            setVideoSize()

            if (mIsPaused) {
                mMediaPlayer!!.seekTo(mCurrentPosition)
                mIsPaused = false
            }
            mMediaPlayer!!.start()
        }
    }

    private fun setVideoSize() {
        try {
            val videoWidth = mMediaPlayer!!.videoWidth
            val videoHeight = mMediaPlayer!!.videoHeight
            val videoProportion = videoWidth.toFloat() / videoHeight.toFloat()

            val videoWidthInContainer = mContainer!!.width
            val videoHeightInContainer = mContainer!!.height
            val videoInContainerProportion = videoWidthInContainer.toFloat() / videoHeightInContainer.toFloat()

            val lp = mSurfaceView!!.layoutParams
            if (videoProportion > videoInContainerProportion) {
                lp.width = videoWidthInContainer
                lp.height = (videoWidthInContainer.toFloat() / videoProportion).toInt()
            } else {
                lp.width = (videoProportion * videoHeightInContainer.toFloat()).toInt()
                lp.height = videoHeightInContainer
            }
            mSurfaceView!!.layoutParams = lp
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setContainerLayoutListener(true)
    }
}