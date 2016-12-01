package com.beike.testhotfix.model;

/**
 * Created by liupeng_a on 2016/11/24.
 */

public class People implements TestInterface{

    @Override
    public String say() {
        return "错误已经修复，请放心使用！！！！";
    }
}
