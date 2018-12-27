package th.co.digio.chatapp.demo.openchannel

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import co.th.digio.chatapp.demo.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.sendbird.android.OpenChannel
import com.sendbird.android.OpenChannelListQuery


class OpenChannelListFragment : Fragment() {

    private var mRecyclerView: RecyclerView? = null
    private var mLayoutManager: LinearLayoutManager? = null
    private var mChannelListAdapter: OpenChannelListAdapter? = null
    private var mSwipeRefresh: SwipeRefreshLayout? = null
    private var mCreateChannelFab: FloatingActionButton? = null

    private var mChannelListQuery: OpenChannelListQuery? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val rootView = inflater.inflate(R.layout.fragment_open_channel_list, container, false)

        retainInstance = true

        setHasOptionsMenu(true)

        (activity as OpenChannelActivity).setActionBarTitle(resources.getString(R.string.all_open_channels))

        mRecyclerView = rootView.findViewById(R.id.recycler_open_channel_list)
        mChannelListAdapter = OpenChannelListAdapter(context!!)

        // Set color?
        mSwipeRefresh = rootView.findViewById(R.id.swipe_layout_open_channel_list)

        // Swipe down to refresh channel list.
        mSwipeRefresh!!.setOnRefreshListener {
            mSwipeRefresh!!.isRefreshing = true
            refreshChannelList(15)
        }

        mCreateChannelFab = rootView.findViewById(R.id.fab_open_channel_list)
        mCreateChannelFab!!.setOnClickListener {
            val intent = Intent(activity, CreateOpenChannelActivity::class.java)
            startActivity(intent)
        }

        setUpAdapter()
        setUpRecyclerView()

        return rootView
    }

    override fun onResume() {
        super.onResume()

        // Refresh once
        refreshChannelList(15)
    }

    internal fun setUpRecyclerView() {
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

    // Set touch listeners to RecyclerView items
    private fun setUpAdapter() {
        mChannelListAdapter!!.setOnItemClickListener(object : OpenChannelListAdapter.OnItemClickListener {
            override fun onItemClick(channel: OpenChannel) {
                val channelUrl = channel.url
                val fragment = OpenChatFragment.newInstance(channelUrl)
                fragmentManager!!.beginTransaction()
                        .replace(R.id.container_open_channel, fragment)
                        .addToBackStack(null)
                        .commit()
            }
        })

        mChannelListAdapter!!.setOnItemLongClickListener(object : OpenChannelListAdapter.OnItemLongClickListener {
            override fun onItemLongPress(channel: OpenChannel) {}
        })
    }

    /**
     * Creates a new query to get the list of the user's Open Channels,
     * then replaces the existing dataset.
     *
     * @param numChannels   The number of channels to load.
     */
    internal fun refreshChannelList(numChannels: Int) {
        mChannelListQuery = OpenChannel.createOpenChannelListQuery()
        mChannelListQuery!!.setLimit(numChannels)
        mChannelListQuery!!.next { list, e ->
            if (e != null) {

            }
            mChannelListAdapter!!.setOpenChannelList(list)

            if (mSwipeRefresh!!.isRefreshing) {
                mSwipeRefresh!!.isRefreshing = false
            }
        }
    }

    /**
     * Loads the next channels from the current query instance.
     */
    internal fun loadNextChannelList() {
        mChannelListQuery!!.next(OpenChannelListQuery.OpenChannelListQueryResultHandler { list, e ->
            if (e != null) {
                // Error!
                return@OpenChannelListQueryResultHandler
            }

            for (channel in list) {
                mChannelListAdapter!!.addLast(channel)
            }
        })
    }

    companion object {

        const val EXTRA_OPEN_CHANNEL_URL = "OPEN_CHANNEL_URL"
        private val LOG_TAG = OpenChannelListFragment::class.java.simpleName

        fun newInstance(): OpenChannelListFragment {

            return OpenChannelListFragment()
        }
    }
}
