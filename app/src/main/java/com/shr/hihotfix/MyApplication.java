package com.shr.hihotfix;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.shr.fix.FixManager;

public class MyApplication extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        FixManager fixManager = new FixManager(base);
        if (fixManager.checkNeedFix()) {
            fixManager.loadDex();
        }
        super.attachBaseContext(base);
    }

    @Override
    public void onCreate() {
        super.onCreate();

    }
}
