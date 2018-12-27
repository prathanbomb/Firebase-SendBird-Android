package th.co.digio.chatapp.demo.main

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import co.th.digio.chatapp.demo.R
import com.sendbird.android.SendBird
import com.sendbird.android.User
import com.sendbird.android.UserListQuery
import th.co.digio.chatapp.demo.groupchannel.SelectableUserListAdapter
import java.util.*

class BlockedMembersListActivity : AppCompatActivity() {

    private var mLayoutManager: LinearLayoutManager? = null
    private var mRecyclerView: RecyclerView? = null
    private var mListAdapter: SelectableUserListAdapter? = null
    private var mUserListQuery: UserListQuery? = null

    private var mButtonEdit: Button? = null
    private var mButtonUnblock: Button? = null

    private var mSelectedIds: MutableList<String>? = null
    private var mCurrentState: Int = 0

    private var mToolbar: Toolbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_blocked_members_list)

        mSelectedIds = ArrayList()

        mButtonEdit = findViewById<View>(R.id.button_edit) as Button
        mButtonEdit!!.setOnClickListener { setState(STATE_EDIT) }
        mButtonEdit!!.isEnabled = false

        mButtonUnblock = findViewById<View>(R.id.button_unblock) as Button
        mButtonUnblock!!.setOnClickListener {
            mListAdapter!!.unblock()
            setState(STATE_NORMAL)
        }
        mButtonUnblock!!.isEnabled = false

        mToolbar = findViewById<View>(R.id.toolbar_blocked_members_list) as Toolbar
        setSupportActionBar(mToolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setHomeAsUpIndicator(R.drawable.ic_arrow_left_white_24_dp)
        }

        mRecyclerView = findViewById<View>(R.id.recycler_select_user) as RecyclerView
        mListAdapter = SelectableUserListAdapter(this, true, false)
        mListAdapter!!.setItemCheckedChangeListener(object : SelectableUserListAdapter.OnItemCheckedChangeListener {
            override fun onItemChecked(user: User, checked: Boolean) {
                if (checked) {
                    userSelected(true, user.userId)
                } else {
                    userSelected(false, user.userId)
                }
            }
        })

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

    internal fun setState(state: Int) {
        if (state == STATE_EDIT) {
            mCurrentState = STATE_EDIT
            mButtonUnblock!!.visibility = View.VISIBLE
            mButtonEdit!!.visibility = View.GONE
            mListAdapter!!.setShowCheckBox(true)
        } else if (state == STATE_NORMAL) {
            mCurrentState = STATE_NORMAL
            mButtonUnblock!!.visibility = View.GONE
            mButtonEdit!!.visibility = View.VISIBLE
            mListAdapter!!.setShowCheckBox(false)
        }
    }

    fun userSelected(selected: Boolean, userId: String) {
        if (selected) {
            mSelectedIds!!.add(userId)
        } else {
            mSelectedIds!!.remove(userId)
        }

        mButtonUnblock!!.isEnabled = mSelectedIds!!.size > 0
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

    private fun loadInitialUserList(size: Int) {
        mUserListQuery = SendBird.createBlockedUserListQuery()

        mUserListQuery!!.setLimit(size)
        mUserListQuery!!.next(UserListQuery.UserListQueryResultHandler { list, e ->
            if (e != null) {
                // Error!
                return@UserListQueryResultHandler
            }

            mListAdapter!!.setUserList(list)
            mButtonEdit!!.isEnabled = list.size > 0
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

    override fun onBackPressed() {
        if (mCurrentState == STATE_EDIT) {
            setState(STATE_NORMAL)
        } else {
            super.onBackPressed()
        }
    }

    fun blockedMemberCount(size: Int) {
        mButtonEdit!!.isEnabled = size > 0
    }

    companion object {
        private const val STATE_NORMAL = 0
        private const val STATE_EDIT = 1
    }
}
