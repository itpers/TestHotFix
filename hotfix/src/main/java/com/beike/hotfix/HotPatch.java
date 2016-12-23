package com.beike.hotfix;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.IOException;

import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;

/**
 * Created by liupeng_a on 2016/11/24.
 */

public class HotPatch {
    private static final String TAG = "HotPatch";

    private static Context mContext;
    private static SignatureVerify signatureCheck;

    public static void init(Context context){
        signatureCheck = new SignatureVerify(context);
        mContext = context;

        File hackDir = context.getDir("hackDir", Context.MODE_PRIVATE);
        File hackDex = new File(hackDir, "hack_dex.jar");
        try{
            AssetsUtil.copyAssets(mContext, "hack_dex.jar", hackDex.getAbsolutePath());
            inject(hackDex.getAbsolutePath(), false);
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public static void inject(String patch, boolean doVerify){
        File file = new File(patch);
        if (!file.exists()){
            Log.e(TAG, "inject: " + file.getAbsolutePath() + " does not exists");
            return;
        }

        if (doVerify && !signatureCheck.verifyApk(file)){
            return;
        }

        if (hasLexClassLoader()){

        }else if (hasDexClassLoader()) {
            Log.i(TAG, "inject: have DexClassLoader");
            injectDexClassLoader(patch);
        }else{

        }
    }

    private static boolean hasLexClassLoader(){
        try {
            Class.forName("dalvik.system.LexClassLoader");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static boolean hasDexClassLoader(){
        try {
            Class.forName("dalvik.system.BaseDexClassLoader");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static void injectDexClassLoader(String patch){

        try{
            //获取classes.jar的dexElements
            Class<?> cl = Class.forName("dalvik.system.BaseDexClassLoader");
            PathClassLoader patchClassLoader = (PathClassLoader) mContext.getClassLoader();
            Object basePathList = ReflectUtil.getField(cl, "pathList", patchClassLoader);
            Object baseDexElement = ReflectUtil.getField(basePathList.getClass(), "dexElements", basePathList);

            String dexOpt = mContext.getDir("dexopt", Context.MODE_PRIVATE).getAbsolutePath();
            DexClassLoader dexClassLoader = new DexClassLoader(patch, dexOpt, patch, mContext.getClassLoader());
            Object patchPathList = ReflectUtil.getField(cl, "pathList", dexClassLoader);
            Object patchDexElements = ReflectUtil.getField(patchPathList.getClass(), "dexElements", patchPathList);

            //合并两个dexElements
            Object combineElements = ReflectUtil.combineArray(patchDexElements, baseDexElement);

            //将合并后的dexElement重新赋值给app的classloader
            ReflectUtil.setField(basePathList.getClass(), "dexElements", basePathList, combineElements);

        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
