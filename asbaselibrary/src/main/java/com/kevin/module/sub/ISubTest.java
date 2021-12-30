package com.kevin.module.sub;

/**
 * Created by tuchuantao on 2021/12/30
 * Desc:
 */
public interface ISubTest {

  String getVersion();

  ISubTest EMPTY = new ISubTest() {

    @Override
    public String getVersion() {
      return "";
    }
  };
}
