package com.beike.testhotfix;

import android.app.Application;
import android.os.Environment;

import com.beike.hotfixlib.HotPatch;

/**
 * Created by liupeng_a on 2016/11/24.
 */

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        HotPatch.init(this);

        String dexPath = Environment.getExternalStorageDirectory().getAbsolutePath().concat("/patch_dex.jar");
        HotPatch.inject(dexPath);

    }
}
