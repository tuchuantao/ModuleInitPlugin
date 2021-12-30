package com.tu.myplugin

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import javassist.ClassPool
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException

/**
 * Created by tuchuantao on 2021/1/22
 */
class MyTransform : Transform() {

    override fun getName(): String {
        return "MyTransform"
    }

    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> {
        return TransformManager.CONTENT_CLASS
    }

    override fun isIncremental(): Boolean {
        return false
    }

    override fun getScopes(): MutableSet<in QualifiedContent.Scope> {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    override fun transform(invocation: TransformInvocation) {
        println(">>>>>>MyTransform transform()")
        for (input in invocation.inputs) {
            //遍历jar文件 对jar不操作，但是要输出到out路径
            input.jarInputs.parallelStream()
                .forEach { jarInput: JarInput ->
                    val src = jarInput.file
                    println(">>>>>>transform()  input.getJarInputs fielName:" + src.name)
                    val dst: File = invocation.outputProvider.getContentLocation(
                        jarInput.name, jarInput.contentTypes, jarInput.scopes,
                        Format.JAR
                    )
                    try {
                        FileUtils.copyFile(src, dst)
                    } catch (e: IOException) {
                        throw RuntimeException(e)
                    }
                }
            //遍历文件，在遍历过程中
            input.directoryInputs.parallelStream()
                .forEach { directoryInput: DirectoryInput ->
                    val src = directoryInput.file
                    println(">>>>>getDirectoryInputs()  fileName:" + src.name + "  directoryInput=" + directoryInput)
                    val dst: File = invocation.outputProvider.getContentLocation(
                        directoryInput.name, directoryInput.contentTypes,
                        directoryInput.scopes, Format.DIRECTORY
                    )
                    try {
                        scanFilesAndInsertCode(src.absolutePath)
                        println(">>>>>getDirectoryInputs() dst=$dst")
                        FileUtils.copyDirectory(src, dst)
                        //          FileUtils.deleteDirectory(src);
                    } catch (e: Exception) {
                        println(e.message)
                    }
                }
        }
    }

    @Throws(Exception::class)
    private fun scanFilesAndInsertCode(path: String) {
        println(">>>>>scanFilesAndInsertCode（） file path=$path")
        val classPool = ClassPool.getDefault()
        classPool.appendClassPath(path) //将当前路径加入类池,不然找不到这个类
        val ctClass = classPool.getCtClass("com.tu.transform.PluginTestClass") ?: return
        if (ctClass.isFrozen) {
            ctClass.defrost()
        }
        val ctMethod = ctClass.getDeclaredMethod("init")
        val insetStr = "System.out.println(\"我是插入的代码\");"
        ctMethod.insertAfter(insetStr) //在方法末尾插入代码
        ctClass.writeFile(path)
        ctClass.detach() //释放

        val ctClass2 = classPool.getCtClass("com.tu.transform.PluginTestClass2") ?: return
        if (ctClass2.isFrozen) {
            ctClass2.defrost()
        }
        val ctMethod2 = ctClass2.getDeclaredMethod("init")
        val insetStr2 = "System.out.println(\"我是插入的代码\");"
        ctMethod2.insertAfter(insetStr2) //在方法末尾插入代码
        ctClass2.writeFile(path)
        ctClass2.detach() //释放
    }
}