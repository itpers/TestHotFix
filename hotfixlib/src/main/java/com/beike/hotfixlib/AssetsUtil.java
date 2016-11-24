package com.beike.hotfixlib;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by liupeng_a on 2016/11/24.
 */

public class AssetsUtil {

    public static void copyAssets(Context context, String assetsName, String destFilePath) throws IOException{
        File file = new File(destFilePath);
        FileOutputStream outputStream = new FileOutputStream(file);

        InputStream inputStream = context.getAssets().open(assetsName);

        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1){
            outputStream.write(buffer, 0, length);
        }
        inputStream.close();
        outputStream.close();
    }
}
