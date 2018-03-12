package com.sendbird.android.sample.main;


import android.app.Application;

import com.sendbird.android.SendBird;

public class BaseApplication extends Application {

//    private static final String APP_ID = "9DA1B1F4-0BE6-4DA8-82C5-2E81DAB56F23"; // US-1 Demo
    private static final String APP_ID = "BFE5F605-27A5-4F31-8C4C-102DE50D9A25";
    public static final String VERSION = "3.0.38";

    @Override
    public void onCreate() {
        super.onCreate();
        SendBird.init(APP_ID, getApplicationContext());
    }
}
