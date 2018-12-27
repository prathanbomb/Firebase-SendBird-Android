package th.co.digio.chatapp.demo.openchannel

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import co.th.digio.chatapp.demo.R

class OpenChannelActivity : AppCompatActivity() {
    private var mOnBackPressedListener: OnBackPressedListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_open_channel)

        val toolbar = findViewById<Toolbar>(R.id.toolbar_open_channel)
        setSupportActionBar(toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setHomeAsUpIndicator(R.drawable.ic_arrow_left_white_24_dp)
        }

        // Load list of Open Channels
        val fragment = OpenChannelListFragment.newInstance()

        val manager = supportFragmentManager
        manager.popBackStack()

        manager.beginTransaction()
                .replace(R.id.container_open_channel, fragment)
                .commit()
    }

    internal interface OnBackPressedListener {
        fun onBack(): Boolean
    }

    internal fun setOnBackPressedListener(listener: OnBackPressedListener) {
        mOnBackPressedListener = listener
    }

    override fun onBackPressed() {
        if (mOnBackPressedListener != null && mOnBackPressedListener!!.onBack()) {
            return
        }
        super.onBackPressed()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == android.R.id.home) {
            onBackPressed()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    internal fun setActionBarTitle(title: String) {
        if (supportActionBar != null) {
            supportActionBar!!.title = title
        }
    }
}
