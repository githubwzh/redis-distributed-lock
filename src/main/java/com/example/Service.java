package com.example;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ClassDescribe:
 * Author :wangzhanhua
 * Date: 2017-09-25
 * Since
 * To change this template use File | Settings | File Templates.
 */
public class Service {
    private static JedisPool pool = null;

    public static Map<Long, StockInfo> map = new HashMap<Long, StockInfo>() {
        /**
         * 0-99共一百个库存
         */
        {
            for (long i = 0; i < 15; i++) {
                StockInfo stockInfo = new StockInfo();
                stockInfo.setStockid(i);
                stockInfo.setStocknum(0);
                stockInfo.setPickoutnum(0);
                put(i, stockInfo);
            }
        }
    };

    static {
        JedisPoolConfig config = new JedisPoolConfig();
        // 设置最大连接数
        config.setMaxTotal(200);
        // 设置最大空闲数
        config.setMaxIdle(8);
        // 设置最大等待时间
        config.setMaxWaitMillis(3600);
        // 在borrow一个jedis实例时，是否需要验证，若为true，则所有jedis实例均是可用的
        config.setTestOnBorrow(true);
        pool = new JedisPool(config, "127.0.0.1", 6379, 3000);
    }

    DistributedLock lock = new DistributedLock(pool);

    public static void main(String[] args) {
        Jedis jedis = pool.getResource();
        String lockKey = "lockKey";
        String watch = jedis.watch(lockKey);
        System.out.println(watch + "******");
        transcationTest(jedis, lockKey, "wzh1");
        transcationTest(jedis, lockKey, "wzh3");
    }

    private static void transcationTest(Jedis jedis, String lockKey, String name) {
        Transaction transaction = jedis.multi();
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        transaction.set(lockKey, name);
        System.out.println(jedis.watch(lockKey));
        List<Object> results = transaction.exec();
        if (results == null) {
            System.out.println("------事物执行结果-null---" + results);
        } else {
            System.out.println("------事物执行结果----" + results);
        }
    }

    int n = 500;

    /**
     * 模拟秒杀商品
     */
    public void seckill() {
        // 返回锁的value值，供释放锁时候进行判断
        String indentifier = null;
        try {
            indentifier = lock.lockWithTimeout("resource", 5000, 1000);
            System.out.println(--n);
            lock.releaseLock("resource", indentifier);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 模拟单库存并发操作
     */
    public void processStockInfo(StockInfo stockInfo) {
        String lockname = "stockinfo：" + stockInfo.getStockid();
        // 返回锁的value值，供释放锁时候进行判断,获取锁的时间为10秒，锁过期时间60秒
        String indentifier = null;
        try {
            indentifier = lock.lockWithTimeout(lockname, 10000, 60000);
            StockInfo currStockInfo = Service.map.get(stockInfo.getStockid());
            System.out.println("操作前的库存：" + currStockInfo);
            currStockInfo.setStocknum(currStockInfo.getStocknum() + 1);
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            lock.releaseLock(lockname, indentifier);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void lockArrays(List<StockInfo> stockInfos) {
        try {
            String[] locknames = new String[stockInfos.size()];
            for (int i = 0; i < locknames.length; i++) {
                Long stockid = stockInfos.get(i).getStockid();
                locknames[i] = stockid.toString();
            }
            //获取锁超时时间30秒，锁被获取后有效期60秒。
            Map<String, String> stringStringMap = lock.lockWithTimeout(locknames, 30000, 60000);
            if (stringStringMap != null && stringStringMap.size() > 0) {
                //获得锁成功，执行业务逻辑
                System.out.println("--------处理业务逻辑---start-----" + Thread.currentThread().getName() + "--***--" + Service.map);
                for (StockInfo stockInfo : stockInfos) {
                    StockInfo stockInfoPara = Service.map.get(stockInfo.getStockid());//模拟数据库取数据，更新库存
                    Thread.sleep(200);
                    stockInfoPara.setStocknum(stockInfoPara.getStocknum() + 1);
                }
                System.out.println("--------处理业务逻辑---end-----" + Thread.currentThread().getName() + "--***--" + Service.map);
                lock.releaseLock(stringStringMap);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}