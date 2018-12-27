package th.co.digio.chatapp.demo.groupchannel

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import co.th.digio.chatapp.demo.R
import com.sendbird.android.GroupChannel
import com.sendbird.android.Member


class MemberListActivity : AppCompatActivity() {

    private var mListAdapter: UserListAdapter? = null
    private var mRecyclerView: RecyclerView? = null
    private var mLayoutManager: LinearLayoutManager? = null
    private var mChannelUrl: String? = null
    private var mChannel: GroupChannel? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_member_list)

        mChannelUrl = intent.getStringExtra(GroupChatFragment.EXTRA_CHANNEL_URL)
        if (mChannelUrl == null) {
            // Theoretically shouldn't happen
            finish()
        }

        mChannelUrl = intent.getStringExtra(GroupChatFragment.EXTRA_CHANNEL_URL)
        mRecyclerView = findViewById(R.id.recycler_member_list)
        mListAdapter = UserListAdapter(this, true)

        val toolbar = findViewById<Toolbar>(R.id.toolbar_member_list)
        setSupportActionBar(toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setHomeAsUpIndicator(R.drawable.ic_arrow_left_white_24_dp)
        }

        setUpRecyclerView()

        getChannelFromUrl(mChannelUrl)
    }

    private fun setUpRecyclerView() {
        mLayoutManager = LinearLayoutManager(this)
        mRecyclerView!!.layoutManager = mLayoutManager
        mRecyclerView!!.adapter = mListAdapter
        mRecyclerView!!.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == android.R.id.home) {
            onBackPressed()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    private fun getChannelFromUrl(url: String?) {
        GroupChannel.getChannel(url, GroupChannel.GroupChannelGetHandler { groupChannel, e ->
            if (e != null) {
                // Error!
                return@GroupChannelGetHandler
            }

            mChannel = groupChannel

            refreshChannel()
        })
    }

    private fun refreshChannel() {
        mChannel!!.refresh(GroupChannel.GroupChannelRefreshHandler { e ->
            if (e != null) {
                // Error!
                return@GroupChannelRefreshHandler
            }

            setUserList(mChannel!!.members)
        })
    }

    private fun setUserList(userList: List<Member>) {
        mListAdapter!!.setUserList(userList)
    }

}
