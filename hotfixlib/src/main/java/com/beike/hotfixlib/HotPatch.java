package com.beike.hotfixlib;

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
    private static final String PATH_LIST = "pathList";
    private static final String DEX_ELEMENTS = "dexElements";
    private static final String DEX_OPT = "dexopt";

    private static Context mContext;

    public static void init(Context context){
        mContext = context;

        File hackDir = context.getDir("hackDir", Context.MODE_PRIVATE);
        File hackDex = new File(hackDir, "hack_dex.jar");
        try{
            AssetsUtil.copyAssets(mContext, "hack_dex.jar", hackDex.getAbsolutePath());
            inject(hackDex.getAbsolutePath());
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public static void inject(String patch){
        File file = new File(patch);
        if (file.exists()){
            try{
                //获取classes.jar的dexElements
                Class<?> cl = Class.forName("dalvik.system.BaseDexClassLoader");
                PathClassLoader patchClassLoader = (PathClassLoader) mContext.getClassLoader();
                Object basePathList = ReflectUtil.getField(cl, PATH_LIST, patchClassLoader);
                Object baseDexElement = ReflectUtil.getField(basePathList.getClass(), DEX_ELEMENTS, basePathList);

                //获取patch_dex.jar的dexElements (需要把patch_dex.jar先加载进去)
                String dexOpt = mContext.getDir(DEX_OPT, Context.MODE_PRIVATE).getAbsolutePath();
                DexClassLoader dexClassLoader = new DexClassLoader(patch, dexOpt, dexOpt, mContext.getClassLoader());
                Object patchPathList = ReflectUtil.getField(cl, PATH_LIST, dexClassLoader);
                Object patchDexElements = ReflectUtil.getField(patchPathList.getClass(), DEX_ELEMENTS, patchPathList);

                //合并两个dexElements
                Object combineElements = ReflectUtil.combineArray(patchDexElements, baseDexElement);

                //将合并后的dexElement重新赋值给app的classloader
                ReflectUtil.setField(basePathList.getClass(), DEX_ELEMENTS, basePathList, combineElements);
            }catch (Exception e){
                e.printStackTrace();
            }
        }else {
            Log.e(TAG, "inject: " + file.getAbsolutePath() + " does not exists");
        }
    }
}
