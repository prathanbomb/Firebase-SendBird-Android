package th.co.digio.chatapp.demo.main

import android.Manifest
import android.app.Activity
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import co.th.digio.chatapp.demo.R
import com.google.android.material.snackbar.Snackbar
import com.sendbird.android.SendBird
import th.co.digio.chatapp.demo.utils.*
import java.io.File
import java.util.*

class SettingsActivity : AppCompatActivity() {

    private var mIMM: InputMethodManager? = null
    private var mCalendar: Calendar? = null

    private var mNickNameChanged = false

    private var mRequestingCamera = false
    private var mTempPhotoUri: Uri? = null

    private var mSettingsLayout: CoordinatorLayout? = null
    private var mImageViewProfile: ImageView? = null
    private var mEditTextNickname: EditText? = null
    private var mButtonSaveNickName: Button? = null

    private var mLinearLayoutNotifications: LinearLayout? = null
    private var mSwitchNotifications: SwitchCompat? = null
    private var mSwitchNotificationsShowPreviews: SwitchCompat? = null

    private var mSwitchNotificationsDoNotDisturb: SwitchCompat? = null
    private var mLinearLayoutDoNotDisturb: LinearLayout? = null
    private var mLinearLayoutNotificationsDoNotDisturbFrom: LinearLayout? = null
    private var mLinearLayoutNotificationsDoNotDisturbTo: LinearLayout? = null
    private var mTextViewNotificationsDoNotDisturbFrom: TextView? = null
    private var mTextViewNotificationsDoNotDisturbTo: TextView? = null

    private var mCheckBoxGroupChannelDistinct: CheckBox? = null

    private var mLinearLayoutBlockedMembersList: LinearLayout? = null

    private var mCheckedChangeListener: CompoundButton.OnCheckedChangeListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        mIMM = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        mCalendar = Calendar.getInstance(Locale.getDefault())

        val toolbar = findViewById<Toolbar>(R.id.toolbar_settings)
        setSupportActionBar(toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setHomeAsUpIndicator(R.drawable.ic_window_close_white_24_dp)
        }

        mSettingsLayout = findViewById(R.id.layout_settings)

        mImageViewProfile = findViewById(R.id.image_view_profile)
        mEditTextNickname = findViewById(R.id.edit_text_nickname)
        mButtonSaveNickName = findViewById(R.id.button_save_nickname)

        mLinearLayoutNotifications = findViewById(R.id.linear_layout_notifications)
        mSwitchNotifications = findViewById(R.id.switch_notifications)
        mSwitchNotificationsShowPreviews = findViewById(R.id.switch_notifications_show_previews)

        mSwitchNotificationsDoNotDisturb = findViewById(R.id.switch_notifications_do_not_disturb)
        mLinearLayoutDoNotDisturb = findViewById(R.id.linear_layout_do_not_disturb)
        mLinearLayoutNotificationsDoNotDisturbFrom = findViewById(R.id.linear_layout_notifications_do_not_disturb_from)
        mLinearLayoutNotificationsDoNotDisturbTo = findViewById(R.id.linear_layout_notifications_do_not_disturb_to)
        mTextViewNotificationsDoNotDisturbFrom = findViewById(R.id.text_view_notifications_do_not_disturb_from)
        mTextViewNotificationsDoNotDisturbTo = findViewById(R.id.text_view_notifications_do_not_disturb_to)

        mCheckBoxGroupChannelDistinct = findViewById(R.id.checkbox_make_group_channel_distinct)

        mLinearLayoutBlockedMembersList = findViewById(R.id.linear_layout_blocked_members_list)
        mLinearLayoutBlockedMembersList!!.setOnClickListener {
            val intent = Intent(this@SettingsActivity, BlockedMembersListActivity::class.java)
            startActivity(intent)
        }

        //+ ProfileUrl
        val profileUrl = PreferenceUtils.getProfileUrl(this@SettingsActivity)
        if (profileUrl!!.isNotEmpty()) {
            ImageUtils.displayRoundImageFromUrlWithoutCache(this@SettingsActivity, profileUrl, mImageViewProfile!!)
        }
        mImageViewProfile!!.setOnClickListener { showSetProfileOptionsDialog() }
        //- ProfileUrl

        //+ Nickname
        mEditTextNickname!!.isEnabled = false
        val nickname = PreferenceUtils.getNickname(this@SettingsActivity)
        if (nickname!!.isNotEmpty()) {
            mEditTextNickname!!.setText(nickname)
        }
        mEditTextNickname!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (s != null && s.isNotEmpty() && s.toString() != nickname) {
                    mNickNameChanged = true
                }
            }
        })
        mButtonSaveNickName!!.setOnClickListener {
            if (mEditTextNickname!!.isEnabled) {
                if (mNickNameChanged) {
                    mNickNameChanged = false

                    updateCurrentUserInfo(mEditTextNickname!!.text.toString())
                }

                mButtonSaveNickName!!.text = "EDIT"
                mEditTextNickname!!.isEnabled = false
                mEditTextNickname!!.isFocusable = false
                mEditTextNickname!!.isFocusableInTouchMode = false
            } else {
                mButtonSaveNickName!!.text = "SAVE"
                mEditTextNickname!!.isEnabled = true
                mEditTextNickname!!.isFocusable = true
                mEditTextNickname!!.isFocusableInTouchMode = true
                if (mEditTextNickname!!.text != null && mEditTextNickname!!.text.isNotEmpty()) {
                    mEditTextNickname!!.setSelection(0, mEditTextNickname!!.text.length)
                }
                mEditTextNickname!!.requestFocus()
                mIMM!!.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
            }
        }
        //- Nickname

        //+ Notifications
        val notifications = PreferenceUtils.getNotifications(this@SettingsActivity)
        mSwitchNotifications!!.isChecked = notifications
        mSwitchNotificationsShowPreviews!!.isChecked = PreferenceUtils.getNotificationsShowPreviews(this@SettingsActivity)
        checkNotifications(notifications)

        val doNotDisturb = PreferenceUtils.getNotificationsDoNotDisturb(this@SettingsActivity)
        mSwitchNotificationsDoNotDisturb!!.isChecked = doNotDisturb
        checkDoNotDisturb(doNotDisturb)

        mSwitchNotifications!!.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                PushUtils.registerPushTokenForCurrentUser(this@SettingsActivity, SendBird.RegisterPushTokenWithStatusHandler { pushTokenRegistrationStatus, e ->
                    if (e != null) {
                        mSwitchNotifications!!.isChecked = !isChecked
                        checkNotifications(!isChecked)
                        return@RegisterPushTokenWithStatusHandler
                    }

                    PreferenceUtils.setNotifications(this@SettingsActivity, isChecked)
                    checkNotifications(isChecked)
                })
            } else {
                PushUtils.unregisterPushTokenForCurrentUser(this@SettingsActivity, SendBird.UnregisterPushTokenHandler { e ->
                    if (e != null) {
                        mSwitchNotifications!!.isChecked = !isChecked
                        checkNotifications(!isChecked)
                        return@UnregisterPushTokenHandler
                    }

                    PreferenceUtils.setNotifications(this@SettingsActivity, isChecked)
                    checkNotifications(isChecked)
                })
            }
        }

        mSwitchNotificationsShowPreviews!!.setOnCheckedChangeListener { buttonView, isChecked -> PreferenceUtils.setNotificationsShowPreviews(this@SettingsActivity, isChecked) }

        mCheckedChangeListener = CompoundButton.OnCheckedChangeListener { buttonView, isChecked -> saveDoNotDisturb(isChecked) }

        mSwitchNotificationsDoNotDisturb!!.setOnCheckedChangeListener(mCheckedChangeListener)

        mLinearLayoutNotificationsDoNotDisturbFrom!!.setOnClickListener { setDoNotDisturbTime(true, mTextViewNotificationsDoNotDisturbTo) }

        mLinearLayoutNotificationsDoNotDisturbTo!!.setOnClickListener { setDoNotDisturbTime(false, mTextViewNotificationsDoNotDisturbTo) }

        SendBird.getDoNotDisturb { isDoNotDisturbOn, startHour, startMin, endHour, endMin, timezone, e ->
            mCalendar!!.clear()
            mCalendar!!.set(Calendar.HOUR_OF_DAY, startHour)
            mCalendar!!.set(Calendar.MINUTE, startMin)
            val fromMillis = mCalendar!!.timeInMillis

            PreferenceUtils.setNotificationsDoNotDisturbFrom(this@SettingsActivity, fromMillis.toString())
            mTextViewNotificationsDoNotDisturbTo!!.text = DateUtils.formatTimeWithMarker(fromMillis)

            mCalendar!!.clear()
            mCalendar!!.set(Calendar.HOUR_OF_DAY, endHour)
            mCalendar!!.set(Calendar.MINUTE, endMin)
            val toMillis = mCalendar!!.timeInMillis

            PreferenceUtils.setNotificationsDoNotDisturbTo(this@SettingsActivity, toMillis.toString())
            mTextViewNotificationsDoNotDisturbTo!!.text = DateUtils.formatTimeWithMarker(toMillis)

            mSwitchNotificationsDoNotDisturb!!.isChecked = isDoNotDisturbOn
        }
        //- Notifications

        //+ Group Channel Distinct
        mCheckBoxGroupChannelDistinct!!.isChecked = PreferenceUtils.getGroupChannelDistinct(this@SettingsActivity)

        mCheckBoxGroupChannelDistinct!!.setOnCheckedChangeListener { buttonView, isChecked -> PreferenceUtils.setGroupChannelDistinct(this@SettingsActivity, isChecked) }
        //- Group Channel Distinct
    }

    private fun saveDoNotDisturb(doNotDisturb: Boolean) {
        val doNotDisturbFrom = PreferenceUtils.getNotificationsDoNotDisturbFrom(this@SettingsActivity)
        val doNotDisturbTo = PreferenceUtils.getNotificationsDoNotDisturbTo(this@SettingsActivity)
        if (doNotDisturbFrom!!.isNotEmpty() && doNotDisturbTo!!.isNotEmpty()) {
            val startHour = DateUtils.getHourOfDay(java.lang.Long.valueOf(doNotDisturbFrom))
            val startMin = DateUtils.getMinute(java.lang.Long.valueOf(doNotDisturbFrom))
            val endHour = DateUtils.getHourOfDay(java.lang.Long.valueOf(doNotDisturbTo))
            val endMin = DateUtils.getMinute(java.lang.Long.valueOf(doNotDisturbTo))

            SendBird.setDoNotDisturb(doNotDisturb, startHour, startMin, endHour, endMin, TimeZone.getDefault().id, SendBird.SetDoNotDisturbHandler { e ->
                if (e != null) {
                    mSwitchNotificationsDoNotDisturb!!.setOnCheckedChangeListener(null)
                    mSwitchNotificationsDoNotDisturb!!.isChecked = !doNotDisturb
                    mSwitchNotificationsDoNotDisturb!!.setOnCheckedChangeListener(mCheckedChangeListener)

                    PreferenceUtils.setNotificationsDoNotDisturb(this@SettingsActivity, !doNotDisturb)
                    checkDoNotDisturb(!doNotDisturb)
                    return@SetDoNotDisturbHandler
                }

                mSwitchNotificationsDoNotDisturb!!.setOnCheckedChangeListener(null)
                mSwitchNotificationsDoNotDisturb!!.isChecked = doNotDisturb
                mSwitchNotificationsDoNotDisturb!!.setOnCheckedChangeListener(mCheckedChangeListener)

                PreferenceUtils.setNotificationsDoNotDisturb(this@SettingsActivity, doNotDisturb)
                checkDoNotDisturb(doNotDisturb)
            })
        }
    }

    private fun setDoNotDisturbTime(from: Boolean, textView: TextView?) {
        var timeMillis = System.currentTimeMillis()
        if (from) {
            val doNotDisturbFrom = PreferenceUtils.getNotificationsDoNotDisturbFrom(this@SettingsActivity)
            if (doNotDisturbFrom!!.isNotEmpty()) {
                timeMillis = java.lang.Long.valueOf(doNotDisturbFrom)
            }
        } else {
            val doNotDisturbTo = PreferenceUtils.getNotificationsDoNotDisturbTo(this@SettingsActivity)
            if (doNotDisturbTo!!.isNotEmpty()) {
                timeMillis = java.lang.Long.valueOf(doNotDisturbTo)
            }
        }

        TimePickerDialog(this@SettingsActivity, TimePickerDialog.OnTimeSetListener { timePicker, hour, min ->
            mCalendar!!.clear()
            mCalendar!!.set(Calendar.HOUR_OF_DAY, hour)
            mCalendar!!.set(Calendar.MINUTE, min)
            val millis = mCalendar!!.timeInMillis

            if (from) {
                if (millis.toString() != PreferenceUtils.getNotificationsDoNotDisturbFrom(this@SettingsActivity)) {
                    PreferenceUtils.setNotificationsDoNotDisturbFrom(this@SettingsActivity, millis.toString())
                    saveDoNotDisturb(true)
                }
            } else {
                if (millis.toString() != PreferenceUtils.getNotificationsDoNotDisturbTo(this@SettingsActivity)) {
                    PreferenceUtils.setNotificationsDoNotDisturbTo(this@SettingsActivity, millis.toString())
                    saveDoNotDisturb(true)
                }
            }
            textView!!.text = DateUtils.formatTimeWithMarker(millis)
        }, DateUtils.getHourOfDay(timeMillis), DateUtils.getMinute(timeMillis), true).show()
    }

    private fun checkNotifications(notifications: Boolean) {
        if (notifications) {
            mLinearLayoutNotifications!!.visibility = View.VISIBLE
            val doNotDisturb = PreferenceUtils.getNotificationsDoNotDisturb(this@SettingsActivity)
            checkDoNotDisturb(doNotDisturb)
        } else {
            mLinearLayoutNotifications!!.visibility = View.GONE
        }
    }

    private fun checkDoNotDisturb(doNotDisturb: Boolean) {
        if (doNotDisturb) {
            mLinearLayoutDoNotDisturb!!.visibility = View.VISIBLE
        } else {
            mLinearLayoutDoNotDisturb!!.visibility = View.GONE
        }

        val doNotDisturbFrom = PreferenceUtils.getNotificationsDoNotDisturbFrom(this@SettingsActivity)
        if (doNotDisturbFrom!!.isNotEmpty()) {
            mTextViewNotificationsDoNotDisturbFrom!!.text = DateUtils.formatTimeWithMarker(java.lang.Long.valueOf(doNotDisturbFrom))
        } else {
            mTextViewNotificationsDoNotDisturbFrom!!.text = ""
        }

        val doNotDisturbTo = PreferenceUtils.getNotificationsDoNotDisturbTo(this@SettingsActivity)
        if (doNotDisturbTo!!.isNotEmpty()) {
            mTextViewNotificationsDoNotDisturbTo!!.text = DateUtils.formatTimeWithMarker(java.lang.Long.valueOf(doNotDisturbTo))
        } else {
            mTextViewNotificationsDoNotDisturbTo!!.text = ""
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showSetProfileOptionsDialog() {
        val options = arrayOf("Upload a photo", "Take a photo")

        val builder = AlertDialog.Builder(this@SettingsActivity)
        builder.setTitle("Set profile image")
                .setItems(options) { dialog, which ->
                    if (which == 0) {
                        requestMedia()
                    } else if (which == 1) {
                        requestCamera()
                    }
                }
        builder.create().show()
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == INTENT_REQUEST_CHOOSE_MEDIA && resultCode == Activity.RESULT_OK) {
            // If user has successfully chosen the image, show a dialog to confirm upload.
            if (data == null) {
                return
            }

            val uri = data.data
            val info = FileUtils.getFileInfo(this@SettingsActivity, uri)
            if (info != null) {
                val path = info["path"] as String?
                if (path != null) {
                    val profileImage = File(path)
                    updateCurrentUserProfileImage(profileImage, mImageViewProfile)
                }
            }
        } else if (requestCode == INTENT_REQUEST_CAMERA && resultCode == Activity.RESULT_OK) {
            if (!mRequestingCamera) {
                return
            }

            val profileImage = File(mTempPhotoUri!!.path)
            updateCurrentUserProfileImage(profileImage, mImageViewProfile)
            mRequestingCamera = false
        }

        // Set this as true to restore background connection management.
        SendBird.setAutoBackgroundDetection(true)
    }

    private fun requestMedia() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestStoragePermissions(PERMISSION_WRITE_EXTERNAL_STORAGE_UPLOAD)
        } else {
            val intent = Intent()
            // Show only images, no videos or anything else
            intent.type = "image/*"
            intent.action = Intent.ACTION_PICK
            // Always show the chooser (if there are multiple options available)
            startActivityForResult(Intent.createChooser(intent, "Select Image"), INTENT_REQUEST_CHOOSE_MEDIA)

            // Set this as false to maintain connection
            // even when an external Activity is started.
            SendBird.setAutoBackgroundDetection(false)
        }
    }

    private fun requestCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestStoragePermissions(PERMISSION_WRITE_EXTERNAL_STORAGE_CAMERA)
        } else {
            mRequestingCamera = true
            mTempPhotoUri = getTempFileUri(false)

            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, mTempPhotoUri)

            val resInfoList = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            for (resolveInfo in resInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                grantUriPermission(packageName, mTempPhotoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivityForResult(intent, INTENT_REQUEST_CAMERA)

            SendBird.setAutoBackgroundDetection(false)
        }
    }

    private fun getTempFileUri(doNotUseFileProvider: Boolean): Uri? {
        var uri: Uri? = null
        try {
            val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val tempFile = File.createTempFile("SendBird_" + System.currentTimeMillis(), ".jpg", path)

            uri = if (Build.VERSION.SDK_INT >= 24 && !doNotUseFileProvider) {
                FileProvider.getUriForFile(this, "co.th.digio.chatapp.demo.provider", tempFile)
            } else {
                Uri.fromFile(tempFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return uri
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        SendBird.setAutoBackgroundDetection(true)

        when (requestCode) {
            PERMISSION_WRITE_EXTERNAL_STORAGE_UPLOAD -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestMedia()
            }

            PERMISSION_WRITE_EXTERNAL_STORAGE_CAMERA -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestCamera()
            }
        }
    }

    private fun requestStoragePermissions(code: Int) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            // Provide an additional rationale to the user if the permission was not granted
            // and the user would benefit from additional context for the use of the permission.
            // For example if the user has previously denied the permission.
            Snackbar.make(mSettingsLayout!!, "Storage access permissions are required to upload/download files.", Snackbar.LENGTH_LONG)
                    .setAction("OK") {
                        // Maintains connection.
                        SendBird.setAutoBackgroundDetection(false)
                        ActivityCompat.requestPermissions(
                                this@SettingsActivity,
                                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                                code
                        )
                    }
                    .show()
        } else {
            // Maintains connection.
            SendBird.setAutoBackgroundDetection(false)
            // Permission has not been granted yet. Request it directly.
            ActivityCompat.requestPermissions(
                    this@SettingsActivity,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    code
            )
        }
    }

    private fun updateCurrentUserProfileImage(profileImage: File, imageView: ImageView?) {
        val nickname = PreferenceUtils.getNickname(this@SettingsActivity)
        SendBird.updateCurrentUserInfoWithProfileImage(nickname, profileImage, SendBird.UserInfoUpdateHandler { e ->
            if (e != null) {
                // Error!
                Toast.makeText(this@SettingsActivity, "" + e.code + ":" + e.message, Toast.LENGTH_SHORT).show()

                // Show update failed snackbar
                showSnackbar("Update user info failed")
                return@UserInfoUpdateHandler
            }

            try {
                PreferenceUtils.setProfileUrl(this@SettingsActivity, SendBird.getCurrentUser().profileUrl)
                ImageUtils.displayRoundImageFromUrlWithoutCache(this@SettingsActivity, Uri.fromFile(profileImage).toString(), imageView!!)
            } catch (e1: Exception) {
                e1.printStackTrace()
            }
        })
    }

    private fun updateCurrentUserInfo(userNickname: String) {
        val profileUrl = PreferenceUtils.getProfileUrl(this@SettingsActivity)
        SendBird.updateCurrentUserInfo(userNickname, profileUrl, SendBird.UserInfoUpdateHandler { e ->
            if (e != null) {
                // Error!
                Toast.makeText(this@SettingsActivity, "" + e.code + ":" + e.message, Toast.LENGTH_SHORT).show()

                // Show update failed snackbar
                showSnackbar("Update user info failed")
                return@UserInfoUpdateHandler
            }

            PreferenceUtils.setNickname(this@SettingsActivity, userNickname)
        })
    }

    private fun showSnackbar(text: String) {
        val snackbar = Snackbar.make(mSettingsLayout!!, text, Snackbar.LENGTH_SHORT)
        snackbar.show()
    }

    companion object {
        private const val INTENT_REQUEST_CHOOSE_MEDIA = 0xf0
        private const val INTENT_REQUEST_CAMERA = 0xf1
        private const val PERMISSION_WRITE_EXTERNAL_STORAGE_UPLOAD = 0xf0
        private const val PERMISSION_WRITE_EXTERNAL_STORAGE_CAMERA = 0xf1
    }

}
