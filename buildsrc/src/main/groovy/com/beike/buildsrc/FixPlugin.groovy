package com.beike.buildsrc

import com.android.build.gradle.internal.pipeline.TransformTask
import com.android.build.gradle.internal.transforms.ProGuardTransform
import groovy.io.FileType
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

public class FixPlugin implements Plugin<Project> {

    String patchDir
    String patchName
    boolean minify

    File storeFile
    String storePassword
    String keyAlias
    String keyPassword

    @Override
    void apply(Project project) {

        project.afterEvaluate {
            FixUtils.init(project)

            patchDir = project.rootDir.absolutePath + File.separator + "patch_dex"
            patchName = "patch_dex.jar"

            def signConfig = project.android.signingConfigs.release
            storeFile = signConfig.storeFile
            storePassword = signConfig.storePassword
            keyAlias = signConfig.keyAlias
            keyPassword = signConfig.keyPassword

            def dexRelease = project.tasks.findByName("transformClassesWithDexForRelease")
            def dexHotfix = project.tasks.findByName("transformClassesWithDexForHotfix")
            def proguardRelease = project.tasks.findByName("transformClassesAndResourcesWithProguardForRelease")
            def proguardHotfix = project.tasks.findByName("transformClassesAndResourcesWithProguardForHotfix")

            if (proguardRelease) {
                proguardReleaseClosure(proguardRelease)
            }
            if (proguardHotfix) {
                proguardHotfixClosure(proguardHotfix)
            }

            if (dexRelease) {
                dexReleaseClosure(dexRelease)
            }

            if (dexHotfix) {
                dexHotfixClosure(dexHotfix)
            }
        }

    }

    def proguardReleaseClosure = { Task proguardRelease ->
        proguardRelease.doFirst {
            minify = true;
        }

        proguardRelease.doLast {
            File file = new File("$project.buildDir" + File.separator + "outputs" + File.separator + "mapping" + File.separator + "release" + File.separator + "mapping.txt")
            if (file.exists()) {
                FixUtils.copyFile(file, new File(project.projectDir, "mapping.txt"))
            }
        }
    }

    def proguardHotfixClosure = { Task proguardDebug ->
        proguardDebug.doFirst {
            File mappingFile = new File(FixUtils.mappingPath)
            if (mappingFile.exists()) {
                def transformTask = (TransformTask) proguardDebug
                def transform = (ProGuardTransform) transformTask.getTransform()
                transform.applyTestedMapping(mappingFile)
            } else {
                String tips = "没有mapping.txt, 请先运行assembleRelease生成mapping.txt文件"
                throw new IllegalStateException(tips)
            }
            minify = true
        }
    }

    def dexHotfixClosure = { Task dexDebug ->
        dexDebug.outputs.upToDateWhen { false }

        dexDebug.doFirst {
            Map<String, String> md5Map

            File patchFile = new File(patchDir)
            if (patchFile.exists()) {
                FixUtils.cleanDirectory(patchFile)
            }

            File hashFile = new File(FixUtils.hashPath)
            if (hashFile.exists()) {
                md5Map = FixUtils.resolveHashFile(hashFile)
            } else {
                String tips = "没有hash.txt, 请先运行assembleRelease生成hash.txt文件"
                throw new IllegalStateException(tips)
            }


            if (minify) {
                dexDebug.inputs.files.files.each { File file ->
                    file.eachFileRecurse(FileType.FILES, { File f ->
                        if (f.absolutePath.endsWith('.jar')) {
                            FixUtils.processJar(f, md5Map, patchDir, true)
                        }
                    })
                }
            } else {
                dexDebug.inputs.files.files.each { File file ->
                    if (file.name.endsWith('.jar') && FixUtils.shouldProcessJar(file.absolutePath)) {
                        FixUtils.processJar(file, md5Map, patchDir, false)
                    } else if (file.isDirectory()) {
                        FixUtils.processDir(file, md5Map, patchDir, false)
                    }
                }
            }

            FixUtils.dx(project, patchDir, patchName)
            FixUtils.signApk(new File(patchDir, patchName), storeFile, keyPassword, storePassword, keyAlias)
        }
    }

    def dexReleaseClosure = { Task dexRelease ->
        // not up-to-date
        // http://stackoverflow.com/questions/7289874/resetting-the-up-to-date-property-of-gradle-tasks
        dexRelease.outputs.upToDateWhen { false }

        dexRelease.doFirst {
            File hashFile = FixUtils.createHashFile()
            def writer = hashFile.newPrintWriter()

            // 打开混淆，编译后文件路径 "build/intermediates/transforms/proguard/……"
            if (minify) {
                dexRelease.inputs.files.files.each { File file ->
                    file.eachFileRecurse(FileType.FILES, { File f ->
                        if (f.absolutePath.endsWith('.jar')) {
                            FixUtils.processJar(f, writer, true)
                        }
                    })
                }
            } else {
                // 未开混淆编译后文件路径 "build/intermediates/classes/……"
                dexRelease.inputs.files.files.each { File file ->
                    if (file.name.endsWith('.jar') && FixUtils.shouldProcessJar(file.absolutePath)) {
                        FixUtils.processJar(file, writer, false)
                    } else if (file.isDirectory()) {
                        FixUtils.processDir(file, writer)
                    }
                }
            }
            writer.close()
        }

    }
}