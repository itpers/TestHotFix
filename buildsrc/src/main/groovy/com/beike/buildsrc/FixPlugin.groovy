package com.beike.buildsrc

import com.android.build.gradle.internal.pipeline.TransformTask
import com.android.build.gradle.internal.transforms.ProGuardTransform
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task;

public class FixPlugin implements Plugin<Project>{

    boolean debugOn
    boolean generatePatch
    String patchDir
    String patchName
    boolean minify

    File storeFile
    String storePassword
    String keyAlias
    String keyPassword

    @Override
    void apply(Project project) {

        project.extensions.create("fixMode", FixExtension)
        project.extensions.create("fixSignConfig", SignExtension)

        project.afterEvaluate {
            FixUtils.init(project)

            //get Extension params
            def fixMode = project.extensions.findByName("fixMode")
            debugOn = fixMode.debugOn
            generatePatch = fixMode.generatePatch
            patchDir = fixMode.patchDir
            patchName = fixMode.patchName

            def signConfig =  project.extensions.findByName("fixSignConfig") as SignExtension
            storeFile = signConfig.storeFile
            storePassword = signConfig.storePassword
            keyAlias = signConfig.keyAlias
            keyPassword = signConfig.keyPassword

            def dexRelease = project.tasks.findByName("transformClassesWithDexForRelease")
            def dexDebug = project.tasks.findByName("transformClassesWithDexForDebug")
            def proguardRelease = project.tasks.findByName("transformClassesAndResourcesWithProguardForRelease")
            def proguardDebug = project.tasks.findByName("transformClassesAndResourcesWithProguardForDebug")

            if (proguardRelease){
                proguardReleaseClosure(proguardRelease)
            }
            if (proguardDebug){
                proguardDebugClosure(proguardDebug)
            }

        }

    }

    def proguardReleaseClosure = { Task proguardRelease ->
        proguardRelease.doFirst {
            minify = true;
        }

        //copy mapping.txt to app rootDir
        proguardRelease.doLast {
            File file = new File("$project.buildDir\\outputs\\mapping\\release\\mapping.txt")
            if (file.exists()) {
                FixUtils.copyFile(file, new File(project.projectDir, "mapping.txt"))
            }
        }
    }

    def proguardDebugClosure = { Task proguardDebug ->
        proguardDebug.doFirst {
            File mappingFile = new File(FixUtils.mappingPath)
            if (mappingFile.exists()){
                def transformTask = (TransformTask)proguardDebug
                def transform = (ProGuardTransform)transformTask.getTransform()
                transform.applyTestedMapping(mappingFile)
            }else {
                String tips = "mapping.txt not found, you can run 'Generate Signed Apk' with release and minify to generate a mapping, or setting generatePath false"
                throw new IllegalStateException(tips)
            }
            minify = true
        }
    }
}