package th.co.digio.chatapp.demo.openchannel

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import co.th.digio.chatapp.demo.R
import com.sendbird.android.OpenChannel
import java.util.*

/**
 * An adapter that displays a list of Open Channels in an RecyclerView.
 */
internal class OpenChannelListAdapter(private val mContext: Context) : RecyclerView.Adapter<OpenChannelListAdapter.ChannelHolder>() {

    private var mChannelList: MutableList<OpenChannel>? = null
    private var mItemClickListener: OnItemClickListener? = null
    private var mItemLongClickListener: OnItemLongClickListener? = null

    internal interface OnItemClickListener {
        fun onItemClick(channel: OpenChannel)
    }

    internal interface OnItemLongClickListener {
        fun onItemLongPress(channel: OpenChannel)
    }

    init {
        mChannelList = ArrayList()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_open_channel, parent, false)
        return ChannelHolder(view)
    }

    override fun onBindViewHolder(holder: ChannelHolder, position: Int) {
        holder.bind(mContext, mChannelList!![position], position, mItemClickListener, mItemLongClickListener)
    }

    override fun getItemCount(): Int {
        return mChannelList!!.size
    }

    fun setOpenChannelList(channelList: MutableList<OpenChannel>) {
        mChannelList = channelList
        notifyDataSetChanged()
    }

    fun addLast(channel: OpenChannel) {
        mChannelList!!.add(channel)
        notifyDataSetChanged()
    }

    fun setOnItemLongClickListener(listener: OnItemLongClickListener) {
        mItemLongClickListener = listener
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        mItemClickListener = listener
    }

    internal class ChannelHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // A list of colors for decorating each list item.
        private val colorList = arrayOf("#ff2de3e1", "#ff35a3fb", "#ff805aff", "#ffcf47fb", "#ffe248c3")

        private var nameText: TextView = itemView.findViewById(R.id.text_open_channel_list_name) as TextView
        private var participantCountText: TextView = itemView.findViewById(R.id.text_open_channel_list_participant_count) as TextView
        private var coloredDecorator: ImageView = itemView.findViewById(R.id.image_open_channel_list_decorator) as ImageView

        fun bind(context: Context, channel: OpenChannel, position: Int, clickListener: OnItemClickListener?, longClickListener: OnItemLongClickListener?) {
            nameText.text = channel.name

            val participantCount = String.format(context.resources
                    .getString(R.string.open_channel_list_participant_count), channel.participantCount)
            participantCountText.text = participantCount

            coloredDecorator.setBackgroundColor(Color.parseColor(colorList[position % colorList.size]))

            // Set an OnClickListener to this item.
            if (clickListener != null) {
                itemView.setOnClickListener { clickListener.onItemClick(channel) }
            }

            // Set an OnLongClickListener to this item.
            if (longClickListener != null) {
                itemView.setOnLongClickListener {
                    longClickListener.onItemLongPress(channel)

                    // return true if the callback consumed the long click
                    true
                }
            }
        }

    }

}
