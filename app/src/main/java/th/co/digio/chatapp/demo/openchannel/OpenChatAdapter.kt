package th.co.digio.chatapp.demo.openchannel


import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import co.th.digio.chatapp.demo.R
import com.sendbird.android.*
import th.co.digio.chatapp.demo.utils.DateUtils
import th.co.digio.chatapp.demo.utils.FileUtils
import th.co.digio.chatapp.demo.utils.ImageUtils
import java.util.*

/**
 * An adapter for a RecyclerView that displays messages in an Open Channel.
 */

internal class OpenChatAdapter(private val mContext: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var mMessageList: MutableList<BaseMessage>? = null
    private var mItemClickListener: OnItemClickListener? = null
    private var mItemLongClickListener: OnItemLongClickListener? = null

    /**
     * An interface to implement item click callbacks in the activity or fragment that
     * uses this adapter.
     */
    internal interface OnItemClickListener {
        fun onUserMessageItemClick(message: UserMessage)

        fun onFileMessageItemClick(message: FileMessage)

        fun onAdminMessageItemClick(message: AdminMessage)
    }

    internal interface OnItemLongClickListener {
        fun onBaseMessageLongClick(message: BaseMessage)
    }


    init {
        mMessageList = ArrayList()
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        mItemClickListener = listener
    }

    fun setOnItemLongClickListener(listener: OnItemLongClickListener) {
        mItemLongClickListener = listener
    }

    fun setMessageList(messages: MutableList<BaseMessage>) {
        mMessageList = messages
        notifyDataSetChanged()
    }

    fun addFirst(message: BaseMessage) {
        mMessageList!!.add(0, message)
        notifyDataSetChanged()
    }

    fun addLast(message: BaseMessage) {
        mMessageList!!.add(message)
        notifyDataSetChanged()
    }

    fun delete(msgId: Long) {
        for (msg in mMessageList!!) {
            if (msg.messageId == msgId) {
                mMessageList!!.remove(msg)
                notifyDataSetChanged()
                break
            }
        }
    }

    fun update(message: BaseMessage) {
        var baseMessage: BaseMessage
        for (index in mMessageList!!.indices) {
            baseMessage = mMessageList!![index]
            if (message.messageId == baseMessage.messageId) {
                mMessageList!!.removeAt(index)
                mMessageList!!.add(index, message)
                notifyDataSetChanged()
                break
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (viewType) {
            VIEW_TYPE_USER_MESSAGE -> {
                val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.list_item_open_chat_user, parent, false)
                return UserMessageHolder(view)

            }
            VIEW_TYPE_ADMIN_MESSAGE -> {
                val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.list_item_open_chat_admin, parent, false)
                return AdminMessageHolder(view)

            }
            VIEW_TYPE_FILE_MESSAGE -> {
                val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.list_item_open_chat_file, parent, false)
                return FileMessageHolder(view)
            }

            // Theoretically shouldn't happen.
            else -> {
                val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.list_item_open_chat_user, parent, false)
                return UserMessageHolder(view)
            }
        }

    }

    override fun getItemViewType(position: Int): Int {
        return when {
            mMessageList!![position] is UserMessage -> VIEW_TYPE_USER_MESSAGE
            mMessageList!![position] is AdminMessage -> VIEW_TYPE_ADMIN_MESSAGE
            mMessageList!![position] is FileMessage -> VIEW_TYPE_FILE_MESSAGE
            else -> -1
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = mMessageList!![position]

        var isNewDay = false

        // If there is at least one item preceding the current one, check the previous message.
        if (position < mMessageList!!.size - 1) {
            val prevMessage = mMessageList!![position + 1]

            // If the date of the previous message is different, display the date before the message,
            // and also set isContinuous to false to show information such as the sender's nickname
            // and profile image.
            if (!DateUtils.hasSameDate(message.createdAt, prevMessage.createdAt)) {
                isNewDay = true
            }

        } else if (position == mMessageList!!.size - 1) {
            isNewDay = true
        }

        when (holder.itemViewType) {
            VIEW_TYPE_USER_MESSAGE -> (holder as UserMessageHolder).bind(mContext, message as UserMessage, isNewDay,
                    mItemClickListener, mItemLongClickListener)
            VIEW_TYPE_ADMIN_MESSAGE -> (holder as AdminMessageHolder).bind(message as AdminMessage, isNewDay,
                    mItemClickListener)
            VIEW_TYPE_FILE_MESSAGE -> (holder as FileMessageHolder).bind(mContext, message as FileMessage, isNewDay,
                    mItemClickListener, mItemLongClickListener)
            else -> {
            }
        }
    }

    override fun getItemCount(): Int {
        return mMessageList!!.size
    }

    private inner class UserMessageHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {

        internal var nicknameText: TextView = itemView.findViewById(R.id.text_open_chat_nickname) as TextView
        internal var messageText: TextView = itemView.findViewById(R.id.text_open_chat_message) as TextView
        internal var editedText: TextView = itemView.findViewById(R.id.text_open_chat_edited) as TextView
        internal var timeText: TextView = itemView.findViewById(R.id.text_open_chat_time) as TextView
        internal var dateText: TextView = itemView.findViewById(R.id.text_open_chat_date) as TextView
        internal var profileImage: ImageView = itemView.findViewById(R.id.image_open_chat_profile) as ImageView

        // Binds message details to ViewHolder item
        internal fun bind(context: Context, message: UserMessage, isNewDay: Boolean,
                          clickListener: OnItemClickListener?,
                          longClickListener: OnItemLongClickListener?) {

            val sender = message.sender

            // If current user sent the message, display name in different color
            if (sender.userId == SendBird.getCurrentUser().userId) {
                nicknameText.setTextColor(ContextCompat.getColor(context, R.color.openChatNicknameMe))
            } else {
                nicknameText.setTextColor(ContextCompat.getColor(context, R.color.openChatNicknameOther))
            }

            // Show the date if the message was sent on a different date than the previous one.
            if (isNewDay) {
                dateText.visibility = View.VISIBLE
                dateText.text = DateUtils.formatDate(message.createdAt)
            } else {
                dateText.visibility = View.GONE
            }

            nicknameText.text = message.sender.nickname
            messageText.text = message.message
            timeText.text = DateUtils.formatTime(message.createdAt)

            if (message.updatedAt > 0) {
                editedText.visibility = View.VISIBLE
            } else {
                editedText.visibility = View.GONE
            }

            // Get profile image and display it
            ImageUtils.displayRoundImageFromUrl(context, message.sender.profileUrl, profileImage)

            if (clickListener != null) {
                itemView.setOnClickListener { clickListener.onUserMessageItemClick(message) }
            }

            if (longClickListener != null) {
                itemView.setOnLongClickListener {
                    longClickListener.onBaseMessageLongClick(message)
                    true
                }
            }
        }
    }

    private inner class AdminMessageHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {

        internal var messageText: TextView = itemView.findViewById(R.id.text_open_chat_message) as TextView
        internal var dateText: TextView = itemView.findViewById(R.id.text_open_chat_date) as TextView

        internal fun bind(message: AdminMessage, isNewDay: Boolean, listener: OnItemClickListener?) {
            messageText.text = message.message

            // Show the date if the message was sent on a different date than the previous one.
            if (isNewDay) {
                dateText.visibility = View.VISIBLE
                dateText.text = DateUtils.formatDate(message.createdAt)
            } else {
                dateText.visibility = View.GONE
            }

            itemView.setOnClickListener { listener!!.onAdminMessageItemClick(message) }
        }
    }

    private inner class FileMessageHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {

        internal var nicknameText: TextView = itemView.findViewById(R.id.text_open_chat_nickname) as TextView
        internal var timeText: TextView = itemView.findViewById(R.id.text_open_chat_time) as TextView
        internal var fileNameText: TextView = itemView.findViewById(R.id.text_open_chat_file_name) as TextView
        internal var fileSizeText: TextView = itemView.findViewById(R.id.text_open_chat_file_size) as TextView
        internal var dateText: TextView = itemView.findViewById(R.id.text_open_chat_date) as TextView
        internal var profileImage: ImageView = itemView.findViewById(R.id.image_open_chat_profile) as ImageView
        internal var fileThumbnail: ImageView = itemView.findViewById(R.id.image_open_chat_file_thumbnail) as ImageView

        // Binds message details to ViewHolder item
        internal fun bind(context: Context, message: FileMessage, isNewDay: Boolean,
                          clickListener: OnItemClickListener?,
                          longClickListener: OnItemLongClickListener?) {
            val sender = message.sender

            // If current user sent the message, display name in different color
            if (sender.userId == SendBird.getCurrentUser().userId) {
                nicknameText.setTextColor(ContextCompat.getColor(context, R.color.openChatNicknameMe))
            } else {
                nicknameText.setTextColor(ContextCompat.getColor(context, R.color.openChatNicknameOther))
            }

            // Show the date if the message was sent on a different date than the previous one.
            if (isNewDay) {
                dateText.visibility = View.VISIBLE
                dateText.text = DateUtils.formatDate(message.createdAt)
            } else {
                dateText.visibility = View.GONE
            }

            // Get profile image and display it
            ImageUtils.displayRoundImageFromUrl(context, message.sender.profileUrl, profileImage)

            fileNameText.text = message.name
            fileSizeText.text = FileUtils.toReadableFileSize(message.size.toLong())
            nicknameText.text = message.sender.nickname

            // If image, display thumbnail
            if (message.type.toLowerCase().startsWith("image")) {
                // Get thumbnails from FileMessage
                val thumbnails = message.thumbnails as ArrayList<FileMessage.Thumbnail>

                // If thumbnails exist, get smallest (first) thumbnail and display it in the message
                if (thumbnails.size > 0) {
                    if (message.type.toLowerCase().contains("gif")) {
                        ImageUtils.displayGifImageFromUrl(context, message.url, fileThumbnail, thumbnails[0].url, fileThumbnail.drawable)
                    } else {
                        ImageUtils.displayImageFromUrl(context, thumbnails[0].url, fileThumbnail, fileThumbnail.drawable)
                    }
                } else {
                    if (message.type.toLowerCase().contains("gif")) {
                        ImageUtils.displayGifImageFromUrl(context, message.url, fileThumbnail, null as String?, fileThumbnail.drawable)
                    } else {
                        ImageUtils.displayImageFromUrl(context, message.url, fileThumbnail, fileThumbnail.drawable)
                    }
                }

            } else if (message.type.toLowerCase().startsWith("video")) {
                // Get thumbnails from FileMessage
                val thumbnails = message.thumbnails as ArrayList<FileMessage.Thumbnail>

                // If thumbnails exist, get smallest (first) thumbnail and display it in the message
                if (thumbnails.size > 0) {
                    ImageUtils.displayImageFromUrlWithPlaceHolder(
                            context, thumbnails[0].url, fileThumbnail, R.drawable.ic_file_message)
                } else {
                    fileThumbnail.setImageDrawable(context.resources.getDrawable(R.drawable.ic_play))
                }

            } else {
                fileThumbnail.setImageDrawable(context.resources.getDrawable(R.drawable.ic_file_message))
            }

            if (clickListener != null) {
                itemView.setOnClickListener { clickListener.onFileMessageItemClick(message) }
            }

            if (longClickListener != null) {
                itemView.setOnLongClickListener {
                    longClickListener.onBaseMessageLongClick(message)
                    true
                }
            }

        }
    }

    companion object {
        private const val VIEW_TYPE_USER_MESSAGE = 10
        private const val VIEW_TYPE_FILE_MESSAGE = 20
        private const val VIEW_TYPE_ADMIN_MESSAGE = 30
    }


}
