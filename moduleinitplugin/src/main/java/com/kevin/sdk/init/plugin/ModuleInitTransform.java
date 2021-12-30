package com.kevin.sdk.init.plugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;

import com.android.SdkConstants;
import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.Format;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformException;
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.gradle.internal.pipeline.TransformManager;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import org.gradle.api.Project;

/**
 * Created by tuchuantao on 2021/12/23
 * Desc:
 */
public class ModuleInitTransform extends Transform {

  private static final String TAG = "ModuleInitPlugin";
  private static final String SUFFIX = "_ModuleProvider";
  private static final String PKG = "com.kevin.module.init";
  private static final String PLUGIN_MANAGER_CLASS = "com.kevin.module.init.PluginManager";
  private static final String APT_CREATE_PATH = PKG.replace('.', File.separatorChar);

  private File mManagerSrc;
  private File mManagerDst;
  private TransformInvocation mInvocation;
  private final ModuleInitExtension mModuleInitExtension;

  public ModuleInitTransform(final Project project) {
    mModuleInitExtension = project.getExtensions().findByType(ModuleInitExtension.class);
  }


  @Override
  public String getName() {
    return TAG;
  }

  @Override
  public Set<QualifiedContent.ContentType> getInputTypes() {
    return TransformManager.CONTENT_CLASS;
  }

  @Override
  public Set<? super QualifiedContent.Scope> getScopes() {
    if (mModuleInitExtension.isApp) {
      return TransformManager.SCOPE_FULL_PROJECT;
    }
    return TransformManager.PROJECT_ONLY;
  }

  @Override
  public boolean isIncremental() {
    return false;
  }

  @Override
  public void transform(final TransformInvocation invocation)
      throws TransformException, InterruptedException, IOException {
    super.transform(invocation);
    System.out.println(TAG + ": start... is App: "+ mModuleInitExtension.isApp);
    long startTime = System.currentTimeMillis();

    mInvocation = invocation;
    final Set<String> classSet = Collections.newSetFromMap(new ConcurrentHashMap());
    final ClassPool classPool = ClassPool.getDefault();
    mManagerSrc = null;
    mManagerDst = null;

    for (TransformInput input : invocation.getInputs()) {
      input.getJarInputs().parallelStream().forEach(new Consumer<JarInput>() {
        @Override
        public void accept(JarInput jarInput) {
          File src = jarInput.getFile();
          File dst = invocation.getOutputProvider()
              .getContentLocation(jarInput.getName(), jarInput.getContentTypes(),
                  jarInput.getScopes(), Format.JAR);
          try {
            if (scanJarFile(src, classSet)) {
              mManagerSrc = src;
              mManagerDst = dst;
            } else {
              FileUtils.copyFile(src, dst);
            }
            classPool.appendClassPath(src.getAbsolutePath());
          } catch (IOException | NotFoundException e) {
            throw new RuntimeException(e);
          }
        }
      });

      input.getDirectoryInputs().parallelStream().forEach(new Consumer<DirectoryInput>() {
        @Override
        public void accept(DirectoryInput directoryInput) {
          File src = directoryInput.getFile();
          File dst = invocation.getOutputProvider()
              .getContentLocation(directoryInput.getName(), directoryInput.getContentTypes(),
                  directoryInput.getScopes(), Format.DIRECTORY);
          try {
            if (scanDir(src, classSet)) {
              mManagerSrc = src;
              mManagerDst = dst;
            } else {
              FileUtils.copyDirectory(src, dst);
            }
            classPool.appendClassPath(src.getAbsolutePath());
          } catch (IOException | NotFoundException e) {
            throw new RuntimeException(e);
          }
        }
      });
    }

    if (mManagerSrc != null) {
      if (!classSet.isEmpty()) {
        generateInitCode(classPool, classSet);
      }
      if (mManagerSrc.isDirectory()) {
        FileUtils.copyDirectory(mManagerSrc, mManagerDst);
      } else {
        FileUtils.copyFile(mManagerSrc, mManagerDst);
      }
    }
    System.out.println(TAG + ": end，cost time=" + (System.currentTimeMillis() - startTime));
  }

  private boolean scanJarFile(File file, Set<String> initClasses) throws IOException {
    JarFile jarFile = new JarFile(file);
    Enumeration<JarEntry> entries = jarFile.entries();
    JarEntry jarEntry;
    boolean hasManager = false;
    while (entries.hasMoreElements()) {
      jarEntry = entries.nextElement();
      if (!checkPackage(jarEntry.getName())) {
        continue;
      }
      if (!hasManager) {
        hasManager |= isManagerClass(jarEntry.getName());
      }
      if (isAptClass(jarEntry.getName())) {
        String className = trimName(jarEntry.getName(), 0).replace(File.separatorChar, '.');
        initClasses.add(className);
      }
    }
    return hasManager;
  }

  private boolean scanDir(File dir, Set<String> initClasses) {
    File packageDir = new File(dir, APT_CREATE_PATH);
    boolean hasManager = false;
    if (packageDir.exists() && packageDir.isDirectory()) {
      Collection<File> files = FileUtils.listFiles(packageDir,
          new SuffixFileFilter(SdkConstants.DOT_CLASS, IOCase.INSENSITIVE),
          TrueFileFilter.INSTANCE);
      for (File f : files) {
        String className = f.getAbsolutePath()
            .substring(dir.getAbsolutePath().length() + 1);
        if (!hasManager) {
          hasManager |= isManagerClass(className);
        }
        if (isAptClass(className)) {
          className = trimName(className, 0).replace(File.separatorChar, '.');
          initClasses.add(className);
        }
      }
    }
    return hasManager;
  }

  private void generateInitCode(ClassPool classPool, Set<String> classSet) {
    try {
      CtClass ctClass = classPool.get(PLUGIN_MANAGER_CLASS);
      if (ctClass.isFrozen()) {
        ctClass.defrost();
      }
      CtMethod ctMethod = ctClass.getDeclaredMethod("init");
      StringBuilder builder;
      ctMethod.setBody("{}");
      for (String className : classSet) {
        System.out.println(TAG + ": className=" + className);
        builder = new StringBuilder();
        builder.append("registerPlugin(")
          .append(className).append(".getKey(), ")
          .append("(java.lang.Object)").append(className).append(".getPlugin(), ")
          .append(className).append(".getType());\n");
        ctMethod.insertAfter(builder.toString());
      }
      if (mManagerSrc.isDirectory()) {
        generateDirCode(ctClass);
      } else {
        generateJarCode(ctClass);
      }
    } catch (NotFoundException | CannotCompileException | IOException | NullPointerException e) {
      e.printStackTrace();
    }
  }

  private void generateDirCode(CtClass ctClass) throws CannotCompileException, IOException {
    ctClass.writeFile(mManagerSrc.getAbsolutePath());
    ctClass.detach();
  }

  private void generateJarCode(CtClass ctClass) throws CannotCompileException, IOException {
    JarFile jarFile = new JarFile(mManagerSrc);
    File tempDir = mInvocation.getContext().getTemporaryDir();
    File outputJar = new File(tempDir, mManagerSrc.getName());
    JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(outputJar));
    Enumeration<JarEntry> enumeration = jarFile.entries();
    JarEntry jarEntry;
    while (enumeration.hasMoreElements()) {
      jarEntry = enumeration.nextElement();
      jarOutputStream.putNextEntry(new ZipEntry(jarEntry.getName()));
      if (checkPackage(jarEntry.getName()) && isManagerClass(jarEntry.getName())) {
        jarOutputStream.write(ctClass.toBytecode());
      } else {
        InputStream inputStream = jarFile.getInputStream(jarEntry);
        jarOutputStream.write(IOUtils.toByteArray(inputStream));
        inputStream.close();
      }
      jarOutputStream.closeEntry();
    }
    jarOutputStream.close();
    jarFile.close();
    saveModifiedFile(outputJar, mManagerSrc);
  }

  private void saveModifiedFile(File newFile, File oldFile) throws IOException {
    if (oldFile.exists()) {
      oldFile.delete();
    }
    FileUtils.copyFile(newFile, oldFile);
  }

  private boolean checkPackage(String classPath) {
    return classPath.endsWith(SdkConstants.DOT_CLASS) && (classPath.startsWith(APT_CREATE_PATH) || classPath.startsWith(PKG));
  }

  private boolean isManagerClass(String classPath) {
    return PLUGIN_MANAGER_CLASS.equals(trimName(classPath, 0).replace(File.separatorChar, '.'));
  }

  private boolean isAptClass(String classPath) {
    return trimName(classPath, 0).endsWith(SUFFIX);
  }

  /**
   * [prefix]com/xxx/aaa.class --> com/xxx/aaa
   * [prefix]com\xxx\aaa.class --> com\xxx\aaa
   */
  private String trimName(String s, int start) {
    return s.substring(start, s.length() - SdkConstants.DOT_CLASS.length());
  }
}
