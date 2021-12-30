package com.kevin.init.apt;

import java.io.IOException;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

import com.google.auto.service.AutoService;
import com.kevin.init.annotation.ModuleProvider;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

/**
 * Created by tuchuantao on 2021/12/23
 * Desc:
 */
// NOTE: Processor注册方式
// 1、通过第三方库自动注册：
//      1、implementation 'com.google.auto.service:auto-service:1.0-rc6'
//      2、annotationProcessor 'com.google.auto.service:auto-service:1.0-rc6'
//      3、@AutoService(Processor.class)
// 2、手动注册processor：
//      1、新建一个resources/目录，与java/目录同级，在里边建一个javax.annotation.processing.Processor文件
//      2、在该文件里写上MyProcessor的路径，com.kevintu.annotator.IocProcessor
//@AutoService(Processor.class)
public class ModuleInitProcessor extends AbstractProcessor {

  private static final String SUFFIX = "_ModuleProvider";
  private static final String INIT_PKG = "com.kevin.module.init";
  private static final String DEFAULT_JAVA_DOC = "Auto Generated Codes \n";

  private Set<Element> mAutoRegisterSet = new HashSet<>();
  // 生成代码辅助类
  private Filer mFiler;
  // 日志辅助类
  private Messager mMessager;
  // 跟元素相关的辅助类，帮助我们去获取一些元素相关的信息
  // - VariableElement  一般代表成员变量
  // - ExecutableElement  一般代表类中的方法
  // - TypeElement  一般代表类
  // - PackageElement  一般代表Package
  private Elements mElementUtils;

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    mFiler = processingEnv.getFiler();
    mMessager = processingEnv.getMessager();
    mElementUtils = processingEnv.getElementUtils();
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    Set<String> annotationTypes = new LinkedHashSet<>();
    annotationTypes.add(ModuleProvider.class.getCanonicalName());
    return annotationTypes;
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  public Set<String> getSupportedOptions() {
    return super.getSupportedOptions();
  }

  @Override
  public boolean process(Set<? extends TypeElement> set, RoundEnvironment env) {
    if (env.processingOver()) {
      generateInit();
    } else {
      processAnnotations(env);
    }
    return true;
  }

  private void processAnnotations(RoundEnvironment env) {
    Set<? extends Element> set = env.getElementsAnnotatedWith(ModuleProvider.class);
    if (set != null && !set.isEmpty()) {
      mAutoRegisterSet.addAll(set);
    }
  }

  private void generateInit() {
    if (mAutoRegisterSet.isEmpty()) {
      return;
    }
    for (Element element : mAutoRegisterSet) {
      if (element.getKind() != ElementKind.CLASS) { // 只处理修饰在具体类上的注解
        return;
      }
      ModuleProvider annotation = element.getAnnotation(ModuleProvider.class);

      MethodSpec getTypeMethod = MethodSpec.methodBuilder("getType")
          .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
          .returns(TypeName.INT)
          .addStatement("return $L", annotation.type())
          .build();
      MethodSpec getKeyMethod = MethodSpec.methodBuilder("getKey")
          .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
          .returns(ClassName.get("java.lang", "Class"))
          .addStatement("return " + getKeyClass(annotation).toString() + ".class")
          .build();
      ClassName className = ClassName.get(element.getEnclosingElement().toString(), element.getSimpleName().toString());
      MethodSpec getPluginMethod = MethodSpec.methodBuilder("getPlugin")
          .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
          .returns(className)
          .addStatement("return new $T()", className)
          .build();

      TypeSpec generateClass = TypeSpec.classBuilder(element.getSimpleName().toString() + annotation.type() + SUFFIX)
          .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
          .addMethod(getTypeMethod)
          .addMethod(getKeyMethod)
          .addMethod(getPluginMethod)
          .addJavadoc(DEFAULT_JAVA_DOC)
          .build();

      try {
        JavaFile.builder(INIT_PKG, generateClass)
            .build()
            .writeTo(mFiler);
      } catch (IOException e) {
        e.printStackTrace();
        mMessager.printMessage(Diagnostic.Kind.ERROR, "Write to Init java file error, classPath=" + element.toString());
      }
    }
  }

  private TypeMirror getKeyClass(ModuleProvider annotation) {
    try {
      annotation.interfaceClass();
    } catch (MirroredTypeException e) {
      return e.getTypeMirror();
    }
    return null;
  }
}
