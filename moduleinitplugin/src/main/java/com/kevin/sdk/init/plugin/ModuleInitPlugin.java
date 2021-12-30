package com.kevin.sdk.init.plugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

import com.android.build.gradle.BaseExtension;

/**
 * Created by tuchuantao on 2021/12/23
 * Desc:
 */
public class ModuleInitPlugin implements Plugin<Project> {

  @Override
  public void apply(Project project) {
    project.getExtensions().create(ModuleInitExtension.NAME, ModuleInitExtension.class);
    project.getExtensions().findByType(BaseExtension.class)
        .registerTransform(new ModuleInitTransform(project));
  }
}
