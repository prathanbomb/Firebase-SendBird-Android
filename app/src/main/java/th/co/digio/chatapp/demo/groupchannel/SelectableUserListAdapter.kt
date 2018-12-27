package th.co.digio.chatapp.demo.groupchannel

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import co.th.digio.chatapp.demo.R
import com.sendbird.android.SendBird
import com.sendbird.android.User
import th.co.digio.chatapp.demo.main.BlockedMembersListActivity
import th.co.digio.chatapp.demo.utils.ImageUtils
import th.co.digio.chatapp.demo.utils.PreferenceUtils
import java.util.*

/**
 * Populates a RecyclerView with a list of users, each with a checkbox.
 */

class SelectableUserListAdapter(private val mContext: Context, private val mIsBlockedList: Boolean, private var mShowCheckBox: Boolean) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var mUsers: MutableList<User>? = null

    private var mSelectableUserHolder: SelectableUserHolder? = null

    // For the adapter to track which users have been selected
    private var mCheckedChangeListener: OnItemCheckedChangeListener? = null

    interface OnItemCheckedChangeListener {
        fun onItemChecked(user: User, checked: Boolean)
    }

    init {
        mUsers = ArrayList()
        mSelectedUserIds = ArrayList()
    }

    fun setItemCheckedChangeListener(listener: OnItemCheckedChangeListener) {
        mCheckedChangeListener = listener
    }

    fun setUserList(users: MutableList<User>) {
        mUsers = users
        notifyDataSetChanged()
    }

    fun setShowCheckBox(showCheckBox: Boolean) {
        mShowCheckBox = showCheckBox
        if (mSelectableUserHolder != null) {
            mSelectableUserHolder!!.setShowCheckBox(showCheckBox)
        }
        notifyDataSetChanged()
    }

    fun unblock() {
        for (userId in mSelectedUserIds) {
            SendBird.unblockUserWithUserId(userId, SendBird.UserUnblockHandler { e ->
                if (e != null) {
                    return@UserUnblockHandler
                }

                var user: User
                for (index in mUsers!!.indices) {
                    user = mUsers!![index]
                    if (userId == user.userId) {
                        mUsers!!.removeAt(index)
                        break
                    }
                }

                (mContext as BlockedMembersListActivity).blockedMemberCount(mUsers!!.size)

                notifyDataSetChanged()
            })
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_selectable_user, parent, false)
        mSelectableUserHolder = SelectableUserHolder(view, mIsBlockedList, mShowCheckBox)
        return mSelectableUserHolder as SelectableUserHolder
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as SelectableUserHolder).bind(
                mContext,
                mUsers!![position],
                isSelected(mUsers!![position]),
                this.mCheckedChangeListener!!)
    }

    override fun getItemCount(): Int {
        return mUsers!!.size
    }

    fun isSelected(user: User): Boolean {
        return mSelectedUserIds.contains(user.userId)
    }

    fun addLast(user: User) {
        mUsers!!.add(user)
        notifyDataSetChanged()
    }

    private inner class SelectableUserHolder(itemView: View, private val mIsBlockedList: Boolean, private var mShowCheckBox: Boolean) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView
        private val profileImage: ImageView
        private val blockedImage: ImageView
        private val checkbox: CheckBox

        init {

            this.setIsRecyclable(false)

            nameText = itemView.findViewById(R.id.text_selectable_user_list_nickname)
            profileImage = itemView.findViewById(R.id.image_selectable_user_list_profile)
            blockedImage = itemView.findViewById(R.id.image_user_list_blocked)
            checkbox = itemView.findViewById(R.id.checkbox_selectable_user_list)
        }

        fun setShowCheckBox(showCheckBox: Boolean) {
            mShowCheckBox = showCheckBox
        }

        internal fun bind(context: Context, user: User, isSelected: Boolean, listener: OnItemCheckedChangeListener) {
            nameText.text = user.nickname
            ImageUtils.displayRoundImageFromUrl(context, user.profileUrl, profileImage)

            if (mIsBlockedList) {
                blockedImage.visibility = View.VISIBLE
            } else {
                blockedImage.visibility = View.GONE
            }

            if (mShowCheckBox) {
                checkbox.visibility = View.VISIBLE
            } else {
                checkbox.visibility = View.GONE
            }

            checkbox.isChecked = isSelected

            if (mShowCheckBox) {
                itemView.setOnClickListener {
                    if (mShowCheckBox) {
                        checkbox.isChecked = !checkbox.isChecked
                    }
                }
            }

            checkbox.setOnCheckedChangeListener { buttonView, isChecked ->
                listener.onItemChecked(user, isChecked)

                if (isChecked) {
                    mSelectedUserIds.add(user.userId)
                } else {
                    mSelectedUserIds.remove(user.userId)
                }
            }

            if (user.userId == PreferenceUtils.getUserId(context)) {
                itemView.isEnabled = false
                checkbox.isEnabled = false
            }
        }
    }

    companion object {
        private lateinit var mSelectedUserIds: MutableList<String>
    }
}
