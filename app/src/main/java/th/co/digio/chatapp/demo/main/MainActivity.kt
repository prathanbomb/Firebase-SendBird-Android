package th.co.digio.chatapp.demo.main

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import co.th.digio.chatapp.demo.R
import com.firebase.ui.auth.AuthUI
import com.sendbird.android.SendBird
import th.co.digio.chatapp.demo.groupchannel.GroupChannelActivity
import th.co.digio.chatapp.demo.openchannel.OpenChannelActivity
import th.co.digio.chatapp.demo.privatechannel.PrivateChannelActivity
import th.co.digio.chatapp.demo.utils.PreferenceUtils

class MainActivity : AppCompatActivity() {

    private var mToolbar: Toolbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mToolbar = findViewById(R.id.toolbar_main)
        setSupportActionBar(mToolbar)

        findViewById<View>(R.id.linear_layout_group_channels).setOnClickListener {
            val intent = Intent(this@MainActivity, GroupChannelActivity::class.java)
            startActivity(intent)
        }

        findViewById<View>(R.id.linear_layout_private_channels).setOnClickListener {
            val intent = Intent(this@MainActivity, PrivateChannelActivity::class.java)
            startActivity(intent)
        }

        findViewById<View>(R.id.linear_layout_open_channels).setOnClickListener {
            val intent = Intent(this@MainActivity, OpenChannelActivity::class.java)
            startActivity(intent)
        }

        findViewById<View>(R.id.button_disconnect).setOnClickListener {
            // Unregister push tokens and disconnect
            disconnect()
        }

        // Displays the SDK version in a TextView
        val sdkVersion = String.format(resources.getString(R.string.all_app_version),
                BaseApplication.VERSION, SendBird.getSDKVersion())
        (findViewById<View>(R.id.text_main_versions) as TextView).text = sdkVersion
    }

    /**
     * Unregisters all push tokens for the current user so that they do not receive any notifications,
     * then disconnects from SendBird.
     */
    private fun disconnect() {
        SendBird.unregisterPushTokenAllForCurrentUser { e ->
            if (e != null) {
                // Error!
                e.printStackTrace()

                // Don't return because we still need to disconnect.
            } else {
                //                    Toast.makeText(MainActivity.this, "All push tokens unregistered.", Toast.LENGTH_SHORT).show();
            }

            SendBird.disconnect { PreferenceUtils.setConnected(this@MainActivity, false) }

            AuthUI.getInstance()
                    .signOut(this@MainActivity)
                    .addOnCompleteListener {
                        val intent = Intent(applicationContext, LoginActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_main -> {
                val intent = Intent(this@MainActivity, SettingsActivity::class.java)
                startActivity(intent)
                return true
            }
        }
        return false
    }
}
