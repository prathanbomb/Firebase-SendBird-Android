package th.co.digio.chatapp.demo.main

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.widget.ContentLoadingProgressBar
import co.th.digio.chatapp.demo.R
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.sendbird.android.SendBird
import th.co.digio.chatapp.demo.utils.PreferenceUtils
import th.co.digio.chatapp.demo.utils.PushUtils

class RegisterActivity : AppCompatActivity() {
    private var mAuth: FirebaseAuth? = null
    private var mRegisterLayout: CoordinatorLayout? = null
    private var mEditTextEmail: EditText? = null
    private var mEditTextPassword: EditText? = null
    private var mEditTextReTypePassword: EditText? = null
    private var mEditTextNickname: EditText? = null
    private var mProgressBar: ContentLoadingProgressBar? = null
    private var mCancelButton: Button? = null
    private var mRegisterButton: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        mRegisterLayout = findViewById(R.id.layout_register)
        mEditTextEmail = findViewById(R.id.edittext_email)
        mEditTextPassword = findViewById(R.id.edittext_password)
        mEditTextReTypePassword = findViewById(R.id.edittext_retype_password)
        mEditTextNickname = findViewById(R.id.edittext_nickname)
        mCancelButton = findViewById(R.id.button_cancel)
        mRegisterButton = findViewById(R.id.button_register)
        mProgressBar = findViewById(R.id.progress_bar_register)

        mAuth = FirebaseAuth.getInstance()

        mCancelButton!!.setOnClickListener {
            val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
        mRegisterButton!!.setOnClickListener {
            if (!mEditTextEmail!!.text.toString().isEmpty()
                    && !mEditTextPassword!!.text.toString().isEmpty()
                    && !mEditTextReTypePassword!!.text.toString().isEmpty()
                    && !mEditTextNickname!!.text.toString().isEmpty()) {
                if (mEditTextPassword!!.text.toString() == mEditTextReTypePassword!!.text.toString()) {
                    createUser(mEditTextEmail!!.text.toString(), mEditTextPassword!!.text.toString())
                } else {
                    Toast.makeText(this@RegisterActivity, "Re-type password must match.",
                            Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this@RegisterActivity, "Please complete form.",
                        Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createUser(email: String, password: String) {
        showProgressBar(true)
        mAuth!!.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "createUserWithEmail:success")
                        val user = mAuth!!.currentUser
                        connectToSendBird(user!!.uid, mEditTextNickname!!.text.toString())
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "createUserWithEmail:failure", task.exception)
                        Toast.makeText(this@RegisterActivity, task.exception!!.message,
                                Toast.LENGTH_SHORT).show()
                        showProgressBar(false)
                    }
                }
    }

    override fun onBackPressed() {
        val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    /**
     * Attempts to connect a user to SendBird.
     * @param userId    The unique ID of the user.
     * @param userNickname  The user's nickname, which will be displayed in chats.
     */
    private fun connectToSendBird(userId: String, userNickname: String) {
        // Show the loading indicator
        mRegisterLayout!!.isEnabled = false

        SendBird.connect(userId, SendBird.ConnectHandler { user, e ->
            // Callback received; hide the progress bar.
            showProgressBar(false)

            if (e != null) {
                // Error!
                Toast.makeText(
                        this@RegisterActivity, "" + e.code + ": " + e.message,
                        Toast.LENGTH_SHORT)
                        .show()

                // Show login failure snackbar
                showSnackbar("Login to SendBird failed")
                mRegisterLayout!!.isEnabled = true
                PreferenceUtils.setConnected(this@RegisterActivity, false)
                return@ConnectHandler
            }

            PreferenceUtils.setUserId(this@RegisterActivity, mAuth!!.currentUser!!.uid)
            PreferenceUtils.setNickname(this@RegisterActivity, userNickname)
            PreferenceUtils.setProfileUrl(this@RegisterActivity, user.profileUrl)
            PreferenceUtils.setConnected(this@RegisterActivity, true)

            // Update the user's nickname
            updateCurrentUserInfo(userNickname)
            updateCurrentUserPushToken()

            // Proceed to MainActivity
            val intent = Intent(this@RegisterActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        })
    }

    /**
     * Update the user's push token.
     */
    private fun updateCurrentUserPushToken() {
        PushUtils.registerPushTokenForCurrentUser(this@RegisterActivity, null)
    }

    /**
     * Updates the user's nickname.
     * @param userNickname  The new nickname of the user.
     */
    private fun updateCurrentUserInfo(userNickname: String) {
        SendBird.updateCurrentUserInfo(userNickname, null, SendBird.UserInfoUpdateHandler { e ->
            if (e != null) {
                // Error!
                Toast.makeText(
                        this@RegisterActivity, "" + e.code + ":" + e.message,
                        Toast.LENGTH_SHORT)
                        .show()

                // Show update failed snackbar
                showSnackbar("Update user nickname failed")

                return@UserInfoUpdateHandler
            }

            PreferenceUtils.setNickname(this@RegisterActivity, userNickname)
        })
    }

    // Displays a Snackbar from the bottom of the screen
    private fun showSnackbar(text: String) {
        val snackbar = Snackbar.make(mRegisterLayout!!, text, Snackbar.LENGTH_SHORT)

        snackbar.show()
    }

    // Shows or hides the ProgressBar
    private fun showProgressBar(show: Boolean) {
        if (show) {
            mProgressBar!!.show()
        } else {
            mProgressBar!!.hide()
        }
    }

    companion object {

        private val TAG = "RegisterActivity"
    }

}
