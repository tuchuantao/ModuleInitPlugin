package com.kevin.init.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by tuchuantao on 2021/12/23
 * Desc:
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface ModuleProvider {

  Class<?> interfaceClass();

  int type() default 0; // 支持同一接口多种实现
}
