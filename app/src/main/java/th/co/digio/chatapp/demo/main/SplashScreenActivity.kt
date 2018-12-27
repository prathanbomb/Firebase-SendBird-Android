package th.co.digio.chatapp.demo.main

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import co.th.digio.chatapp.demo.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.sendbird.android.SendBird
import th.co.digio.chatapp.demo.utils.PreferenceUtils
import th.co.digio.chatapp.demo.utils.PushUtils

class SplashScreenActivity : AppCompatActivity() {

    private var mAuth: FirebaseAuth? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        mAuth = FirebaseAuth.getInstance()
    }

    override fun onStart() {
        super.onStart()
        if (mAuth!!.currentUser != null) {
            connectToSendBird(mAuth!!.currentUser!!)
        } else {
            val intent = Intent(this@SplashScreenActivity, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
        //        if(PreferenceUtils.getConnected(this)) {
        //            connectToSendBird(PreferenceUtils.getUserId(this));
        //        } else {
        //            // Proceed to MainActivity
        //            Intent intent = new Intent(SplashScreenActivity.this, LoginActivity.class);
        //            startActivity(intent);
        //            finish();
        //        }
    }

    /**
     * Attempts to connect a user to SendBird.
     * @param currentUser    The unique ID of the user.
     */
    private fun connectToSendBird(currentUser: FirebaseUser) {
        SendBird.connect(currentUser.uid, SendBird.ConnectHandler { user, e ->
            if (e != null) {
                // Error!
                Toast.makeText(
                        this@SplashScreenActivity, "" + e.code + ": " + e.message,
                        Toast.LENGTH_SHORT)
                        .show()
                PreferenceUtils.setConnected(this@SplashScreenActivity, false)
                return@ConnectHandler
            }

            PreferenceUtils.setUserId(this@SplashScreenActivity, mAuth!!.currentUser!!.uid)
            PreferenceUtils.setNickname(this@SplashScreenActivity, user.nickname)
            PreferenceUtils.setProfileUrl(this@SplashScreenActivity, user.profileUrl)
            PreferenceUtils.setConnected(this@SplashScreenActivity, true)

            updateCurrentUserPushToken()

            // Proceed to MainActivity
            val intent = Intent(this@SplashScreenActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        })
    }

    /**
     * Update the user's push token.
     */
    private fun updateCurrentUserPushToken() {
        PushUtils.registerPushTokenForCurrentUser(this@SplashScreenActivity, null)
    }

}
