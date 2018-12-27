package th.co.digio.chatapp.demo.openchannel

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
import th.co.digio.chatapp.demo.utils.FileUtils
import th.co.digio.chatapp.demo.utils.MediaPlayerActivity
import th.co.digio.chatapp.demo.utils.PhotoViewerActivity
import java.io.File
import java.util.*

class OpenChatFragment : Fragment() {

    private var mIMM: InputMethodManager? = null

    private var mRecyclerView: RecyclerView? = null
    private var mChatAdapter: OpenChatAdapter? = null
    private var mLayoutManager: LinearLayoutManager? = null
    private var mRootLayout: View? = null
    private var mMessageEditText: EditText? = null
    private var mMessageSendButton: Button? = null
    private var mUploadFileButton: ImageButton? = null
    private var mCurrentEventLayout: View? = null
    private var mCurrentEventText: TextView? = null

    private var mChannel: OpenChannel? = null
    private var mChannelUrl: String? = null
    private var mPrevMessageListQuery: PreviousMessageListQuery? = null

    private var mCurrentState = STATE_NORMAL
    private var mEditingMessage: BaseMessage? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mIMM = activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_open_chat, container, false)

        retainInstance = true

        setHasOptionsMenu(true)

        mRootLayout = rootView.findViewById(R.id.layout_open_chat_root)

        mRecyclerView = rootView.findViewById(R.id.recycler_open_channel_chat)

        mCurrentEventLayout = rootView.findViewById(R.id.layout_open_chat_current_event)
        mCurrentEventText = rootView.findViewById(R.id.text_open_chat_current_event)

        setUpChatAdapter()
        setUpRecyclerView()

        // Set up chat box
        mMessageSendButton = rootView.findViewById(R.id.button_open_channel_chat_send)
        mMessageEditText = rootView.findViewById(R.id.edittext_chat_message)

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
                        editMessage(mEditingMessage!!, mMessageEditText!!.text.toString())
                    }
                }
                setState(STATE_NORMAL, null)
            } else {
                val userInput = mMessageEditText!!.text.toString()
                if (userInput.isNotEmpty()) {
                    sendUserMessage(userInput)
                    mMessageEditText!!.setText("")
                }
            }
        }

        mUploadFileButton = rootView.findViewById(R.id.button_open_channel_chat_upload)
        mUploadFileButton!!.setOnClickListener { requestImage() }


        // Gets channel from URL user requested
        mChannelUrl = arguments!!.getString(OpenChannelListFragment.EXTRA_OPEN_CHANNEL_URL)
        enterChannel(mChannelUrl)

        return rootView
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_WRITE_EXTERNAL_STORAGE ->

                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // Permission granted.
                    Snackbar.make(mRootLayout!!, "Storage permissions granted. You can now upload or download files.",
                            Snackbar.LENGTH_LONG)
                            .show()
                } else {
                    // Permission denied.
                    Snackbar.make(mRootLayout!!, "Permissions denied.",
                            Snackbar.LENGTH_SHORT)
                            .show()
                }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == INTENT_REQUEST_CHOOSE_IMAGE && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                Log.d(LOG_TAG, "data is null!")
                return
            }
            showUploadConfirmDialog(data.data)
        }
    }

    override fun onResume() {
        super.onResume()

        // Set this as true to restart auto-background detection.
        // This means that you will be automatically disconnected from SendBird when your
        // app enters the background.
        SendBird.setAutoBackgroundDetection(true)

        SendBird.addConnectionHandler(CONNECTION_HANDLER_ID, object : SendBird.ConnectionHandler {
            override fun onReconnectStarted() {
                Log.d("CONNECTION", "OpenChatFragment onReconnectStarted()")
            }

            override fun onReconnectSucceeded() {
                Log.d("CONNECTION", "OpenChatFragment onReconnectSucceeded()")
            }

            override fun onReconnectFailed() {
                Log.d("CONNECTION", "OpenChatFragment onReconnectFailed()")
            }
        })

        SendBird.addChannelHandler(CHANNEL_HANDLER_ID, object : SendBird.ChannelHandler() {
            override fun onMessageReceived(baseChannel: BaseChannel, baseMessage: BaseMessage) {
                // Add new message to view
                if (baseChannel.url == mChannelUrl) {
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
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()

        mChannel!!.exit(OpenChannel.OpenChannelExitHandler { e ->
            if (e != null) {
                // Error!
                e.printStackTrace()
                return@OpenChannelExitHandler
            }
        })
    }

    override fun onPause() {
        super.onPause()

        SendBird.removeChannelHandler(CHANNEL_HANDLER_ID)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_open_chat, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.action_open_chat_view_participants) {
            val intent = Intent(activity, ParticipantListActivity::class.java)
            intent.putExtra(EXTRA_CHANNEL_URL, mChannel!!.url)
            startActivity(intent)

            return true
        }

        return super.onOptionsItemSelected(item)
    }

    private fun setUpChatAdapter() {
        mChatAdapter = OpenChatAdapter(activity!!)
        mChatAdapter!!.setOnItemClickListener(object : OpenChatAdapter.OnItemClickListener {
            override fun onUserMessageItemClick(message: UserMessage) {}

            override fun onFileMessageItemClick(message: FileMessage) {
                onFileMessageClicked(message)
            }

            override fun onAdminMessageItemClick(message: AdminMessage) {}
        })

        mChatAdapter!!.setOnItemLongClickListener(object : OpenChatAdapter.OnItemLongClickListener {
            override fun onBaseMessageLongClick(message: BaseMessage) {
                showMessageOptionsDialog(message)
            }
        })
    }

    private fun showMessageOptionsDialog(message: BaseMessage) {
        val options = arrayOf("Edit message", "Delete message")

        val builder = AlertDialog.Builder(activity!!)
        builder.setItems(options) { dialog, which ->
            if (which == 0) {
                setState(STATE_EDIT, message)
            } else if (which == 1) {
                deleteMessage(message)
            }
        }
        builder.create().show()
    }

    private fun setState(state: Int, editingMessage: BaseMessage?) {
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
            }
        }//                mIMM.hideSoftInputFromWindow(mMessageEditText.getWindowToken(), 0);
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (context as OpenChannelActivity).setOnBackPressedListener(object : OpenChannelActivity.OnBackPressedListener {
            override fun onBack(): Boolean {
                if (mCurrentState == STATE_EDIT) {
                    setState(STATE_NORMAL, null)
                    return true
                }
                return false
            }
        })
    }

    private fun setUpRecyclerView() {
        mLayoutManager = LinearLayoutManager(activity)
        mLayoutManager!!.reverseLayout = true
        mRecyclerView!!.layoutManager = mLayoutManager
        mRecyclerView!!.adapter = mChatAdapter

        // Load more messages when user reaches the top of the current message list.
        mRecyclerView!!.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {

                if (mLayoutManager!!.findLastVisibleItemPosition() == mChatAdapter!!.itemCount - 1) {
                    loadNextMessageList(30)
                }
                Log.v(LOG_TAG, "onScrollStateChanged")
            }
        })
    }

    private fun onFileMessageClicked(message: FileMessage) {
        val type = message.type.toLowerCase()
        when {
            type.startsWith("image") -> {
                val i = Intent(activity, PhotoViewerActivity::class.java)
                i.putExtra("url", message.url)
                i.putExtra("type", message.type)
                startActivity(i)
            }
            type.startsWith("video") -> {
                val intent = Intent(activity, MediaPlayerActivity::class.java)
                intent.putExtra("url", message.url)
                startActivity(intent)
            }
            else -> showDownloadConfirmDialog(message)
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
                    .setPositiveButton(R.string.download) { dialog, which ->
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            FileUtils.downloadFile(activity!!, message.url, message.name)
                        }
                    }
                    .setNegativeButton(R.string.cancel, null).show()
        }

    }

    private fun showUploadConfirmDialog(uri: Uri?) {
        AlertDialog.Builder(activity!!)
                .setMessage("Upload file?")
                .setPositiveButton(R.string.upload) { dialog, which ->
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        // Specify two dimensions of thumbnails to generate
                        val thumbnailSizes = ArrayList<FileMessage.ThumbnailSize>()
                        thumbnailSizes.add(FileMessage.ThumbnailSize(240, 240))
                        thumbnailSizes.add(FileMessage.ThumbnailSize(320, 320))
                        sendImageWithThumbnail(uri, thumbnailSizes)
                    }
                }
                .setNegativeButton(R.string.cancel, null).show()
    }

    private fun requestImage() {
        if (ContextCompat.checkSelfPermission(activity!!, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // If storage permissions are not granted, request permissions at run-time,
            // as per < API 23 guidelines.
            requestStoragePermissions()
        } else {
            val intent = Intent()
            // Show only images, no videos or anything else
            intent.type = "image/* video/*"
            intent.action = Intent.ACTION_GET_CONTENT
            // Always show the chooser (if there are multiple options available)
            startActivityForResult(Intent.createChooser(intent, "Select Media"), INTENT_REQUEST_CHOOSE_IMAGE)

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

    /**
     * Enters an Open Channel.
     *
     *
     * A user must successfully enter a channel before being able to load or send messages
     * within the channel.
     *
     * @param channelUrl The URL of the channel to enter.
     */
    private fun enterChannel(channelUrl: String?) {
        OpenChannel.getChannel(channelUrl, OpenChannel.OpenChannelGetHandler { openChannel, e ->
            if (e != null) {
                // Error!
                e.printStackTrace()
                return@OpenChannelGetHandler
            }

            // Enter the channel
            openChannel.enter(OpenChannel.OpenChannelEnterHandler { e ->
                if (e != null) {
                    // Error!
                    e.printStackTrace()
                    return@OpenChannelEnterHandler
                }

                mChannel = openChannel
                loadInitialMessageList(30)

                // Set action bar title to name of channel
                (activity as OpenChannelActivity).setActionBarTitle(mChannel!!.name)
            })
        })
    }

    private fun sendUserMessage(text: String) {
        mChannel!!.sendUserMessage(text, BaseChannel.SendUserMessageHandler { userMessage, e ->
            if (e != null) {
                // Error!
                Log.e(LOG_TAG, e.toString())
                Toast.makeText(
                        activity,
                        "Send failed with error " + e.code + ": " + e.message, Toast.LENGTH_SHORT)
                        .show()
                return@SendUserMessageHandler
            }

            // Display sent message to RecyclerView
            mChatAdapter!!.addFirst(userMessage)
        })
    }

    /**
     * Sends a File Message containing an image file.
     * Also requests thumbnails to be generated in specified sizes.
     *
     * @param uri The URI of the image, which in this case is received through an Intent request.
     */
    private fun sendImageWithThumbnail(uri: Uri?, thumbnailSizes: List<FileMessage.ThumbnailSize>) {
        val info = FileUtils.getFileInfo(activity!!, uri!!)
        val path = info!!["path"] as String
        val file = File(path)
        val name = file.name
        val mime = info["mime"] as String
        val size = info["size"] as Int

        if (path == "") {
            Toast.makeText(activity, "File must be located in local storage.", Toast.LENGTH_LONG).show()
        } else {
            // Send image with thumbnails in the specified dimensions
            mChannel!!.sendFileMessage(file, name, mime, size, "", null, thumbnailSizes, BaseChannel.SendFileMessageHandler { fileMessage, e ->
                if (e != null) {
                    Toast.makeText(activity, "" + e.code + ":" + e.message, Toast.LENGTH_SHORT).show()
                    return@SendFileMessageHandler
                }

                mChatAdapter!!.addFirst(fileMessage)
            })
        }
    }

    /**
     * Replaces current message list with new list.
     * Should be used only on initial load.
     */
    private fun loadInitialMessageList(numMessages: Int) {

        mPrevMessageListQuery = mChannel!!.createPreviousMessageListQuery()
        mPrevMessageListQuery!!.load(numMessages, true, PreviousMessageListQuery.MessageListQueryResult { list, e ->
            if (e != null) {
                // Error!
                e.printStackTrace()
                return@MessageListQueryResult
            }

            mChatAdapter!!.setMessageList(list)
        })

    }

    /**
     * Loads messages and adds them to current message list.
     *
     *
     * A PreviousMessageListQuery must have been already initialized through [.loadInitialMessageList]
     */
    @Throws(NullPointerException::class)
    private fun loadNextMessageList(numMessages: Int) {

        if (mChannel == null) {
            throw NullPointerException("Current channel instance is null.")
        }

        if (mPrevMessageListQuery == null) {
            throw NullPointerException("Current query instance is null.")
        }

        mPrevMessageListQuery!!.load(numMessages, true, PreviousMessageListQuery.MessageListQueryResult { list, e ->
            if (e != null) {
                // Error!
                e.printStackTrace()
                return@MessageListQueryResult
            }

            for (message in list) {
                mChatAdapter!!.addLast(message)
            }
        })
    }

    private fun editMessage(message: BaseMessage, editedMessage: String) {
        mChannel!!.updateUserMessage(message.messageId, editedMessage, null, null, BaseChannel.UpdateUserMessageHandler { userMessage, e ->
            if (e != null) {
                // Error!
                Toast.makeText(activity, "Error " + e.code + ": " + e.message, Toast.LENGTH_SHORT).show()
                return@UpdateUserMessageHandler
            }

            loadInitialMessageList(30)
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

            loadInitialMessageList(30)
        })
    }

    companion object {

        private val LOG_TAG = OpenChatFragment::class.java.simpleName

        private const val STATE_NORMAL = 0
        private const val STATE_EDIT = 1

        private const val CHANNEL_HANDLER_ID = "CHANNEL_HANDLER_OPEN_CHAT"
        private const val CONNECTION_HANDLER_ID = "CONNECTION_HANDLER_OPEN_CHAT"

        private const val INTENT_REQUEST_CHOOSE_IMAGE = 300
        private const val PERMISSION_WRITE_EXTERNAL_STORAGE = 13

        internal const val EXTRA_CHANNEL_URL = "CHANNEL_URL"

        /**
         * To create an instance of this fragment, a Channel URL should be passed.
         */
        fun newInstance(channelUrl: String): OpenChatFragment {
            val fragment = OpenChatFragment()

            val args = Bundle()
            args.putString(OpenChannelListFragment.EXTRA_OPEN_CHANNEL_URL, channelUrl)
            fragment.arguments = args

            return fragment
        }
    }
}
