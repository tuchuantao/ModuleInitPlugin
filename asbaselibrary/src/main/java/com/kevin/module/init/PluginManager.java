package com.kevin.module.init;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import android.util.Log;
import android.util.SparseArray;

/**
 * Created by tuchuantao on 2021/7/2
 * Desc: 组件管理类，初始化由Plugin自动注入
 */
public class PluginManager {

  private static final String TAG = "PluginManager";
  private static volatile PluginManager sInstance;

  private static final String EMPTY_IMPL_NAME = "EMPTY"; // 每个接口下的空实现变量名

  private final Map<Class<?>, SparseArray<Object>> mPluginMap = new HashMap<>();
  private boolean mHasLoadPlugins = false;

  private PluginManager() {
  }

  public static PluginManager getInstance() {
    if (sInstance == null) {
      synchronized (PluginManager.class) {
        if (sInstance == null) {
          sInstance = new PluginManager();
        }
      }
    }
    return sInstance;
  }

  /**
   * 由Plugin自动注入
   */
  public synchronized void init() {
  }

  private synchronized void registerPlugin(Class<?> key, Object pluginObj, int id) {
    try {
      SparseArray<Object> pluginList;
      if (mPluginMap.containsKey(key)) {
        pluginList = mPluginMap.get(key);
        if (pluginList != null) {
          pluginList.put(id,pluginObj);
        }
      } else {
        pluginList = new SparseArray<>();
        pluginList.put(id,pluginObj);
      }
      Log.d(TAG, "put plugin key =" + key + ", pluginObj : " + pluginObj.getClass().getName());
      mPluginMap.put(key, pluginList);
    } catch (Exception e) {
      Log.e(TAG, "Register error: " + e);
    }
  }

  public synchronized static <T> T getPlugin(Class<T> interfaceClass) {
    SparseArray<Object> pluginImpList = null;
    try {
      pluginImpList = getInstance().mPluginMap.get(interfaceClass);
    } catch (Exception e) {
      Log.e(TAG, "getPlugin() error: " + e);
    }
    if (pluginImpList == null || pluginImpList.size() == 0) {
      Log.e(TAG, "getPlugin() interfaceClass : " + interfaceClass.getName() + " , get a null");
      pluginImpList = createDefaultPlugin(interfaceClass);
    }
    return (T) pluginImpList.get(0);
  }

  public synchronized static <T> SparseArray<T> getPluginList(Class<T> interfaceClass) {
    SparseArray<Object> pluginImpList = null;
    try {
      pluginImpList = getInstance().mPluginMap.get(interfaceClass);
    } catch (Exception e) {
      Log.e(TAG, "getPlugin() error: " + e);
    }

    if (pluginImpList == null || pluginImpList.size() == 0) {
      pluginImpList = createDefaultPlugin(interfaceClass);
    }

    return (SparseArray<T>) pluginImpList;
  }

  public synchronized static <T> T getPlugin(Class<T> interfaceClass, int id) {
    SparseArray<T> pluginImpList = null;
    try {
      pluginImpList = (SparseArray<T>) getInstance().mPluginMap.get(interfaceClass);
    } catch (Exception e) {
      Log.e(TAG, "getPlugin() error: " + e);
    }

    if (pluginImpList == null || pluginImpList.size() == 0) {
      pluginImpList = createDefaultPlugin(interfaceClass);
    }
    return pluginImpList.get(id,pluginImpList.get(0));
  }

  /**
   * 加载默认空实现，避免到处判空
   *
   * @param interfaceClass
   * @param
   * @return
   */
  private static <T> SparseArray<T> createDefaultPlugin(Class<?> interfaceClass) {
    Log.e(TAG, "createDefaultPlugin() : " + interfaceClass.getName());
    SparseArray<T> pluginList = new SparseArray<>();
    T pluginImp = null;
    try {
      Field field = interfaceClass.getDeclaredField(EMPTY_IMPL_NAME);
      field.setAccessible(true);
      pluginImp = (T) field.get(null);
      pluginList.put(0, (T) pluginImp);
      if (pluginImp != null && getInstance().mHasLoadPlugins) {
        getInstance().mPluginMap.put(interfaceClass, (SparseArray<Object>) pluginList);
      }
    } catch (Exception e) {
      Log.e(TAG, "createDefaultPlugin() error: " + e);
    }
    return pluginList;
  }
}
