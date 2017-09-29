package com.example;

/**
 * ClassDescribe:
 * Author :wangzhanhua
 * Date: 2017-09-25
 * Since
 * To change this template use File | Settings | File Templates.
 */
class ThreadB extends Thread {
    private Service service;

    public ThreadB(Service service) {
        this.service = service;
    }

    @Override
    public void run() {
        StockInfo stockInfoPara = new StockInfo();
        stockInfoPara.setStockid(3L);
        service.processStockInfo(stockInfoPara);
    }
}

class Test3 {
    public static void main(String[] args) {
        Service service = new Service();
        for (int i = 0; i < 50; i++) {
            ThreadB threadB = new ThreadB(service);
            threadB.start();
        }
    }
}
