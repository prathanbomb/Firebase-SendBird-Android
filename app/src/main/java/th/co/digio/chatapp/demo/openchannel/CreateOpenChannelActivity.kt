package th.co.digio.chatapp.demo.openchannel

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import co.th.digio.chatapp.demo.R
import com.google.android.material.textfield.TextInputEditText
import com.sendbird.android.OpenChannel

/**
 * Allows a user to create an Open Channel.
 * Dialog instead of activity?
 */

class CreateOpenChannelActivity : AppCompatActivity() {

    private var mIMM: InputMethodManager? = null

    private var mNameEditText: TextInputEditText? = null
    private var enableCreate = false
    private var mCreateButton: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_open_channel)

        mIMM = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        val toolbar = findViewById<Toolbar>(R.id.toolbar_create_open_channel)
        setSupportActionBar(toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setHomeAsUpIndicator(R.drawable.ic_arrow_left_white_24_dp)
        }

        mNameEditText = findViewById(R.id.edittext_create_open_channel_name)

        mCreateButton = findViewById(R.id.button_create_open_channel)

        mCreateButton!!.setOnClickListener { createOpenChannel(mNameEditText!!.text!!.toString()) }

        mCreateButton!!.isEnabled = enableCreate

        mNameEditText!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s.isEmpty()) {
                    if (enableCreate) {
                        mCreateButton!!.isEnabled = false
                        enableCreate = false
                    }
                } else {
                    if (!enableCreate) {
                        mCreateButton!!.isEnabled = true
                        enableCreate = true
                    }
                }
            }

            override fun afterTextChanged(s: Editable) {

            }
        })

        mNameEditText!!.requestFocus()
        mIMM!!.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
    }

    override fun onPause() {
        super.onPause()
        mIMM!!.hideSoftInputFromWindow(mNameEditText!!.windowToken, 0)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == android.R.id.home) {
            onBackPressed()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    private fun createOpenChannel(name: String) {
        OpenChannel.createChannelWithOperatorUserIds(name, null, null, null, OpenChannel.OpenChannelCreateHandler { openChannel, e ->
            if (e != null) {
                // Error!
                return@OpenChannelCreateHandler
            }

            // Open Channel created
            finish()
        })
    }


}
