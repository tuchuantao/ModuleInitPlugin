package com.kevin.sdk.asublibrary;

import com.kevin.init.annotation.ModuleProvider;
import com.kevin.module.sub.ISubTest;

/**
 * Created by tuchuantao on 2021/12/30
 * Desc:
 */
@ModuleProvider(interfaceClass = ISubTest.class)
public class SubTest implements ISubTest {

  @Override
  public String getVersion() {
    return "V1.0.0";
  }
}
