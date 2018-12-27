package th.co.digio.chatapp.demo.openchannel

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import co.th.digio.chatapp.demo.R
import com.sendbird.android.OpenChannel
import com.sendbird.android.UserListQuery
import th.co.digio.chatapp.demo.groupchannel.UserListAdapter

/**
 * Displays a list of the participants of a specified Open Channel.
 */

class ParticipantListActivity : AppCompatActivity() {

    private var mListAdapter: UserListAdapter? = null
    private var mRecyclerView: RecyclerView? = null
    private var mLayoutManager: LinearLayoutManager? = null
    private var mChannelUrl: String? = null
    private var mChannel: OpenChannel? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_participant_list)
        mRecyclerView = findViewById(R.id.recycler_participant_list)

        mChannelUrl = intent.getStringExtra(OpenChatFragment.EXTRA_CHANNEL_URL)
        mListAdapter = UserListAdapter(this, false)

        val toolbar = findViewById<Toolbar>(R.id.toolbar_participant_list)
        setSupportActionBar(toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setHomeAsUpIndicator(R.drawable.ic_arrow_left_white_24_dp)
        }

        setUpRecyclerView()

        getChannelFromUrl()
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
    }

    /**
     * Gets the channel instance with the channel URL.
     */
    private fun getChannelFromUrl() {
        OpenChannel.getChannel(mChannelUrl) { openChannel, e ->
            mChannel = openChannel

            getUserList()
        }
    }

    private fun getUserList() {
        val userListQuery = mChannel!!.createParticipantListQuery()
        userListQuery.next(UserListQuery.UserListQueryResultHandler { list, e ->
            if (e != null) {
                // Error!
                return@UserListQueryResultHandler
            }

            mListAdapter!!.setUserList(list)
        })
    }


}
