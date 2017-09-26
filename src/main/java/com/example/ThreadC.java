package com.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * ClassDescribe:
 * Author :wangzhanhua
 * Date: 2017-09-25
 * Since
 * To change this template use File | Settings | File Templates.
 */
class ThreadC extends Thread {
    private Service service;

    public ThreadC(Service service) {
        this.service = service;
    }

    @Override
    public void run() {
        List<StockInfo> stockInfos = new ArrayList<StockInfo>();
        Random random = new Random();
        for (long i=1;i<4;i++){
//            int randomLong = random.nextInt(11);
            StockInfo stockInfoPara = new StockInfo() ;
            stockInfoPara.setStockid((long)i);
            stockInfoPara.setStocknum(0);
            stockInfoPara.setPickoutnum(0);
            stockInfos.add(stockInfoPara);
        }
//        service.processStockInfoList(stockInfos);
        service.lockArrays();
    }
}

/**
 * A线程需要1,2资源。B需要1,2资源。A获得1，B获得2，A等待B释放2，B等待A释放1.最终超时失败
 */

class Test4{
    public static void main(String[] args) {
        Service service = new Service();
        for (int i = 0; i < 5; i++) {
            ThreadC thread = new ThreadC(service);
            thread.start();
        }
    }
}