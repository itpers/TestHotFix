package com.beike.buildsrc

import javassist.ClassPool
import org.gradle.api.Project

public class FixUtils {
    static ClassPool classPool = ClassPool.default

    //匹配jar包的路径，如果包含集合中的路径，不注入代码，不生成MD5
    static List noProcessJarPath = ['com.android.support', 'com\\android\\support\\']
    static List noProcessClsPath = ['android\\support\\', '$', 'R.class', 'BuildConfig.class']
    static List noProcessClsName = ['com.beike.hotfixlib.AssetsUtil', 'com.beike.hotfixlib.HotPatch', 'com.beike.hotfixlib.ReflectUtil', 'com.beike.testhotfix.MyApplication']

    static String hashPath;
    static String mappingPath;
    static String sdkDir;

    static init(Project project){
        hashPath = project.projectDir.absolutePath + "\\hash.txt"
        mappingPath = project.projectDir.absolutePath + "\\mapping.txt"

        initJavassist(project)

    }

    /**
     * 初始化javassist（主要导入一些classpath）
     * @param project
     */
    private static void initJavassist(Project project){
        Properties prop = new Properties()
        File local = new File(project.rootDir, "local.properties")
        prop.load(local.newInputStream())
        sdkDir = prop.getProperty("sdk.dir")

        String version = project.android.compileSdkVersion
        String androidJar = sdkDir + "\\" + "platforms" + version + "\\android.jar"
        String apacheJar = sdkDir + "\\" + "platforms" + version + "\\optional\\org.apache.http.legacy.jar"
        if (new File(androidJar).exists()){
            classPool.appendClassPath(androidJar)
        }
        if (new File(apacheJar).exists()){
            classPool.appendClassPath(apacheJar)
        }

        def libPath = project.rootDir.absolutePath.concat("\\antilazy.jar")
        classPool.appendClassPath(libPath)
    }

    static void copyFile(File sourFile, File targetFile){
        targetFile.getParentFile().mkdirs()
        targetFile.newOutputStream() << sourFile.newInputStream();
    }
}