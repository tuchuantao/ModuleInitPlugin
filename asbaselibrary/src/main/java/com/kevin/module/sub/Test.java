package com.kevin.module.sub;

import android.util.Log;

import com.kevin.init.annotation.ModuleProvider;

/**
 * Created by tuchuantao on 2021/12/30
 * Desc:
 */
@ModuleProvider(interfaceClass = ITest.class)
public class Test implements ITest {

  @Override
  public void printLog(String msg) {
    Log.d("Test", msg);
  }
}
