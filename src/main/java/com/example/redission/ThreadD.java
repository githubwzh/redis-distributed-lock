package com.example.redission;

import com.example.StockInfo;

import java.util.*;

/**
 * ClassDescribe:
 * Author :wangzhanhua
 * Date: 2017-09-25
 * Since
 * To change this template use File | Settings | File Templates.
 */
class ThreadD extends Thread {
    private com.example.redission.Service service;

    public ThreadD(com.example.redission.Service service) {
        this.service = service;
    }

    @Override
    public void run() {
        service.test03();
    }
}
 class Test5{
    public static void main(String[] args) {
        com.example.redission.Service service = new Service();
        for (int i = 0; i < 100; i++) {
            ThreadD thread = new ThreadD(service);
            thread.start();
        }
    }
}