package com.tu.myplugin

import com.android.build.gradle.BaseExtension

import org.gradle.api.Plugin
import org.gradle.api.Project


/**
 * Created by tuchuantao on 2021/1/22
 */
class MyPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        println(">>>>registerTransform()<<<<")
        project.extensions.findByType(BaseExtension::class.java)
            ?.registerTransform(MyTransform())
    }
}