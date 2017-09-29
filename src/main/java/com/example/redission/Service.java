package com.example.redission;

import com.example.StockInfo;
import org.redisson.Redisson;
import org.redisson.RedissonMultiLock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * ClassDescribe:
 * Author :wangzhanhua
 * Date: 2017-09-25
 * Since redission 官网 https://github.com/redisson/redisson
 * To change this template use File | Settings | File Templates.
 */
public class Service {
    private static RedissonClient redisson = null;

    public static Map<Long, StockInfo> map = new HashMap<Long, StockInfo>() {
        /**
         * 0-99共一百个库存
         */ {
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
        Config config = new Config();
        config.useSingleServer().setAddress("127.0.0.1:6379");
        redisson = Redisson.create(config);
    }

    public static void main(String[] args) {
//      test01();
        test02();
    }

    private static void test01() {
        RLock lock = redisson.getLock("anyLock");
        // 最常见的使用方法
        lock.lock();
        // 支持过期解锁功能
        // 10秒钟以后自动解锁
        // 无需调用unlock方法手动解锁
        lock.lock(10, TimeUnit.SECONDS);

        // 尝试加锁，最多等待100秒，上锁以后10秒自动解锁
        try {
            boolean res = lock.tryLock(100, 10, TimeUnit.SECONDS);
            System.out.println(res);
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        lock.unlock();
    }

    public static void test02() {
        RLock lock1 = redisson.getLock("lock1");
        RLock lock2 = redisson.getLock("lock2");
        RLock lock3 = redisson.getLock("lock3");

        RedissonMultiLock lock = new RedissonMultiLock(lock1, lock2, lock3);
// 同时加锁：lock1 lock2 lock3
// 所有的锁都上锁成功才算成功。
        lock.lock();
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        lock.unlock();
    }

    public static void test03() {
        /**
         * 模拟多线程，同时竞争0-10号库存中随机的三个库存，竞争时间30秒
         */
        List<StockInfo> stockInfos = new ArrayList<StockInfo>();
        Random random = new Random();
        Set set = new HashSet();
        while (true) {
            int randomLong = random.nextInt(11);
            set.add(randomLong);
            if (set.size() == 3) {
                break;
            }
        }
        Iterator iterator = set.iterator();
        while (iterator.hasNext()) {
            int next = ((Integer) iterator.next()).intValue();
            StockInfo stockInfoPara = new StockInfo();
            stockInfoPara.setStockid((long) next);
            stockInfoPara.setStocknum(0);
            stockInfoPara.setPickoutnum(0);
            stockInfos.add(stockInfoPara);
        }
        System.out.println(Thread.currentThread().getName() + "----------------------操作的库存：" + stockInfos);
        //库存id，加锁
        RLock lock1 = redisson.getLock("lock:" + stockInfos.get(0).getStockid());
        RLock lock2 = redisson.getLock("lock:" + stockInfos.get(1).getStockid());
        RLock lock3 = redisson.getLock("lock:" + stockInfos.get(2).getStockid());
        RedissonMultiLock lock = new RedissonMultiLock(lock1, lock2, lock3);
        lock.lock();
        try {
            System.out.println("--------处理业务逻辑---start-----" + Thread.currentThread().getName() + "--***--" + Service.map);
            for (StockInfo stockInfo : stockInfos) {
                StockInfo stockInfoPara = Service.map.get(stockInfo.getStockid());//模拟数据库取数据，更新库存
                Thread.sleep(200);
                stockInfoPara.setStocknum(stockInfoPara.getStocknum() + 1);
            }
            System.out.println("--------处理业务逻辑---end-----" + Thread.currentThread().getName() + "--***--" + Service.map);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        lock.unlock();
    }
}
