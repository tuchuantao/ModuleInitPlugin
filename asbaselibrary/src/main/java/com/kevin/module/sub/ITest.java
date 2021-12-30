package com.kevin.module.sub;

/**
 * Created by tuchuantao on 2021/12/30
 * Desc:
 */
public interface ITest {

  void printLog(String msg);

  ITest EMPTY = new ITest() {

    @Override
    public void printLog(String msg) {
    }
  };
}
