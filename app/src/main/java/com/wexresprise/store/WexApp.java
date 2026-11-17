package com.wexresprise.store;

import android.app.Application;

public class WexApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        if (!CrashLog.isCrashProcess(this)) {
            CrashLog.install(this);
        }
    }
}
