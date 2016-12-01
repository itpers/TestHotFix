package com.beike.buildsrc

import javassist.ClassPool
import javassist.CtClass
import javassist.CtConstructor
import org.apache.tools.ant.util.JavaEnvUtils
import org.gradle.api.GradleException
import org.gradle.api.Project

import java.security.MessageDigest
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

public class FixUtils {
    static ClassPool classPool = ClassPool.default

    //匹配jar包的路径，如果包含集合中的路径，不注入代码，不生成MD5
    static List noProcessJarPath = ['com.android.support', 'com' + File.separator +'android' + File.separator +'support' + File.separator]
    static List noProcessClsPath = ['android' + File.separator +'support' + File.separator, '$', 'R.class', 'BuildConfig.class']
    static List noProcessClsName = ['com.beike.hotfix.AssetsUtil', 'com.beike.hotfix.HotPatch', 'com.beike.hotfix.ReflectUtil', 'com.beike.hotfix.SignatureVerify', 'com.beike.testhotfix.MyApplication']

    static String hashPath;
    static String mappingPath;
    static String sdkDir;

    static init(Project project){
        hashPath = project.projectDir.absolutePath + File.separator + "hash.txt"
        mappingPath = project.projectDir.absolutePath + File.separator + "mapping.txt"

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
        String androidJar = sdkDir + File.separator + "platforms" + File.separator + version + File.separator + "android.jar"
        String apacheJar = sdkDir + File.separator + "platforms" + File.separator + version + File.separator + "optional" + File.separator + "org.apache.http.legacy.jar"
        if (new File(androidJar).exists()){
            classPool.appendClassPath(androidJar)
        }
        if (new File(apacheJar).exists()){
            classPool.appendClassPath(apacheJar)
        }

        def libPath = project.rootDir.absolutePath.concat(File.separator + "hack.jar")
        classPool.appendClassPath(libPath)
    }

    /**
     * 创建hash.txt文件
     * @return
     */
    static File createHashFile() {
        File file = new File(hashPath)
        if (file.exists()) {
            file.delete()
        }
        file.createNewFile()
        return file
    }

    /**
     * 判断jar包是否需要处理（注入字节码或者生成md5)
     * @param filePath
     * @return
     */
    static boolean shouldProcessJar(String filePath){
        for (String value : noProcessJarPath){
            if (filePath.contains(value)) {
                return false
            }
        }
        return true
    }

    /**
     * 判断class是否需要处理
     * @param filePath
     * @param minify
     * @return
     */
    static boolean shouldProcessClass(String filePath, boolean minify){
        File mappingFile = new File(mappingPath)
        Map<String, String> map
        if (mappingFile.exists() && map == null){
            map = resolveMappingFile(mappingFile)
        }

        for (String value : noProcessClsPath){
            if (filePath.contains(value)){
                return false
            }
        }

        for (String value : noProcessClsName){
            if (minify && map != null && map.size() > 0){
                value = map.get(value, value)
            }
            value = value.replace('.', File.separator)
            println 'xxxxxxxxxx==  filePath = ' + filePath + "   value = " + value
            if (filePath.contains(value)){
                println 'xxxxxxxxxx---->  filePath = ' + filePath + '   value = ' + value
                return false
            }
        }
        return true
    }

    /**
     * 加压jar包
     * @param jar
     * @param dest
     */
    static void unZipJar(File jar, String dest){
        JarFile jarFile = new JarFile(jar)
        Enumeration<JarEntry> jarEntryEnumeration = jarFile.entries();
        while (jarEntryEnumeration.hasMoreElements()){
            JarEntry jarEntry = jarEntryEnumeration.nextElement()
            if (jarEntry.directory){
                continue
            }
            String entryName = jarEntry.getName()
            String outFileName = dest + "/" + entryName
            File outFile = new File(outFileName)
            outFile.getParentFile().mkdirs()
            InputStream inputStream = jarFile.getInputStream(jarEntry)
            FileOutputStream fileOutputStream = new FileOutputStream(outFile)
            fileOutputStream << inputStream
            fileOutputStream.close()
            inputStream.close()
        }
        jarFile.close()
    }

    /**
     * 压缩jar包
     * @param jarDir
     * @param dest
     */
    static void zipJar(File jarDir, String dest){
        JarOutputStream outputStream = new JarOutputStream(new FileOutputStream(dest));
        jarDir.eachFileRecurse {File file ->
            if (!file.isDirectory()){
                String entryName = file.getAbsolutePath().substring(jarDir.absolutePath.length() + 1)
                outputStream.putNextEntry(new ZipEntry(entryName))
                InputStream inputStream = new FileInputStream(file)
                outputStream << inputStream
                inputStream.close()
            }
        }
        outputStream.close()
    }

    /**
     * 将路径转换为完整类名
     * @param parent
     * @param c
     * @return
     */
    static String getClassName(File parent, File c){
        def cPath = c.absolutePath
        def pPath = parent.absolutePath
        return cPath.substring(pPath.length() + 1, cPath.length() - 6).replace(File.separator, '.').replace('/', '.')
    }

    /**
     * 获取文件md5值
     * @param file
     * @return
     */
    static String md5(File file){
        MessageDigest digest = MessageDigest.getInstance("MD5")
        def inputStream = file.newInputStream()
        byte[] buf = new byte[16384]
        int len
        while ((len = inputStream.read(buf)) != -1) {
            digest.update(buf, 0, len)
        }
        inputStream.close()

        char[] chars = ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f']
        byte[] bytes = digest.digest()
        int l = bytes.length;
        char[] str = new char[l << 1];
        int i = 0;
        for (int j = 0; i < l; ++i) {
            str[j++] = chars[(240 & bytes[i]) >>> 4];
            str[j++] = chars[15 & bytes[i]];
        }
        return new String(str)
    }

    static void copyFile(File sourFile, File targetFile){
        targetFile.getParentFile().mkdirs()
        targetFile.newOutputStream() << sourFile.newInputStream();
    }

    /**
     * 打包成dexjar 生成补丁包
     * @param project
     * @param patchDir
     * @param patchName
     */
    static void dx(Project project, String patchDir, String patchName){
        File file = new File(patchDir)
        if (file.isDirectory() && file.exists()){
            File[] files = file.listFiles()
            if (files != null && files.size() > 0){
                String buildTool = project.android.buildToolsVersion
                def stdout = new ByteArrayOutputStream()
                project.exec {
                    workingDir "$sdkDir" + File.separator + "build-tools" + File.separator + "$buildTool"
                    commandLine 'dx.bat', '--dex', '--output', "$patchDir" + File.separator + "$patchName", patchDir
                    standardOutput = stdout
                }
                def error = stdout.toString().trim()
                if (error){
                    println "dex error:" + error
                }
            }
        }
    }

    static void signApk(File patchFile, File storeFile, String keyPassword, String storePassword, String keyAlias){
        if(!patchFile.exists() || !storeFile.exists() || keyPassword == null || storePassword == null || keyAlias==null) {
            return
        }

        def args = [JavaEnvUtils.getJdkExecutable('jarsigner'),
                    '-verbose',
                    '-sigalg', 'MD5withRSA',
                    '-digestalg', 'SHA1',
                    '-keystore', storeFile.absolutePath,
                    '-keypass', keyPassword,
                    '-storepass', storePassword,
                    patchFile.absolutePath,
                    keyAlias]
        println "Signing with command:"
        for (String s : args)
            print s + " "
        println ""
        def proc = args.execute()
        def outRedir = new StreamRedir(proc.inputStream, System.out)
        def errRedir = new StreamRedir(proc.errorStream, System.out)

        outRedir.start()
        errRedir.start()

        def result = proc.waitFor()
        outRedir.join()
        errRedir.join()

        if (result != 0) {
            throw new GradleException('Couldn\'t sign')
        }
    }

    static class StreamRedir extends Thread {
        private inStream
        private outStream

        public StreamRedir(inStream, outStream) {
            this.inStream = inStream
            this.outStream = outStream
        }

        public void run() {
            int b;
            while ((b = inStream.read()) != -1)
                outStream.write(b)
        }
    }

    /**
     * 注入代码到class
     * @param dir
     * @param className
     */
    static void processClass(String dir, String className){
        CtClass c = classPool.getCtClass(className)
        if (c.isFrozen()){
            c.defrost()
        }

        CtConstructor[] ctConstructors = c.getDeclaredConstructors()
        if (ctConstructors == null || ctConstructors.length == 0){
            CtConstructor constructor = new CtConstructor(new CtClass[0], c)
            constructor.setBody("{\nSystem.out.println(com.beike.hack.AntilazyLoad.class);\n}")
            c.addConstructor(constructor)
        }else {
            ctConstructors[0].insertBeforeBody('System.out.println(com.beike.hack.AntilazyLoad.class);')
        }
        c.writeFile(dir)
        c.detach()
    }

    /**
     * 删除此文件下的所有内容，但这个文件夹不会删除
     * @param directory
     */
    static void cleanDirectory(File directory){
        if (!directory.exists()){
            throw new IllegalArgumentException("$directory does not exist")
        } else if (!directory.isDirectory()){
            throw new IllegalArgumentException("$directory is not a directory")
        } else {
            File[] files = directory.listFiles()
            for (File file : files){
                if (file.isDirectory()){
                    cleanDirectory(file)
                }
                file.delete()
            }
        }
    }

    static Map<String, String> resolveMappingFile(File mappingFile){
        Map<String, String> map = new HashMap<>()
        def reader = mappingFile.newReader()
        reader.eachLine { line ->
            if (line.endsWith(':')){
                line = line.replace(':', '')
                String[] strs = line.split(' -> ')
                map.put(strs[0], strs[1])
            }
        }
        reader.close()
        return map
    }

    /**
     * 将hash.txt解析成map
     * @param hashFile
     * @return
     */
    static Map<String, String> resolveHashFile(File hashFile){
        Map<String, String> map = new HashMap<>()
        def reader = hashFile.newReader()
        reader.eachLine { line ->
            String[] strs = line.split('-')
            map.put(strs[0], strs[1])
        }
        reader.close()
        return map;
    }

    static void processJar(File file, Writer writer, boolean minify){
        processJar(file, writer, true, false, null, null, minify)
    }

    static void processJar(File file, boolean inject, boolean generatePath, Map<String, String> md5Map, String patchDir, boolean minify){
        processJar(file, null, inject, generatePath, md5Map, patchDir, minify)
    }

    static void processJar(File file, Writer writer, boolean inject, boolean generatePath, Map<String, String> md5Map, String patchDir, boolean minify){
        //解压jar
        File jarDir = new File(file.parent, file.getName().replace('.jar', ''))
        unZipJar(file, jarDir.absolutePath)
        if (inject){
            classPool.appendClassPath(jarDir.absolutePath)
        }
        jarDir.eachFileRecurse { f ->
            if (f.getName().endsWith('.class') && shouldProcessClass(f.absolutePath, minify)){
                String className = getClassName(jarDir, f)
                String md5 = md5(f)

                // writer != null 写入hash.txt
                if (writer != null){
                    writer.println(className + '-' + md5)
                }else if (generatePath) {
                    String value = md5Map.get(className);
                    if (!md5.equals(value)){
                        String pkg = className.substring(0, className.lastIndexOf('.'))
                        String dest = "$patchDir" + File.separator + "${pkg.replace('.', File.separator)}" + File.separator + "$f.name"
                        copyFile(f, new File(dest))
                    }
                }

                if (inject){
                    processClass(jarDir.absolutePath, className)
                }
            }
        }

        if (inject){
            file.delete()
            zipJar(jarDir, file.getAbsolutePath())
        }
        cleanDirectory(jarDir)
        jarDir.delete()
    }

    static void processDir(File file, Writer writer) {
        processDir(file, writer, true, false, null, null, false)
    }

    static void processDir(File file, boolean inject, boolean generatePatch, Map<String, String> md5Map, String patchDir, boolean minify) {
        processDir(file, null, inject, generatePatch, md5Map, patchDir, minify)
    }

    static void processDir(File file, Writer writer, boolean inject, boolean generatePatch, Map<String, String> md5Map, String patchDir, boolean minify) {
        if (inject) {
            classPool.appendClassPath(file.absolutePath)
        }
        file.eachFileRecurse { File f ->
            if (f.name.endsWith('.class') && shouldProcessClass(f.absolutePath, minify)) {
                String className = getClassName(file, f)
                String md5 = md5(f)
                if (writer != null) {
                    writer.println(className + "-" + md5)
                } else if (generatePatch) {
                    String value = md5Map.get(className)
                    if (!md5.equals(value)) {
                        String pkg = className.substring(0, className.lastIndexOf('.'))
                        String dest = "$patchDir" + File.separator + "${pkg.replace('.', File.separator)}" + File.separator + "$f.name"
                        copyFile(f, new File(dest))
                    }
                }

                if (inject) {
                    processClass(file.absolutePath, className)
                }
            }
        }
    }
}