package com.example;

import java.util.*;

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
     test02();
    }
    private void test01() {
        /**
         * 模拟多线程，同时竞争1-6号库存，竞争时间30秒
         */
        List<StockInfo> stockInfos = new ArrayList<StockInfo>();
        for (long i=0;i<3;i++){
            StockInfo stockInfoPara = new StockInfo() ;
            stockInfoPara.setStockid(i);
            stockInfoPara.setStocknum(0);
            stockInfoPara.setPickoutnum(0);
            stockInfos.add(stockInfoPara);
        }
        service.lockArrays(stockInfos);
    }
    private void test02() {
        /**
         * 模拟多线程，同时竞争0-10号库存中随机的三个库存，竞争时间30秒
         */
        List<StockInfo> stockInfos = new ArrayList<StockInfo>();
        Random random = new Random();
        Set set = new HashSet();
        while (true){
            int randomLong = random.nextInt(11);
            set.add(randomLong);
            if(set.size()==3){
                break;
            }
        }
        Iterator iterator = set.iterator();
        while (iterator.hasNext()){
            int next = ((Integer)iterator.next()).intValue();
            StockInfo stockInfoPara = new StockInfo() ;
            stockInfoPara.setStockid((long)next);
            stockInfoPara.setStocknum(0);
            stockInfoPara.setPickoutnum(0);
            stockInfos.add(stockInfoPara);
        }
        System.out.println(Thread.currentThread().getName()+"----------------------操作的库存："+stockInfos);
        service.lockArrays(stockInfos);
    }
    //模拟一秒内，秒杀50件商品
    private void test03(){
        service.seckill();
    }
}

/**
 * A线程需要1,2资源。B需要1,2资源。A获得1，B获得2，A等待B释放2，B等待A释放1.最终超时失败
 */

class Test4{
    public static void main(String[] args) {
        Service service = new Service();
        for (int i = 0; i < 100; i++) {
            ThreadC thread = new ThreadC(service);
            thread.start();
        }
    }
}