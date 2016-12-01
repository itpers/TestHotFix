package com.beike.hotfix;

import android.util.Log;

import java.lang.reflect.Array;
import java.lang.reflect.Field;

/**
 * Created by liupeng_a on 2016/11/24.
 */

public class ReflectUtil {

    private static final String TAG = "ReflectUtil";

    /**
     * 通过反射获取对象的属性值
     * @param cl 目标class
     * @param fieldName 属性名
     * @param object 对象
     * @throws NoSuchFieldException
     */
    public static Object getField(Class<?> cl, String fieldName, Object object) throws NoSuchFieldException, IllegalAccessException {
        Field field = cl.getDeclaredField(fieldName);
        if (!field.isAccessible()){
            field.setAccessible(true);
        }
        return field.get(object);
    }

    public static void setField(Class<?> cl, String fieldName, Object object, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field field = cl.getDeclaredField(fieldName);
        if (!field.isAccessible()){
            field.setAccessible(true);
        }
        field.set(object, value);
    }

    public static Object combineArray(Object array1, Object array2){
        int array1Length = Array.getLength(array1);
        int array2Length = Array.getLength(array2);
        int length = array1Length + array2Length;
        Log.i(TAG, "combineArray: length = " + length);

        Class<?> componentType = array1.getClass().getComponentType();
        Object newArray = Array.newInstance(componentType, length);
        for (int i=0; i<length; i++) {
            if (i < array1Length){
                Array.set(newArray, i, Array.get(array1, i));
            }else {
                Array.set(newArray, i, Array.get(array2, i - array1Length));
            }
        }

        return newArray;
    }
}
