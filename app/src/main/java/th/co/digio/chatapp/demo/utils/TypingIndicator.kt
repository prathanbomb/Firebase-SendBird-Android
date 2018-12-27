package th.co.digio.chatapp.demo.utils

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Build
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView

import androidx.annotation.RequiresApi

class TypingIndicator(internal var mImageViewList: List<ImageView>, private val mAnimDuration: Int) {

    private var mAnimSet: AnimatorSet? = null

    /**
     * Animates all dots in sequential order.
     */
    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    fun animate() {
        var startDelay = 0

        mAnimSet = AnimatorSet()

        for (i in mImageViewList.indices) {
            val dot = mImageViewList[i]
            //            ValueAnimator bounce = ObjectAnimator.ofFloat(dot, "y", mAnimMagnitude);
            val fadeIn = ObjectAnimator.ofFloat(dot, "alpha", 1f, 0.5f)
            val scaleX = ObjectAnimator.ofFloat(dot, "scaleX", 1f, 0.7f)
            val scaleY = ObjectAnimator.ofFloat(dot, "scaleY", 1f, 0.7f)

            fadeIn.duration = mAnimDuration.toLong()
            fadeIn.interpolator = AccelerateDecelerateInterpolator()
            fadeIn.repeatMode = ValueAnimator.REVERSE
            fadeIn.repeatCount = ValueAnimator.INFINITE

            scaleX.duration = mAnimDuration.toLong()
            scaleX.interpolator = AccelerateDecelerateInterpolator()
            scaleX.repeatMode = ValueAnimator.REVERSE
            scaleX.repeatCount = ValueAnimator.INFINITE

            scaleY.duration = mAnimDuration.toLong()
            scaleY.interpolator = AccelerateDecelerateInterpolator()
            scaleY.repeatMode = ValueAnimator.REVERSE
            scaleY.repeatCount = ValueAnimator.INFINITE

            //            bounce.setDuration(mAnimDuration);
            //            bounce.setInterpolator(new AccelerateDecelerateInterpolator());
            //            bounce.setRepeatMode(ValueAnimator.REVERSE);
            //            bounce.setRepeatCount(ValueAnimator.INFINITE);

            //            mAnimSet.play(bounce).after(startDelay);
            mAnimSet!!.play(fadeIn).after(startDelay.toLong())
            mAnimSet!!.play(scaleX).with(fadeIn)
            mAnimSet!!.play(scaleY).with(fadeIn)

            mAnimSet!!.startDelay = 500

            startDelay += mAnimDuration / (mImageViewList.size - 1)
        }

        mAnimSet!!.start()
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    fun stop() {

        if (mAnimSet == null) {
            return
        }

        mAnimSet!!.end()
    }

}
