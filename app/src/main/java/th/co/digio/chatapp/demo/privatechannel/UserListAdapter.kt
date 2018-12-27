package th.co.digio.chatapp.demo.privatechannel

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import co.th.digio.chatapp.demo.R
import com.sendbird.android.Member
import com.sendbird.android.SendBird
import com.sendbird.android.User
import th.co.digio.chatapp.demo.utils.ImageUtils
import java.util.*

/**
 * A simple adapter that displays a list of Users.
 */
class UserListAdapter(private val mContext: Context, private val mIsGroupChannel: Boolean) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val mUsers: MutableList<User>

    init {
        mUsers = ArrayList()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_user, parent, false)
        return UserHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as UserHolder).bind(mContext, mUsers[position])
    }

    override fun getItemCount(): Int {
        return mUsers.size
    }

    fun setUserList(users: List<User>) {
        mUsers.addAll(users)
        notifyDataSetChanged()
    }

    fun addLast(user: User) {
        mUsers.add(user)
        notifyDataSetChanged()
    }

    private inner class UserHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.text_user_list_nickname) as TextView
        private val profileImage: ImageView = itemView.findViewById(R.id.image_user_list_profile) as ImageView
        private val blockedImage: ImageView = itemView.findViewById(R.id.image_user_list_blocked) as ImageView
        private val switchBlock: SwitchCompat = itemView.findViewById(R.id.switch_block) as SwitchCompat

        internal fun bind(context: Context, user: User) {
            nameText.text = user.nickname
            ImageUtils.displayRoundImageFromUrl(context, user.profileUrl, profileImage)

            if (mIsGroupChannel) {
                if (SendBird.getCurrentUser().userId == user.userId) {
                    switchBlock.visibility = View.GONE
                    blockedImage.visibility = View.GONE
                } else {
                    switchBlock.visibility = View.VISIBLE
                }

                val isBlockedByMe = (user as Member).isBlockedByMe
                switchBlock.isChecked = !isBlockedByMe
                if (isBlockedByMe) {
                    blockedImage.visibility = View.VISIBLE
                } else {
                    blockedImage.visibility = View.GONE
                }

                switchBlock.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (isChecked) {
                        SendBird.unblockUser(user, SendBird.UserUnblockHandler { e ->
                            if (e != null) {
                                return@UserUnblockHandler
                            }
                            blockedImage.visibility = View.GONE
                        })
                    } else {
                        SendBird.blockUser(user, SendBird.UserBlockHandler { user, e ->
                            if (e != null) {
                                return@UserBlockHandler
                            }
                            blockedImage.visibility = View.VISIBLE
                        })
                    }
                }
            } else {
                blockedImage.visibility = View.GONE
                switchBlock.visibility = View.GONE
            }
        }
    }
}

