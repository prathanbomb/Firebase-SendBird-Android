package th.co.digio.chatapp.demo.groupchannel

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import co.th.digio.chatapp.demo.R
import com.sendbird.android.GroupChannel
import th.co.digio.chatapp.demo.utils.PreferenceUtils
import java.util.*

/**
 * An Activity to create a new Group Channel.
 * First displays a selectable list of users,
 * then shows an option to create a Distinct channel.
 */

class CreateGroupChannelActivity : AppCompatActivity(), SelectUserFragment.UsersSelectedListener, SelectDistinctFragment.DistinctSelectedListener {

    private var mNextButton: Button? = null
    private var mCreateButton: Button? = null

    private var mSelectedIds: MutableList<String>? = null
    private var mIsDistinct = true

    private var mCurrentState: Int = 0

    private var mToolbar: Toolbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_create_group_channel)

        mSelectedIds = ArrayList()

        if (savedInstanceState == null) {
            val fragment = SelectUserFragment.newInstance()
            val manager = supportFragmentManager
            manager.beginTransaction()
                    .replace(R.id.container_create_group_channel, fragment)
                    .commit()
        }

        mNextButton = findViewById(R.id.button_create_group_channel_next)
        mNextButton!!.setOnClickListener {
            if (mCurrentState == STATE_SELECT_USERS) {
                val fragment = SelectDistinctFragment.newInstance()
                supportFragmentManager.beginTransaction()
                        .replace(R.id.container_create_group_channel, fragment)
                        .addToBackStack(null)
                        .commit()
            }
        }
        mNextButton!!.isEnabled = false

        mCreateButton = findViewById(R.id.button_create_group_channel_create)
        mCreateButton!!.setOnClickListener {
            if (mCurrentState == STATE_SELECT_USERS) {
                //                if (mCurrentState == STATE_SELECT_DISTINCT) {
                mIsDistinct = PreferenceUtils.getGroupChannelDistinct(this@CreateGroupChannelActivity)
                createGroupChannel(mSelectedIds, mIsDistinct)
            }
        }
        mCreateButton!!.isEnabled = false

        mToolbar = findViewById(R.id.toolbar_create_group_channel)
        setSupportActionBar(mToolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setHomeAsUpIndicator(R.drawable.ic_arrow_left_white_24_dp)
        }

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == android.R.id.home) {
            onBackPressed()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    internal fun setState(state: Int) {
        if (state == STATE_SELECT_USERS) {
            mCurrentState = STATE_SELECT_USERS
            mCreateButton!!.visibility = View.VISIBLE
            mNextButton!!.visibility = View.GONE
            //            mCreateButton.setVisibility(View.GONE);
            //            mNextButton.setVisibility(View.VISIBLE);
        } else if (state == STATE_SELECT_DISTINCT) {
            mCurrentState = STATE_SELECT_DISTINCT
            mCreateButton!!.visibility = View.VISIBLE
            mNextButton!!.visibility = View.GONE
        }
    }

    override fun onUserSelected(selected: Boolean, userId: String) {
        if (selected) {
            mSelectedIds!!.add(userId)
        } else {
            mSelectedIds!!.remove(userId)
        }

        mCreateButton!!.isEnabled = mSelectedIds!!.size > 1
    }

    override fun onDistinctSelected(distinct: Boolean) {
        mIsDistinct = distinct
    }

    /**
     * Creates a new Group Channel.
     *
     * Note that if you have not included empty channels in your GroupChannelListQuery,
     * the channel will not be shown in the user's channel list until at least one message
     * has been sent inside.
     *
     * @param userIds   The users to be members of the new channel.
     * @param distinct  Whether the channel is unique for the selected members.
     * If you attempt to create another Distinct channel with the same members,
     * the existing channel instance will be returned.
     */
    private fun createGroupChannel(userIds: List<String>?, distinct: Boolean) {
        GroupChannel.createChannelWithUserIds(userIds, distinct, null, null, null, CUSTOM_TYPE_GROUP, GroupChannel.GroupChannelCreateHandler { groupChannel, e ->
            if (e != null) {
                // Error!
                return@GroupChannelCreateHandler
            }

            val intent = Intent()
            intent.putExtra(EXTRA_NEW_CHANNEL_URL, groupChannel.url)
            setResult(Activity.RESULT_OK, intent)
            finish()
        })
    }

    companion object {

        const val EXTRA_NEW_CHANNEL_URL = "EXTRA_NEW_CHANNEL_URL"
        const val CUSTOM_TYPE_GROUP = "GROUP"

        internal const val STATE_SELECT_USERS = 0
        internal const val STATE_SELECT_DISTINCT = 1
    }


}
