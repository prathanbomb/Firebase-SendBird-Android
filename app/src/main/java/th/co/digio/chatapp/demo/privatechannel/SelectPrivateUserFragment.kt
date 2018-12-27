package th.co.digio.chatapp.demo.privatechannel


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
 * A simple [Fragment] subclass.
 */
class SelectPrivateUserFragment : Fragment() {

    private var mLayoutManager: LinearLayoutManager? = null
    private var mRecyclerView: RecyclerView? = null
    private var mListAdapter: SelectablePrivateUserListAdapter? = null

    private var mUserListQuery: UserListQuery? = null
    private var mListener: SelectPrivateUserFragment.UsersSelectedListener? = null

    // To pass selected user IDs to the parent Activity.
    internal interface UsersSelectedListener {
        fun onUserSelected(userId: String)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val rootView = inflater.inflate(R.layout.fragment_select_private_user, container, false)
        mRecyclerView = rootView.findViewById(R.id.recycler_select_private_user)
        mListAdapter = SelectablePrivateUserListAdapter(activity!!, false, true)

        mListAdapter!!.setSelectItemListener(object : SelectablePrivateUserListAdapter.OnItemSelectedListener {
            override fun onItemSelected(user: User) {
                mListener!!.onUserSelected(user.userId)
            }
        })

        mListener = activity as SelectPrivateUserFragment.UsersSelectedListener?

        setUpRecyclerView()

        loadInitialUserList(15)

        //        ((CreatePrivateChannelActivity) getActivity()).setState(CreatePrivateChannelActivity.STATE_SELECT_USERS);

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

        internal fun newInstance(): SelectPrivateUserFragment {
            return SelectPrivateUserFragment()
        }
    }

}
