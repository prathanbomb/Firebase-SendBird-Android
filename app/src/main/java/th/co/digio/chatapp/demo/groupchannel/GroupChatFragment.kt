package th.co.digio.chatapp.demo.groupchannel

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import co.th.digio.chatapp.demo.R
import com.google.android.material.snackbar.Snackbar
import com.sendbird.android.*
import org.json.JSONException
import th.co.digio.chatapp.demo.utils.*
import java.io.File
import java.util.*


class GroupChatFragment : Fragment() {

    private var mIMM: InputMethodManager? = null
    private var mFileProgressHandlerMap: HashMap<BaseChannel.SendFileMessageWithProgressHandler, FileMessage>? = null

    private var mRootLayout: RelativeLayout? = null
    private var mRecyclerView: RecyclerView? = null
    private var mChatAdapter: GroupChatAdapter? = null
    private var mLayoutManager: LinearLayoutManager? = null
    private var mMessageEditText: EditText? = null
    private var mMessageSendButton: Button? = null
    private var mUploadFileButton: ImageButton? = null
    private var mCurrentEventLayout: View? = null
    private var mCurrentEventText: TextView? = null

    private var mChannel: GroupChannel? = null
    private var mChannelUrl: String? = null
    private val mPrevMessageListQuery: PreviousMessageListQuery? = null

    private var mIsTyping: Boolean = false

    private var mCurrentState = STATE_NORMAL
    private var mEditingMessage: BaseMessage? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mIMM = activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        mFileProgressHandlerMap = HashMap()

        mChannelUrl = if (savedInstanceState != null) {
            // Get channel URL from saved state.
            savedInstanceState.getString(STATE_CHANNEL_URL)
        } else {
            // Get channel URL from GroupChannelListFragment.
            arguments!!.getString(GroupChannelListFragment.EXTRA_GROUP_CHANNEL_URL)
        }

        Log.d(LOG_TAG, mChannelUrl)

        mChatAdapter = GroupChatAdapter(activity)
        setUpChatListAdapter()

        // Load messages from cache.
        mChatAdapter!!.load(mChannelUrl!!)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_group_chat, container, false)

        retainInstance = true

        mRootLayout = rootView.findViewById(R.id.layout_group_chat_root)
        mRecyclerView = rootView.findViewById(R.id.recycler_group_chat)

        mCurrentEventLayout = rootView.findViewById(R.id.layout_group_chat_current_event)
        mCurrentEventText = rootView.findViewById(R.id.text_group_chat_current_event)

        mMessageEditText = rootView.findViewById(R.id.edittext_group_chat_message)
        mMessageSendButton = rootView.findViewById(R.id.button_group_chat_send)
        mUploadFileButton = rootView.findViewById(R.id.button_group_chat_upload)

        mMessageEditText!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
                mMessageSendButton!!.isEnabled = s.isNotEmpty()
            }
        })

        mMessageSendButton!!.isEnabled = false
        mMessageSendButton!!.setOnClickListener {
            if (mCurrentState == STATE_EDIT) {
                val userInput = mMessageEditText!!.text.toString()
                if (userInput.isNotEmpty()) {
                    if (mEditingMessage != null) {
                        editMessage(mEditingMessage!!, userInput)
                    }
                }
                setState(STATE_NORMAL, null, -1)
            } else {
                val userInput = mMessageEditText!!.text.toString()
                if (userInput.isNotEmpty()) {
                    sendUserMessage(userInput)
                    mMessageEditText!!.setText("")
                }
            }
        }

        mUploadFileButton!!.setOnClickListener { requestMedia() }

        mIsTyping = false
        mMessageEditText!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (!mIsTyping) {
                    setTypingStatus(true)
                }

                if (s.isEmpty()) {
                    setTypingStatus(false)
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })

        setUpRecyclerView()

        setHasOptionsMenu(true)

        return rootView
    }

    private fun refresh() {
        if (mChannel == null) {
            GroupChannel.getChannel(mChannelUrl, GroupChannel.GroupChannelGetHandler { groupChannel, e ->
                if (e != null) {
                    // Error!
                    e.printStackTrace()
                    return@GroupChannelGetHandler
                }

                mChannel = groupChannel
                mChatAdapter!!.setChannel(mChannel!!)
                mChatAdapter!!.loadLatestMessages(30, BaseChannel.GetMessagesHandler { _, _ ->
                    mChatAdapter!!.markAllMessagesAsRead()
                })
                updateActionBarTitle()
            })
        } else {
            mChannel!!.refresh(GroupChannel.GroupChannelRefreshHandler { e ->
                if (e != null) {
                    // Error!
                    e.printStackTrace()
                    return@GroupChannelRefreshHandler
                }

                mChatAdapter!!.loadLatestMessages(30, BaseChannel.GetMessagesHandler { _, _ ->
                    mChatAdapter!!.markAllMessagesAsRead()
                })
                updateActionBarTitle()
            })
        }
    }

    override fun onResume() {
        super.onResume()

        mChatAdapter!!.setContext(activity!!) // Glide bug fix (java.lang.IllegalArgumentException: You cannot start a load for a destroyed activity)

        // Gets channel from URL user requested

        Log.d(LOG_TAG, mChannelUrl)

        SendBird.addChannelHandler(CHANNEL_HANDLER_ID, object : SendBird.ChannelHandler() {
            override fun onMessageReceived(baseChannel: BaseChannel, baseMessage: BaseMessage) {
                if (baseChannel.url == mChannelUrl) {
                    mChatAdapter!!.markAllMessagesAsRead()
                    // Add new message to view
                    mChatAdapter!!.addFirst(baseMessage)
                }
            }

            override fun onMessageDeleted(baseChannel: BaseChannel?, msgId: Long) {
                super.onMessageDeleted(baseChannel, msgId)
                if (baseChannel!!.url == mChannelUrl) {
                    mChatAdapter!!.delete(msgId)
                }
            }

            override fun onMessageUpdated(channel: BaseChannel?, message: BaseMessage?) {
                super.onMessageUpdated(channel, message)
                if (channel!!.url == mChannelUrl) {
                    mChatAdapter!!.update(message!!)
                }
            }

            override fun onReadReceiptUpdated(channel: GroupChannel?) {
                if (channel!!.url == mChannelUrl) {
                    mChatAdapter!!.notifyDataSetChanged()
                }
            }

            override fun onTypingStatusUpdated(channel: GroupChannel?) {
                if (channel!!.url == mChannelUrl) {
                    val typingUsers = channel.typingMembers
                    displayTyping(typingUsers)
                }
            }

        })

        SendBird.addConnectionHandler(CONNECTION_HANDLER_ID, object : SendBird.ConnectionHandler {
            override fun onReconnectStarted() {}

            override fun onReconnectSucceeded() {
                refresh()
            }

            override fun onReconnectFailed() {}
        })

        if (SendBird.getConnectionState() == SendBird.ConnectionState.OPEN) {
            refresh()
        } else {
            if (SendBird.reconnect()) {
                // Will call onReconnectSucceeded()
            } else {
                val userId = PreferenceUtils.getUserId(activity!!)
                if (userId == null) {
                    Toast.makeText(activity, "Require user ID to connect to SendBird.", Toast.LENGTH_LONG).show()
                    return
                }

                SendBird.connect(userId, SendBird.ConnectHandler { user, e ->
                    if (e != null) {
                        e.printStackTrace()
                        return@ConnectHandler
                    }

                    refresh()
                })
            }
        }
    }

    override fun onPause() {
        setTypingStatus(false)

        SendBird.removeChannelHandler(CHANNEL_HANDLER_ID)
        SendBird.removeConnectionHandler(CONNECTION_HANDLER_ID)
        super.onPause()
    }

    override fun onDestroy() {
        // Save messages to cache.
        mChatAdapter!!.save()

        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_CHANNEL_URL, mChannelUrl)

        super.onSaveInstanceState(outState)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_group_chat, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        when (id) {
            R.id.action_group_channel_invite -> {
                val intent = Intent(activity, InviteMemberActivity::class.java)
                intent.putExtra(EXTRA_CHANNEL_URL, mChannelUrl)
                startActivity(intent)
                return true
            }
            R.id.action_group_channel_view_members -> {
                val intent = Intent(activity, MemberListActivity::class.java)
                intent.putExtra(EXTRA_CHANNEL_URL, mChannelUrl)
                startActivity(intent)
                return true
            }
            R.id.action_group_channel_leave -> AlertDialog.Builder(activity!!)
                    .setTitle("Leave channel " + mChannel!!.name + "?")
                    .setPositiveButton("Leave") { dialog, which -> leaveChannel(mChannel!!) }
                    .setNegativeButton("Cancel", null)
                    .create().show()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == INTENT_REQUEST_CHOOSE_MEDIA && resultCode == Activity.RESULT_OK) {
            // If user has successfully chosen the image, show a dialog to confirm upload.
            if (data == null) {
                Log.d(LOG_TAG, "data is null!")
                return
            }

            sendFileWithThumbnail(data.data)
        }

        // Set this as true to restore background connection management.
        SendBird.setAutoBackgroundDetection(true)
    }

    private fun setUpRecyclerView() {
        mLayoutManager = LinearLayoutManager(activity)
        mLayoutManager!!.reverseLayout = true
        mRecyclerView!!.layoutManager = mLayoutManager
        mRecyclerView!!.adapter = mChatAdapter
        mRecyclerView!!.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (mLayoutManager!!.findLastVisibleItemPosition() == mChatAdapter!!.itemCount - 1) {
                    mChatAdapter!!.loadPreviousMessages(30, null)
                }
            }
        })
    }

    private fun setUpChatListAdapter() {
        mChatAdapter!!.setItemClickListener(object : GroupChatAdapter.OnItemClickListener {
            override fun onUserMessageItemClick(message: UserMessage) {
                // Restore failed message and remove the failed message from list.
                if (mChatAdapter!!.isFailedMessage(message)) {
                    retryFailedMessage(message)
                    return
                }

                // Message is sending. Do nothing on click event.
                if (mChatAdapter!!.isTempMessage(message)) {
                    return
                }


                if (message.customType == GroupChatAdapter.URL_PREVIEW_CUSTOM_TYPE) {
                    try {
                        val info = UrlPreviewInfo(message.data)
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(info.url))
                        startActivity(browserIntent)
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }

                }
            }

            override fun onFileMessageItemClick(message: FileMessage) {
                // Load media chooser and remove the failed message from list.
                if (mChatAdapter!!.isFailedMessage(message)) {
                    retryFailedMessage(message)
                    return
                }

                // Message is sending. Do nothing on click event.
                if (mChatAdapter!!.isTempMessage(message)) {
                    return
                }


                onFileMessageClicked(message)
            }
        })

        mChatAdapter!!.setItemLongClickListener(object : GroupChatAdapter.OnItemLongClickListener {
            override fun onUserMessageItemLongClick(message: UserMessage, position: Int) {
                showMessageOptionsDialog(message, position)
            }

            override fun onFileMessageItemLongClick(message: FileMessage) {}

            override fun onAdminMessageItemLongClick(message: AdminMessage) {}
        })
    }

    private fun showMessageOptionsDialog(message: BaseMessage, position: Int) {
        val options = arrayOf("Edit message", "Delete message")

        val builder = AlertDialog.Builder(activity!!)
        builder.setItems(options) { dialog, which ->
            if (which == 0) {
                setState(STATE_EDIT, message, position)
            } else if (which == 1) {
                deleteMessage(message)
            }
        }
        builder.create().show()
    }

    private fun setState(state: Int, editingMessage: BaseMessage?, position: Int) {
        when (state) {
            STATE_NORMAL -> {
                mCurrentState = STATE_NORMAL
                mEditingMessage = null

                mUploadFileButton!!.visibility = View.VISIBLE
                mMessageSendButton!!.text = "SEND"
                mMessageEditText!!.setText("")
            }

            STATE_EDIT -> {
                mCurrentState = STATE_EDIT
                mEditingMessage = editingMessage

                mUploadFileButton!!.visibility = View.GONE
                mMessageSendButton!!.text = "SAVE"
                var messageString: String? = (editingMessage as UserMessage).message
                if (messageString == null) {
                    messageString = ""
                }
                mMessageEditText!!.setText(messageString)
                if (messageString.isNotEmpty()) {
                    mMessageEditText!!.setSelection(0, messageString.length)
                }

                mMessageEditText!!.requestFocus()
                mIMM!!.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)

                mRecyclerView!!.postDelayed({ mRecyclerView!!.scrollToPosition(position) }, 500)
            }
        }//                mIMM.hideSoftInputFromWindow(mMessageEditText.getWindowToken(), 0);
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (context as GroupChannelActivity).setOnBackPressedListener(object : GroupChannelActivity.OnBackPressedListener {
            override fun onBack(): Boolean {
                if (mCurrentState == STATE_EDIT) {
                    setState(STATE_NORMAL, null, -1)
                    return true
                }
                return false
            }
        })
    }

    private fun retryFailedMessage(message: BaseMessage) {
        AlertDialog.Builder(activity!!)
                .setMessage("Retry?")
                .setPositiveButton(R.string.resend_message) { dialog, which ->
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        if (message is UserMessage) {
                            val userInput = message.message
                            sendUserMessage(userInput)
                        } else if (message is FileMessage) {
                            val uri = mChatAdapter!!.getTempFileMessageUri(message)
                            sendFileWithThumbnail(uri)
                        }
                        mChatAdapter!!.removeFailedMessage(message)
                    }
                }
                .setNegativeButton(R.string.delete_message) { dialog, which ->
                    if (which == DialogInterface.BUTTON_NEGATIVE) {
                        mChatAdapter!!.removeFailedMessage(message)
                    }
                }.show()
    }

    /**
     * Display which users are typing.
     * If more than two users are currently typing, this will state that "multiple users" are typing.
     *
     * @param typingUsers The list of currently typing users.
     */
    private fun displayTyping(typingUsers: List<Member>) {

        if (typingUsers.size > 0) {
            mCurrentEventLayout!!.visibility = View.VISIBLE
            val string: String

            if (typingUsers.size == 1) {
                string = typingUsers[0].nickname + " is typing"
            } else if (typingUsers.size == 2) {
                string = typingUsers[0].nickname + " " + typingUsers[1].nickname + " is typing"
            } else {
                string = "Multiple users are typing"
            }
            mCurrentEventText!!.text = string
        } else {
            mCurrentEventLayout!!.visibility = View.GONE
        }
    }

    private fun requestMedia() {
        if (ContextCompat.checkSelfPermission(activity!!, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // If storage permissions are not granted, request permissions at run-time,
            // as per < API 23 guidelines.
            requestStoragePermissions()
        } else {
            val intent = Intent()

            // Pick images or videos
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                intent.type = "*/*"
                val mimeTypes = arrayOf("image/*", "video/*")
                intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            } else {
                intent.type = "image/* video/*"
            }

            intent.action = Intent.ACTION_GET_CONTENT

            // Always show the chooser (if there are multiple options available)
            startActivityForResult(Intent.createChooser(intent, "Select Media"), INTENT_REQUEST_CHOOSE_MEDIA)

            // Set this as false to maintain connection
            // even when an external Activity is started.
            SendBird.setAutoBackgroundDetection(false)
        }
    }

    private fun requestStoragePermissions() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity!!,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            // Provide an additional rationale to the user if the permission was not granted
            // and the user would benefit from additional context for the use of the permission.
            // For example if the user has previously denied the permission.
            Snackbar.make(mRootLayout!!, "Storage access permissions are required to upload/download files.",
                    Snackbar.LENGTH_LONG)
                    .setAction("Okay") {
                        requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                                PERMISSION_WRITE_EXTERNAL_STORAGE)
                    }
                    .show()
        } else {
            // Permission has not been granted yet. Request it directly.
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    PERMISSION_WRITE_EXTERNAL_STORAGE)
        }
    }

    private fun onFileMessageClicked(message: FileMessage) {
        val type = message.type.toLowerCase()
        if (type.startsWith("image")) {
            val i = Intent(activity, PhotoViewerActivity::class.java)
            i.putExtra("url", message.url)
            i.putExtra("type", message.type)
            startActivity(i)
        } else if (type.startsWith("video")) {
            val intent = Intent(activity, MediaPlayerActivity::class.java)
            intent.putExtra("url", message.url)
            startActivity(intent)
        } else {
            showDownloadConfirmDialog(message)
        }
    }

    private fun showDownloadConfirmDialog(message: FileMessage) {
        if (ContextCompat.checkSelfPermission(activity!!, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // If storage permissions are not granted, request permissions at run-time,
            // as per < API 23 guidelines.
            requestStoragePermissions()
        } else {
            AlertDialog.Builder(activity!!)
                    .setMessage("Download file?")
                    .setPositiveButton(R.string.download) { _, which ->
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            FileUtils.downloadFile(activity!!, message.url, message.name)
                        }
                    }
                    .setNegativeButton(R.string.cancel, null).show()
        }

    }

    private fun updateActionBarTitle() {
        var title = ""

        if (mChannel != null) {
            title = TextUtils.getGroupChannelTitle(mChannel!!)
        }

        // Set action bar title to name of channel
        if (activity != null) {
            (activity as GroupChannelActivity).setActionBarTitle(title)
        }
    }

    private fun sendUserMessageWithUrl(text: String, url: String) {
        object : WebUtils.UrlPreviewAsyncTask() {
            override fun onPostExecute(info: UrlPreviewInfo) {
                var tempUserMessage: UserMessage?
                val handler = BaseChannel.SendUserMessageHandler { userMessage, e ->
                    if (e != null) {
                        // Error!
                        Log.e(LOG_TAG, e.toString())
                        Toast.makeText(
                                activity,
                                "Send failed with error " + e.code + ": " + e.message, Toast.LENGTH_SHORT)
                                .show()
                        mChatAdapter!!.markMessageFailed(userMessage.requestId)
                        return@SendUserMessageHandler
                    }

                    // Update a sent message to RecyclerView
                    mChatAdapter!!.markMessageSent(userMessage)
                }

                try {
                    // Sending a message with URL preview information and custom type.
                    val jsonString = info.toJsonString()
                    tempUserMessage = mChannel!!.sendUserMessage(text, jsonString, GroupChatAdapter.URL_PREVIEW_CUSTOM_TYPE, handler)
                } catch (e: Exception) {
                    // Sending a message without URL preview information.
                    tempUserMessage = mChannel!!.sendUserMessage(text, handler)
                }


                // Display a user message to RecyclerView
                mChatAdapter!!.addFirst(tempUserMessage!!)
            }
        }.execute(url)
    }

    private fun sendUserMessage(text: String) {
        val urls = WebUtils.extractUrls(text)
        if (urls.size > 0) {
            sendUserMessageWithUrl(text, urls.get(0))
            return
        }

        val tempUserMessage = mChannel!!.sendUserMessage(text, null, GROUP_MESSAGE, BaseChannel.SendUserMessageHandler { userMessage, e ->
            if (e != null) {
                // Error!
                Log.e(LOG_TAG, e.toString())
                Toast.makeText(
                        activity,
                        "Send failed with error " + e.code + ": " + e.message, Toast.LENGTH_SHORT)
                        .show()
                mChatAdapter!!.markMessageFailed(userMessage.requestId)
                return@SendUserMessageHandler
            }

            // Update a sent message to RecyclerView
            mChatAdapter!!.markMessageSent(userMessage)
        })

        // Display a user message to RecyclerView
        mChatAdapter!!.addFirst(tempUserMessage)
    }

    /**
     * Notify other users whether the current user is typing.
     *
     * @param typing Whether the user is currently typing.
     */
    private fun setTypingStatus(typing: Boolean) {
        if (mChannel == null) {
            return
        }

        if (typing) {
            mIsTyping = true
            mChannel!!.startTyping()
        } else {
            mIsTyping = false
            mChannel!!.endTyping()
        }
    }

    /**
     * Sends a File Message containing an image file.
     * Also requests thumbnails to be generated in specified sizes.
     *
     * @param uri The URI of the image, which in this case is received through an Intent request.
     */
    private fun sendFileWithThumbnail(uri: Uri?) {
        // Specify two dimensions of thumbnails to generate
        val thumbnailSizes = ArrayList<FileMessage.ThumbnailSize>()
        thumbnailSizes.add(FileMessage.ThumbnailSize(240, 240))
        thumbnailSizes.add(FileMessage.ThumbnailSize(320, 320))

        val info = FileUtils.getFileInfo(activity!!, uri!!)

        if (info == null) {
            Toast.makeText(activity, "Extracting file information failed.", Toast.LENGTH_LONG).show()
            return
        }

        val path = info["path"] as String
        val file = File(path)
        val name = file.name
        val mime = info["mime"] as String
        val size = info["size"] as Int

        if (path == "") {
            Toast.makeText(activity, "File must be located in local storage.", Toast.LENGTH_LONG).show()
        } else {
            val progressHandler = object : BaseChannel.SendFileMessageWithProgressHandler {
                override fun onProgress(bytesSent: Int, totalBytesSent: Int, totalBytesToSend: Int) {
                    val fileMessage = mFileProgressHandlerMap!![this]
                    if (fileMessage != null && totalBytesToSend > 0) {
                        val percent = totalBytesSent * 100 / totalBytesToSend
                        mChatAdapter!!.setFileProgressPercent(fileMessage, percent)
                    }
                }

                override fun onSent(fileMessage: FileMessage, e: SendBirdException?) {
                    if (e != null) {
                        Toast.makeText(activity, "" + e.code + ":" + e.message, Toast.LENGTH_SHORT).show()
                        mChatAdapter!!.markMessageFailed(fileMessage.requestId)
                        return
                    }

                    mChatAdapter!!.markMessageSent(fileMessage)
                }
            }

            // Send image with thumbnails in the specified dimensions
            val tempFileMessage = mChannel!!.sendFileMessage(file, name, mime, size, "", null, thumbnailSizes, progressHandler)

            mFileProgressHandlerMap!![progressHandler] = tempFileMessage

            mChatAdapter!!.addTempFileMessageInfo(tempFileMessage, uri)
            mChatAdapter!!.addFirst(tempFileMessage)
        }
    }

    private fun editMessage(message: BaseMessage, editedMessage: String) {
        mChannel!!.updateUserMessage(message.messageId, editedMessage, null, null, BaseChannel.UpdateUserMessageHandler { userMessage, e ->
            if (e != null) {
                // Error!
                Toast.makeText(activity, "Error " + e.code + ": " + e.message, Toast.LENGTH_SHORT).show()
                return@UpdateUserMessageHandler
            }

            mChatAdapter!!.loadLatestMessages(30, BaseChannel.GetMessagesHandler { _, _ ->
                mChatAdapter!!.markAllMessagesAsRead()
            })
        })
    }

    /**
     * Deletes a message within the channel.
     * Note that users can only delete messages sent by oneself.
     *
     * @param message The message to delete.
     */
    private fun deleteMessage(message: BaseMessage) {
        mChannel!!.deleteMessage(message, BaseChannel.DeleteMessageHandler { e ->
            if (e != null) {
                // Error!
                Toast.makeText(activity, "Error " + e.code + ": " + e.message, Toast.LENGTH_SHORT).show()
                return@DeleteMessageHandler
            }

            mChatAdapter!!.loadLatestMessages(30, BaseChannel.GetMessagesHandler { list, e ->
                mChatAdapter!!.markAllMessagesAsRead()
            })
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
            fragmentManager!!.popBackStack()
        })
    }

    companion object {

        private const val CONNECTION_HANDLER_ID = "CONNECTION_HANDLER_GROUP_CHAT"

        private val LOG_TAG = GroupChatFragment::class.java.simpleName

        private const val STATE_NORMAL = 0
        private const val STATE_EDIT = 1

        private const val CHANNEL_HANDLER_ID = "CHANNEL_HANDLER_GROUP_CHANNEL_CHAT"
        private const val STATE_CHANNEL_URL = "STATE_CHANNEL_URL"
        private const val INTENT_REQUEST_CHOOSE_MEDIA = 301
        private const val PERMISSION_WRITE_EXTERNAL_STORAGE = 13
        internal const val EXTRA_CHANNEL_URL = "EXTRA_CHANNEL_URL"
        private const val GROUP_MESSAGE = "Group"

        /**
         * To create an instance of this fragment, a Channel URL should be required.
         */
        fun newInstance(channelUrl: String): GroupChatFragment {
            val fragment = GroupChatFragment()

            val args = Bundle()
            args.putString(GroupChannelListFragment.EXTRA_GROUP_CHANNEL_URL, channelUrl)
            fragment.arguments = args

            return fragment
        }
    }
}
