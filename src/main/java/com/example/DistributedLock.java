package com.example;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.exceptions.JedisException;

import java.util.List;
import java.util.UUID;

/**
 * ClassDescribe:
 * Author :wangzhanhua
 * Date: 2017-09-25
 * Since http://www.cnblogs.com/liuyang0/p/6744076.html
 * To change this template use File | Settings | File Templates.
 */
public class DistributedLock {
    private final JedisPool jedisPool;

    public DistributedLock(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }
    /**
     * 加锁
     * @param locaName  锁的key
     * @param acquireTimeout  获取超时时间
     * @param timeout   锁的超时时间，需要满足业务需要
     *
     * @return 锁标识
     */
    public String lockWithTimeout(String locaName, long acquireTimeout, long timeout) throws Exception {
        Jedis conn = null;
        String retIdentifier = null;
        try {
            // 获取连接
            conn = jedisPool.getResource();
            // 随机生成一个value
            String identifier = UUID.randomUUID().toString();
            // 锁名，即key值
            String lockKey = "lock:" + locaName;
            // 超时时间，上锁后超过此时间则自动释放锁
            int lockExpire = (int)(timeout / 1000);

            // 获取锁的超时时间，超过这个时间则放弃获取锁
            long end = System.currentTimeMillis() + acquireTimeout;
            while (System.currentTimeMillis() < end) {
                if (conn.setnx(lockKey, identifier) == 1) {
                    System.out.println("suiji----"+Thread.currentThread().getName()+identifier);
                    conn.expire(lockKey, lockExpire);
                    // 返回value值，用于释放锁时间确认
                    retIdentifier = identifier;
                    System.out.println("*******"+Thread.currentThread().getName()+"-------"+identifier);
                    return retIdentifier;
                }
                // 返回-1代表key没有设置超时时间，为key设置一个超时时间
                if (conn.ttl(lockKey) == -1) {
                    System.out.println("----设置超时时间"+lockKey);
                    conn.expire(lockKey, lockExpire);
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            throw new Exception("获取锁超时，用时："+acquireTimeout+"秒");
        } catch (JedisException e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
        return retIdentifier;
    }
    /**
     * 加锁
     * @param locknames  锁的key
     * @param acquireTimeout  获取超时时间
     * @param timeout   锁的超时时间，需要满足业务需要
     *
     * @return 锁标识
     */
    public String lockWithTimeoutArray(String[] locknames, long acquireTimeout, long timeout) throws Exception {
        Jedis conn = null;
        String retIdentifier = null;
        // 获取锁的超时时间，超过这个时间则放弃获取锁
        try {
            long end = System.currentTimeMillis() + acquireTimeout;
            while (System.currentTimeMillis() < end) {
                // 获取连接
                conn = jedisPool.getResource();
                //1、监视locknames 2、查询redis，看是否有被占用的锁，如果有等待5秒重新查询。注意顺序
                while (true) {
                    boolean isUnique = uniqueIdentify(conn, locknames);
                    if (isUnique) {//监视锁,所有的锁没有被占用
                        System.out.println("**********---*******");
                        break;
                    }
                }
                Transaction transaction = conn.multi();
                String[] identifiers = new String[locknames.length];
                String[] lockKeys = new String[locknames.length];
                int  lockExpire = (int) (timeout / 1000);
                for (int i = 0; i < locknames.length; i++) {
                    String lockname = locknames[i];
                    // 随机生成一个value
                    identifiers[i] = UUID.randomUUID().toString();
                    // 锁名，即key值
                     lockKeys[i] = "lock:" + lockname;
                    // 超时时间，上锁后超过此时间则自动释放锁

                    transaction.setnx(lockKeys[i], identifiers[i]);
                    transaction.expire(lockKeys[i], lockExpire);
                    List<Object> exec = transaction.exec();
                    conn.unwatch();
                    for(Object obj:exec){
                        System.out.println("----obj:"+obj);
                    }
                }
                // 返回-1代表key没有设置超时时间，为key设置一个超时时间
                for(String lockKey:lockKeys){
                    if (conn.ttl(lockKey) == -1) {
                        System.out.println("----设置超时时间" + lockKey);
                        conn.expire(lockKey, lockExpire);
                    }
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

            }
            throw new Exception("获取锁超时，用时："+acquireTimeout+"秒");
        } catch (JedisException e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
        return retIdentifier;
    }

    /**
     * 被监视的锁，没有被占用，返回true，否则取消监视，返回false。
     * @param conn
     * @param locknames
     * @return
     */
    private boolean uniqueIdentify(Jedis conn, String[] locknames) {
        conn.watch(locknames);
        for(String lockname:locknames){
            String s = conn.get(lockname);
            if(s != null){
                conn.unwatch();
              return false;
            }
        }
        return true;
    }

    /**
     * 释放锁
     * @param lockName 锁的key
     * @param identifier    释放锁的标识
     * @return
     */
    public boolean releaseLock(String lockName, String identifier) {
        Jedis conn = null;
        String lockKey = "lock:" + lockName;
        boolean retFlag = false;
        try {
            conn = jedisPool.getResource();
            while (true) {
                // 监视lock，准备开始事务
                String watch = conn.watch(lockKey);
                // 通过前面返回的value值判断是不是该锁，若是该锁，则删除，释放锁
                if (identifier.equals(conn.get(lockKey))) {
                    Transaction transaction = conn.multi();
                    transaction.del(lockKey);
                    List<Object> results = transaction.exec();
                    if (results == null) {
                        continue;
                    }
                    retFlag = true;
                }
                 conn.unwatch();
                break;
            }
        } catch (JedisException e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
        return retFlag;
    }
}
