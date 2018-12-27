package com.sendbird.android.sample.main;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.sendbird.android.SendBird;
import com.sendbird.android.SendBirdException;
import com.sendbird.android.User;
import com.sendbird.android.sample.R;
import com.sendbird.android.sample.utils.PreferenceUtils;
import com.sendbird.android.sample.utils.PushUtils;

import androidx.appcompat.app.AppCompatActivity;

public class SplashScreenActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(mAuth.getCurrentUser() != null) {
            connectToSendBird(mAuth.getCurrentUser());
        } else {
            Intent intent = new Intent(SplashScreenActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
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
    private void connectToSendBird(final FirebaseUser currentUser) {
        SendBird.connect(currentUser.getUid(), new SendBird.ConnectHandler() {
            @Override
            public void onConnected(User user, SendBirdException e) {
                if (e != null) {
                    // Error!
                    Toast.makeText(
                            SplashScreenActivity.this, "" + e.getCode() + ": " + e.getMessage(),
                            Toast.LENGTH_SHORT)
                            .show();
                    PreferenceUtils.setConnected(SplashScreenActivity.this, false);
                    return;
                }

                PreferenceUtils.setUserId(SplashScreenActivity.this, mAuth.getCurrentUser().getUid());
                PreferenceUtils.setNickname(SplashScreenActivity.this, user.getNickname());
                PreferenceUtils.setProfileUrl(SplashScreenActivity.this, user.getProfileUrl());
                PreferenceUtils.setConnected(SplashScreenActivity.this, true);

                updateCurrentUserPushToken();

                // Proceed to MainActivity
                Intent intent = new Intent(SplashScreenActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    /**
     * Update the user's push token.
     */
    private void updateCurrentUserPushToken() {
        PushUtils.registerPushTokenForCurrentUser(SplashScreenActivity.this, null);
    }

}
