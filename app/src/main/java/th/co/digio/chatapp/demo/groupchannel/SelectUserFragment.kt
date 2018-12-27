package th.co.digio.chatapp.demo.groupchannel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import co.th.digio.chatapp.demo.R
import com.sendbird.android.SendBird
import com.sendbird.android.User
import com.sendbird.android.UserListQuery

/**
 * A fragment displaying a list of selectable users.
 */
class SelectUserFragment : Fragment() {

    private var mLayoutManager: LinearLayoutManager? = null
    private var mRecyclerView: RecyclerView? = null
    private var mListAdapter: SelectableUserListAdapter? = null

    private var mUserListQuery: UserListQuery? = null
    private var mListener: UsersSelectedListener? = null

    // To pass selected user IDs to the parent Activity.
    internal interface UsersSelectedListener {
        fun onUserSelected(selected: Boolean, userId: String)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val rootView = inflater.inflate(R.layout.fragment_select_user, container, false)
        mRecyclerView = rootView.findViewById(R.id.recycler_select_user)
        mListAdapter = SelectableUserListAdapter(activity!!, false, true)

        mListAdapter!!.setItemCheckedChangeListener(object : SelectableUserListAdapter.OnItemCheckedChangeListener {
            override fun onItemChecked(user: User, checked: Boolean) {
                if (checked) {
                    mListener!!.onUserSelected(true, user.userId)
                } else {
                    mListener!!.onUserSelected(false, user.userId)
                }
            }
        })

        mListener = activity as UsersSelectedListener?

        setUpRecyclerView()

        loadInitialUserList(15)

        (activity as CreateGroupChannelActivity).setState(CreateGroupChannelActivity.STATE_SELECT_USERS)

        return rootView
    }

    private fun setUpRecyclerView() {
        mLayoutManager = LinearLayoutManager(activity)
        mRecyclerView!!.layoutManager = mLayoutManager
        mRecyclerView!!.adapter = mListAdapter
        mRecyclerView!!.addItemDecoration(DividerItemDecoration(activity!!, DividerItemDecoration.VERTICAL))
        mRecyclerView!!.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (mLayoutManager!!.findLastVisibleItemPosition() == mListAdapter!!.itemCount - 1) {
                    loadNextUserList(10)
                }
            }
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

    companion object {

        internal fun newInstance(): SelectUserFragment {
            return SelectUserFragment()
        }
    }
}
