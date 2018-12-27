package th.co.digio.chatapp.demo.main


import android.app.Application

import com.sendbird.android.SendBird

open class BaseApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        SendBird.init(APP_ID, applicationContext)
    }

    companion object {
        //    private static final String APP_ID = "9DA1B1F4-0BE6-4DA8-82C5-2E81DAB56F23"; // US-1 Demo
        private const val APP_ID = "BFE5F605-27A5-4F31-8C4C-102DE50D9A25"
        const val VERSION = "3.0.38"
    }
}
