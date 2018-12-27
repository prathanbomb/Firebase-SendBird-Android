package th.co.digio.chatapp.demo.main

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.widget.ContentLoadingProgressBar
import co.th.digio.chatapp.demo.R
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginResult
import com.facebook.login.widget.LoginButton
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.sendbird.android.SendBird
import th.co.digio.chatapp.demo.utils.PreferenceUtils
import th.co.digio.chatapp.demo.utils.PushUtils

class LoginActivity : AppCompatActivity() {
    private var mAuth: FirebaseAuth? = null
    private var mCallbackManager: CallbackManager? = null
    private var mLoginLayout: CoordinatorLayout? = null
    private var mUserEmailEditText: TextInputEditText? = null
    private var mUserPasswordEditText: TextInputEditText? = null
    private var mButtonLogin: Button? = null
    private var mButtonRegister: Button? = null
    private var mGoogleLogin: Button? = null
    private var mFacebookLogin: Button? = null
    private var loginButton: LoginButton? = null
    private var mProgressBar: ContentLoadingProgressBar? = null
    private var mGoogleSignInClient: GoogleSignInClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        mAuth = FirebaseAuth.getInstance()

        mLoginLayout = findViewById(R.id.layout_login)

        mUserEmailEditText = findViewById(R.id.edittext_email)
        mUserPasswordEditText = findViewById(R.id.edittext_password)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        //        mUserEmailEditText.setText(PreferenceUtils.getUserId(this));
        //        mUserPasswordEditText.setText(PreferenceUtils.getNickname(this));

        mButtonLogin = findViewById(R.id.button_login_connect)
        mButtonLogin!!.setOnClickListener {
            val email = mUserEmailEditText!!.text!!.toString()
            val password = mUserPasswordEditText!!.text!!.toString()
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this@LoginActivity, "Email and password can't be empty",
                        Toast.LENGTH_SHORT).show()
            } else {
                showProgressBar(true)
                signInWithEmail(email, password)
            }
        }

        mButtonRegister = findViewById(R.id.button_register)
        mButtonRegister!!.setOnClickListener {
            // Proceed to RegisterActivity
            val intent = Intent(this@LoginActivity, RegisterActivity::class.java)
            startActivity(intent)
            finish()
        }

        // A loading indicator
        mProgressBar = findViewById(R.id.progress_bar_login)

        mGoogleLogin = findViewById(R.id.btn_google)
        mGoogleLogin!!.setOnClickListener { signInWithGoogle() }

        // Initialize Facebook Login button
        mCallbackManager = CallbackManager.Factory.create()
        mFacebookLogin = findViewById(R.id.btn_facebook)
        loginButton = findViewById(R.id.login_button)
        loginButton!!.setReadPermissions("email", "public_profile")
        loginButton!!.registerCallback(mCallbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(loginResult: LoginResult) {
                Log.d(TAG, "facebook:onSuccess:$loginResult")
                handleFacebookAccessToken(loginResult.accessToken)
            }

            override fun onCancel() {
                Log.d(TAG, "facebook:onCancel")
                // ...
            }

            override fun onError(error: FacebookException) {
                Log.d(TAG, "facebook:onError", error)
                // ...
            }
        })
        mFacebookLogin!!.setOnClickListener { loginButton!!.callOnClick() }

    }

    private fun signInWithGoogle() {
        val signInIntent = mGoogleSignInClient!!.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    private fun signInWithEmail(email: String, password: String) {
        mAuth!!.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    showProgressBar(false)
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "Authentication Succeeded")
                        val user = mAuth!!.currentUser
                        connectToSendBird(user!!.uid, user.displayName)
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "signInWithEmail:failure", task.exception)
                        Toast.makeText(this@LoginActivity, task.exception!!.message,
                                Toast.LENGTH_SHORT).show()
                    }
                }
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.id!!)
        showProgressBar(true)
        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        mAuth!!.signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "signInWithCredential:success")
                        val user = mAuth!!.currentUser
                        connectToSendBird(user!!.uid, user.displayName)
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "signInWithCredential:failure", task.exception)
                        Snackbar.make(findViewById(R.id.layout_login), "Authentication Failed.", Snackbar.LENGTH_SHORT).show()
                    }
                }
    }

    private fun handleFacebookAccessToken(token: AccessToken) {
        Log.d(TAG, "handleFacebookAccessToken:$token")
        showProgressBar(true)
        val credential = FacebookAuthProvider.getCredential(token.token)
        mAuth!!.signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "signInWithCredential:success")
                        val user = mAuth!!.currentUser
                        connectToSendBird(user!!.uid, user.displayName)
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "signInWithCredential:failure", task.exception)
                        Toast.makeText(this@LoginActivity, "Authentication failed.",
                                Toast.LENGTH_SHORT).show()
                    }
                }
    }


    override fun onStart() {
        super.onStart()
        //        if(PreferenceUtils.getConnected(this)) {
        //            connectToSendBird(PreferenceUtils.getUserId(this), PreferenceUtils.getNickname(this));
        //        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        mCallbackManager!!.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account!!)
            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e)
                // ...
            }

        }
    }

    /**
     * Attempts to connect a user to SendBird.
     * @param userId    The unique ID of the user.
     * @param userNickname  The user's nickname, which will be displayed in chats.
     */
    private fun connectToSendBird(userId: String, userNickname: String?) {
        // Show the loading indicator
        mButtonLogin!!.isEnabled = false

        SendBird.connect(userId, SendBird.ConnectHandler { user, e ->
            // Callback received; hide the progress bar.
            showProgressBar(false)

            if (e != null) {
                // Error!
                Toast.makeText(
                        this@LoginActivity, "" + e.code + ": " + e.message,
                        Toast.LENGTH_SHORT)
                        .show()

                // Show login failure snackbar
                showSnackbar("Login to SendBird failed")
                mButtonLogin!!.isEnabled = true
                PreferenceUtils.setConnected(this@LoginActivity, false)
                return@ConnectHandler
            }

            PreferenceUtils.setUserId(this@LoginActivity, mAuth!!.currentUser!!.uid)
            PreferenceUtils.setNickname(this@LoginActivity, user.nickname)
            PreferenceUtils.setProfileUrl(this@LoginActivity, user.profileUrl)
            PreferenceUtils.setConnected(this@LoginActivity, true)

            // Update the user's nickname
            updateCurrentUserInfo(userNickname)
            updateCurrentUserPushToken()

            // Proceed to MainActivity
            val intent = Intent(this@LoginActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        })
    }

    /**
     * Update the user's push token.
     */
    private fun updateCurrentUserPushToken() {
        PushUtils.registerPushTokenForCurrentUser(this@LoginActivity, null)
    }

    /**
     * Updates the user's nickname.
     * @param userNickname  The new nickname of the user.
     */
    private fun updateCurrentUserInfo(userNickname: String?) {
        SendBird.updateCurrentUserInfo(userNickname, null, SendBird.UserInfoUpdateHandler { e ->
            if (e != null) {
                // Error!
                Toast.makeText(
                        this@LoginActivity, "" + e.code + ":" + e.message,
                        Toast.LENGTH_SHORT)
                        .show()

                // Show update failed snackbar
                showSnackbar("Update user nickname failed")

                return@UserInfoUpdateHandler
            }

            PreferenceUtils.setNickname(this@LoginActivity, userNickname!!)
        })
    }

    // Displays a Snackbar from the bottom of the screen
    private fun showSnackbar(text: String) {
        val snackbar = Snackbar.make(mLoginLayout!!, text, Snackbar.LENGTH_SHORT)

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
        private const val RC_SIGN_IN = 123
        private const val TAG = "LoginActivity"
    }
}
