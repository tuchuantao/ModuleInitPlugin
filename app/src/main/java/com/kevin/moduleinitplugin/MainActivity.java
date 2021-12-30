package com.kevin.moduleinitplugin;

import android.app.Activity;
import android.os.Bundle;

import com.kevin.module.init.PluginManager;
import com.kevin.module.sub.ISubTest;
import com.kevin.module.sub.ITest;

public class MainActivity extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    PluginManager.getInstance().init();
    PluginManager.getPlugin(ITest.class).printLog("---------");
    String version = PluginManager.getPlugin(ISubTest.class).getVersion();
    PluginManager.getPlugin(ITest.class).printLog("version = " + version);
  }
}