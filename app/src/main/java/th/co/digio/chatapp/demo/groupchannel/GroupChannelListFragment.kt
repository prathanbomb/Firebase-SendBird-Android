package th.co.digio.chatapp.demo.groupchannel

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import co.th.digio.chatapp.demo.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.sendbird.android.*
import th.co.digio.chatapp.demo.groupchannel.CreateGroupChannelActivity.Companion.CUSTOM_TYPE_GROUP

class GroupChannelListFragment : Fragment() {

    private var mRecyclerView: RecyclerView? = null
    private var mLayoutManager: LinearLayoutManager? = null
    private var mChannelListAdapter: GroupChannelListAdapter? = null
    private var mCreateChannelFab: FloatingActionButton? = null
    private var mChannelListQuery: GroupChannelListQuery? = null
    private var mSwipeRefresh: SwipeRefreshLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mChannelListAdapter = GroupChannelListAdapter(activity!!)
        mChannelListAdapter!!.load()
    }

    override fun onDestroy() {
        super.onDestroy()
        mChannelListAdapter!!.save()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        Log.d("LIFECYCLE", "GroupChannelListFragment onCreateView()")

        val rootView = inflater.inflate(R.layout.fragment_group_channel_list, container, false)

        retainInstance = true

        // Change action bar title
        (activity as GroupChannelActivity).setActionBarTitle(resources.getString(R.string.all_group_channels))

        mRecyclerView = rootView.findViewById(R.id.recycler_group_channel_list)
        mCreateChannelFab = rootView.findViewById(R.id.fab_group_channel_list)
        mSwipeRefresh = rootView.findViewById(R.id.swipe_layout_group_channel_list)

        mSwipeRefresh!!.setOnRefreshListener {
            mSwipeRefresh!!.isRefreshing = true
            refreshChannelList(15)
        }

        mCreateChannelFab!!.setOnClickListener {
            val intent = Intent(context, CreateGroupChannelActivity::class.java)
            startActivityForResult(intent, INTENT_REQUEST_NEW_GROUP_CHANNEL)
        }

        setUpRecyclerView()
        setUpChannelListAdapter()
        return rootView
    }

    override fun onResume() {
        Log.d("LIFECYCLE", "GroupChannelListFragment onResume()")

        refreshChannelList(15)

        SendBird.addChannelHandler(CHANNEL_HANDLER_ID, object : SendBird.ChannelHandler() {
            override fun onMessageReceived(baseChannel: BaseChannel, baseMessage: BaseMessage) {
                mChannelListAdapter!!.clearMap()
                mChannelListAdapter!!.updateOrInsert(baseChannel)
            }

            override fun onTypingStatusUpdated(channel: GroupChannel?) {
                mChannelListAdapter!!.notifyDataSetChanged()
            }
        })

        super.onResume()
    }

    override fun onPause() {
        Log.d("LIFECYCLE", "GroupChannelListFragment onPause()")

        SendBird.removeChannelHandler(CHANNEL_HANDLER_ID)
        super.onPause()
    }

    override fun onDetach() {
        Log.d("LIFECYCLE", "GroupChannelListFragment onDetach()")
        super.onDetach()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == INTENT_REQUEST_NEW_GROUP_CHANNEL) {
            if (resultCode == RESULT_OK) {
                // Channel successfully created
                // Enter the newly created channel.
                val newChannelUrl = data!!.getStringExtra(CreateGroupChannelActivity.EXTRA_NEW_CHANNEL_URL)
                if (newChannelUrl != null) {
                    enterGroupChannel(newChannelUrl)
                }
            } else {
                Log.d("GrChLIST", "resultCode not STATUS_OK")
            }
        }
    }

    // Sets up recycler view
    private fun setUpRecyclerView() {
        mLayoutManager = LinearLayoutManager(context)
        mRecyclerView!!.layoutManager = mLayoutManager
        mRecyclerView!!.adapter = mChannelListAdapter
        mRecyclerView!!.addItemDecoration(DividerItemDecoration(context!!, DividerItemDecoration.VERTICAL))

        // If user scrolls to bottom of the list, loads more channels.
        mRecyclerView!!.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (mLayoutManager!!.findLastVisibleItemPosition() == mChannelListAdapter!!.itemCount - 1) {
                    loadNextChannelList()
                }
            }
        })
    }

    // Sets up channel list adapter
    private fun setUpChannelListAdapter() {
        mChannelListAdapter!!.setOnItemClickListener(object : GroupChannelListAdapter.OnItemClickListener {
            override fun onItemClick(channel: GroupChannel) {
                enterGroupChannel(channel)
            }
        })
        mChannelListAdapter!!.setOnItemLongClickListener(object : GroupChannelListAdapter.OnItemLongClickListener {
            override fun onItemLongClick(channel: GroupChannel) {
                showChannelOptionsDialog(channel)
            }
        })
    }

    /**
     * Displays a dialog listing channel-specific options.
     */
    private fun showChannelOptionsDialog(channel: GroupChannel) {
        val options: Array<String>
        val pushCurrentlyEnabled = channel.isPushEnabled

        options = if (pushCurrentlyEnabled)
            arrayOf("Leave channel", "Turn push notifications OFF")
        else
            arrayOf("Leave channel", "Turn push notifications ON")

        val builder = AlertDialog.Builder(activity!!)
        builder.setTitle("Channel options")
                .setItems(options) { dialog, which ->
                    if (which == 0) {
                        // Show a dialog to confirm that the user wants to leave the channel.
                        AlertDialog.Builder(activity!!)
                                .setTitle("Leave channel " + channel.name + "?")
                                .setPositiveButton("Leave") { dialog, which -> leaveChannel(channel) }
                                .setNegativeButton("Cancel", null)
                                .create().show()
                    } else if (which == 1) {
                        setChannelPushPreferences(channel, !pushCurrentlyEnabled)
                    }
                }
        builder.create().show()
    }

    /**
     * Turns push notifications on or off for a selected channel.
     * @param channel   The channel for which push preferences should be changed.
     * @param on    Whether to set push notifications on or off.
     */
    private fun setChannelPushPreferences(channel: GroupChannel, on: Boolean) {
        // Change push preferences.
        channel.setPushPreference(on, GroupChannel.GroupChannelSetPushPreferenceHandler { e ->
            if (e != null) {
                e.printStackTrace()
                Toast.makeText(activity, "Error: " + e.message, Toast.LENGTH_SHORT)
                        .show()
                return@GroupChannelSetPushPreferenceHandler
            }

            val toast = if (on)
                "Push notifications have been turned ON"
            else
                "Push notifications have been turned OFF"

            Toast.makeText(activity, toast, Toast.LENGTH_SHORT)
                    .show()
        })
    }

    /**
     * Enters a Group Channel. Upon entering, a GroupChatFragment will be inflated
     * to display messages within the channel.
     *
     * @param channel The Group Channel to enter.
     */
    internal fun enterGroupChannel(channel: GroupChannel) {
        val channelUrl = channel.url

        enterGroupChannel(channelUrl)
    }

    /**
     * Enters a Group Channel with a URL.
     *
     * @param channelUrl The URL of the channel to enter.
     */
    internal fun enterGroupChannel(channelUrl: String) {
        val fragment = GroupChatFragment.newInstance(channelUrl)
        fragmentManager!!.beginTransaction()
                .replace(R.id.container_group_channel, fragment)
                .addToBackStack(null)
                .commit()
    }

    /**
     * Creates a new query to get the list of the user's Group Channels,
     * then replaces the existing dataset.
     *
     * @param numChannels The number of channels to load.
     */
    private fun refreshChannelList(numChannels: Int) {
        mChannelListQuery = GroupChannel.createMyGroupChannelListQuery()
        mChannelListQuery!!.setLimit(numChannels)
        mChannelListQuery!!.setCustomTypeFilter(CUSTOM_TYPE_GROUP)

        mChannelListQuery!!.next(GroupChannelListQuery.GroupChannelListQueryResultHandler { list, e ->
            if (e != null) {
                // Error!
                e.printStackTrace()
                return@GroupChannelListQueryResultHandler
            }

            mChannelListAdapter!!.clearMap()
            mChannelListAdapter!!.setGroupChannelList(list)
        })

        if (mSwipeRefresh!!.isRefreshing) {
            mSwipeRefresh!!.isRefreshing = false
        }
    }

    /**
     * Loads the next channels from the current query instance.
     */
    private fun loadNextChannelList() {
        mChannelListQuery!!.next(GroupChannelListQuery.GroupChannelListQueryResultHandler { list, e ->
            if (e != null) {
                // Error!
                e.printStackTrace()
                return@GroupChannelListQueryResultHandler
            }

            for (channel in list) {
                mChannelListAdapter!!.addLast(channel)
            }
        })
    }

    /**
     * Leaves a Group Channel.
     *
     * @param channel The channel to leave.
     */
    private fun leaveChannel(channel: GroupChannel) {
        channel.leave(GroupChannel.GroupChannelLeaveHandler { e ->
            if (e != null) {
                // Error!
                return@GroupChannelLeaveHandler
            }

            // Re-query message list
            refreshChannelList(15)
        })
    }

    companion object {

        private const val CHANNEL_HANDLER_ID = "CHANNEL_HANDLER_GROUP_CHANNEL_LIST"
        const val EXTRA_GROUP_CHANNEL_URL = "GROUP_CHANNEL_URL"
        const val INTENT_REQUEST_NEW_GROUP_CHANNEL = 302

        fun newInstance(): GroupChannelListFragment {
            return GroupChannelListFragment()
        }
    }
}
