package th.co.digio.chatapp.demo.groupchannel

import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import co.th.digio.chatapp.demo.R
import com.sendbird.android.GroupChannel
import com.sendbird.android.SendBird
import com.sendbird.android.User
import com.sendbird.android.UserListQuery
import java.util.*

/**
 * Displays a selectable list of users within the app. Selected users will be invited into the
 * current channel.
 */

class InviteMemberActivity : AppCompatActivity() {

    private var mLayoutManager: LinearLayoutManager? = null
    private var mRecyclerView: RecyclerView? = null
    private var mListAdapter: SelectableUserListAdapter? = null
    private var mToolbar: Toolbar? = null

    private var mUserListQuery: UserListQuery? = null
    private var mChannelUrl: String? = null
    private var mInviteButton: Button? = null

    private var mSelectedUserIds: MutableList<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_invite_member)

        mSelectedUserIds = ArrayList()

        mRecyclerView = findViewById(R.id.recycler_invite_member)
        mListAdapter = SelectableUserListAdapter(this, false, true)
        mListAdapter!!.setItemCheckedChangeListener(object : SelectableUserListAdapter.OnItemCheckedChangeListener {
            override fun onItemChecked(user: User, checked: Boolean) {
                if (checked) {
                    mSelectedUserIds!!.add(user.userId)
                } else {
                    mSelectedUserIds!!.remove(user.userId)
                }

                // If no users are selected, disable the invite button.
                mInviteButton!!.isEnabled = mSelectedUserIds!!.size > 0
            }
        })

        mToolbar = findViewById(R.id.toolbar_invite_member)
        setSupportActionBar(mToolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setHomeAsUpIndicator(R.drawable.ic_arrow_left_white_24_dp)
        }

        mChannelUrl = intent.getStringExtra(GroupChatFragment.EXTRA_CHANNEL_URL)

        mInviteButton = findViewById(R.id.button_invite_member)
        mInviteButton!!.setOnClickListener {
            if (mSelectedUserIds!!.size > 0) {
                inviteSelectedMembersWithUserIds()
            }
        }
        mInviteButton!!.isEnabled = false

        setUpRecyclerView()

        loadInitialUserList(15)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == android.R.id.home) {
            onBackPressed()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    private fun setUpRecyclerView() {
        mLayoutManager = LinearLayoutManager(this)
        mRecyclerView!!.layoutManager = mLayoutManager
        mRecyclerView!!.adapter = mListAdapter
        mRecyclerView!!.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        mRecyclerView!!.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (mLayoutManager!!.findLastVisibleItemPosition() == mListAdapter!!.itemCount - 1) {
                    loadNextUserList(10)
                }
            }
        })
    }

    private fun inviteSelectedMembersWithUserIds() {

        // Get channel instance from URL first.
        GroupChannel.getChannel(mChannelUrl, GroupChannel.GroupChannelGetHandler { groupChannel, e ->
            if (e != null) {
                // Error!
                return@GroupChannelGetHandler
            }

            // Then invite the selected members to the channel.
            groupChannel.inviteWithUserIds(mSelectedUserIds, GroupChannel.GroupChannelInviteHandler { e ->
                if (e != null) {
                    // Error!
                    return@GroupChannelInviteHandler
                }

                finish()
            })
        })
    }

    /**
     * Replaces current user list with new list.
     * Should be used only on initial load.
     */
    private fun loadInitialUserList(size: Int) {
        mUserListQuery = SendBird.createUserListQuery()

        mUserListQuery!!.setLimit(size)
        mUserListQuery!!.next(UserListQuery.UserListQueryResultHandler { list, e ->
            if (e != null) {
                // Error!
                return@UserListQueryResultHandler
            }

            mListAdapter!!.setUserList(list)
        })
    }

    /**
     * Loads users and adds them to current user list.
     *
     * A PreviousMessageListQuery must have been already initialized through [.loadInitialUserList]
     */
    private fun loadNextUserList(size: Int) {
        mUserListQuery!!.setLimit(size)

        mUserListQuery!!.next(UserListQuery.UserListQueryResultHandler { list, e ->
            if (e != null) {
                // Error!
                return@UserListQueryResultHandler
            }

            for (user in list) {
                mListAdapter!!.addLast(user)
            }
        })
    }

}
