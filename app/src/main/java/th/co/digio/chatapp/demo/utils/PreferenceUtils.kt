package th.co.digio.chatapp.demo.utils

import android.content.Context
import android.content.SharedPreferences

object PreferenceUtils {

    private const val PREFERENCE_KEY_USER_ID = "userId"
    private const val PREFERENCE_KEY_NICKNAME = "nickname"
    private const val PREFERENCE_KEY_PROFILE_URL = "profileUrl"
    private const val PREFERENCE_KEY_CONNECTED = "connected"

    private const val PREFERENCE_KEY_NOTIFICATIONS = "notifications"
    private const val PREFERENCE_KEY_NOTIFICATIONS_SHOW_PREVIEWS = "notificationsShowPreviews"
    private const val PREFERENCE_KEY_NOTIFICATIONS_DO_NOT_DISTURB = "notificationsDoNotDisturb"
    private const val PREFERENCE_KEY_NOTIFICATIONS_DO_NOT_DISTURB_FROM = "notificationsDoNotDisturbFrom"
    private const val PREFERENCE_KEY_NOTIFICATIONS_DO_NOT_DISTURB_TO = "notificationsDoNotDisturbTo"
    private const val PREFERENCE_KEY_GROUP_CHANNEL_DISTINCT = "channelDistinct"

    fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences("sendbird", Context.MODE_PRIVATE)
    }

    fun setUserId(context: Context, userId: String) {
        val editor = getSharedPreferences(context).edit()
        editor.putString(PREFERENCE_KEY_USER_ID, userId).apply()
    }

    fun getUserId(context: Context): String? {
        return getSharedPreferences(context).getString(PREFERENCE_KEY_USER_ID, "")
    }

    fun setNickname(context: Context, nickname: String) {
        val editor = getSharedPreferences(context).edit()
        editor.putString(PREFERENCE_KEY_NICKNAME, nickname).apply()
    }

    fun getNickname(context: Context): String? {
        return getSharedPreferences(context).getString(PREFERENCE_KEY_NICKNAME, "")
    }

    fun setProfileUrl(context: Context, profileUrl: String) {
        val editor = getSharedPreferences(context).edit()
        editor.putString(PREFERENCE_KEY_PROFILE_URL, profileUrl).apply()
    }

    fun getProfileUrl(context: Context): String? {
        return getSharedPreferences(context).getString(PREFERENCE_KEY_PROFILE_URL, "")
    }

    fun setConnected(context: Context, tf: Boolean) {
        val editor = getSharedPreferences(context).edit()
        editor.putBoolean(PREFERENCE_KEY_CONNECTED, tf).apply()
    }

    fun getConnected(context: Context): Boolean {
        return getSharedPreferences(context).getBoolean(PREFERENCE_KEY_CONNECTED, false)
    }

    fun clearAll(context: Context) {
        val editor = getSharedPreferences(context).edit()
        editor.clear().apply()
    }

    fun setNotifications(context: Context, notifications: Boolean) {
        val editor = getSharedPreferences(context).edit()
        editor.putBoolean(PREFERENCE_KEY_NOTIFICATIONS, notifications).apply()
    }

    fun getNotifications(context: Context): Boolean {
        return getSharedPreferences(context).getBoolean(PREFERENCE_KEY_NOTIFICATIONS, true)
    }

    fun setNotificationsShowPreviews(context: Context, notificationsShowPreviews: Boolean) {
        val editor = getSharedPreferences(context).edit()
        editor.putBoolean(PREFERENCE_KEY_NOTIFICATIONS_SHOW_PREVIEWS, notificationsShowPreviews).apply()
    }

    fun getNotificationsShowPreviews(context: Context): Boolean {
        return getSharedPreferences(context).getBoolean(PREFERENCE_KEY_NOTIFICATIONS_SHOW_PREVIEWS, true)
    }

    fun setNotificationsDoNotDisturb(context: Context, notificationsDoNotDisturb: Boolean) {
        val editor = getSharedPreferences(context).edit()
        editor.putBoolean(PREFERENCE_KEY_NOTIFICATIONS_DO_NOT_DISTURB, notificationsDoNotDisturb).apply()
    }

    fun getNotificationsDoNotDisturb(context: Context): Boolean {
        return getSharedPreferences(context).getBoolean(PREFERENCE_KEY_NOTIFICATIONS_DO_NOT_DISTURB, false)
    }

    fun setNotificationsDoNotDisturbFrom(context: Context, notificationsDoNotDisturbFrom: String) {
        val editor = getSharedPreferences(context).edit()
        editor.putString(PREFERENCE_KEY_NOTIFICATIONS_DO_NOT_DISTURB_FROM, notificationsDoNotDisturbFrom).apply()
    }

    fun getNotificationsDoNotDisturbFrom(context: Context): String? {
        return getSharedPreferences(context).getString(PREFERENCE_KEY_NOTIFICATIONS_DO_NOT_DISTURB_FROM, "")
    }

    fun setNotificationsDoNotDisturbTo(context: Context, notificationsDoNotDisturbTo: String) {
        val editor = getSharedPreferences(context).edit()
        editor.putString(PREFERENCE_KEY_NOTIFICATIONS_DO_NOT_DISTURB_TO, notificationsDoNotDisturbTo).apply()
    }

    fun getNotificationsDoNotDisturbTo(context: Context): String? {
        return getSharedPreferences(context).getString(PREFERENCE_KEY_NOTIFICATIONS_DO_NOT_DISTURB_TO, "")
    }

    fun setGroupChannelDistinct(context: Context, channelDistinct: Boolean) {
        val editor = getSharedPreferences(context).edit()
        editor.putBoolean(PREFERENCE_KEY_GROUP_CHANNEL_DISTINCT, channelDistinct).apply()
    }

    fun getGroupChannelDistinct(context: Context): Boolean {
        return getSharedPreferences(context).getBoolean(PREFERENCE_KEY_GROUP_CHANNEL_DISTINCT, true)
    }
}// Prevent instantiation
