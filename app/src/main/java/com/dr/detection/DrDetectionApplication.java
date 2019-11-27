package com.dr.detection;

import android.app.Application;
import android.content.Context;

public class DrDetectionApplication extends Application {

    private static Context mContext;
    private static DrDetectionApplication mInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        mContext = getApplicationContext();
    }

    public static synchronized DrDetectionApplication getInstance() {
        return mInstance;
    }

    public static Context getContext() {
        return mContext;
    }
}

