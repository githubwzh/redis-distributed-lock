package com.example;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.exceptions.JedisException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
                    conn.expire(lockKey, lockExpire);
                    // 返回value值，用于释放锁时间确认
                    retIdentifier = identifier;
                    return retIdentifier;
                }
                // 返回-1代表key没有设置超时时间，为key设置一个超时时间
                if (conn.ttl(lockKey) == -1) {
                    conn.expire(lockKey, lockExpire);
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            throw new Exception("获取锁超时，用时："+acquireTimeout+"毫秒");
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
    int count =100;
    public Map<String,String> lockWithTimeout(String[] locknames, long acquireTimeout, long timeout) throws Exception {
        Jedis conn = null;
        conn = jedisPool.getResource();
        Map<String,String> locknameIdentifierMap = new HashMap<String, String>();
        String preLockname = "lock:";
        //为锁名称加前缀
        for(int i=0;i<locknames.length;i++){
            locknames[i] = preLockname + locknames[i];
        }
        // 获取锁的超时时间，超过这个时间则放弃获取锁
        try {
            long end = System.currentTimeMillis() + acquireTimeout;
            int  lockExpire = (int) (timeout / 1000);
            while (System.currentTimeMillis() < end) {
                // 获取连接
                //1、监视locknames 2、查询redis，看是否有被占用的锁，如果有等待5秒重新查询。注意顺序
                while (true) {
                    boolean isUnique = uniqueIdentify(conn, locknames,lockExpire);
                    if (isUnique) {//监视锁,所有的锁没有被占用
                        System.out.println("线程："+Thread.currentThread().getName()+"准备就绪，开始获取锁");
                        break;
                    }else{
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
                Transaction transaction = conn.multi();
                for (int i = 0; i < locknames.length; i++) {
                    String identifier = UUID.randomUUID().toString(); // 随机生成一个value
                    locknameIdentifierMap.put(locknames[i],identifier);//释放锁用
                    transaction.setnx(locknames[i], identifier);
                }
                List<Object> execRes = transaction.exec();
                conn.unwatch();
                boolean commiteSuccess = true;
                if(execRes == null){
                    System.out.println("线程："+Thread.currentThread().getName()+"获取锁【失败】");
                    continue;
                }
                for(Object obj:execRes){
                    System.out.println("线程："+Thread.currentThread().getName()+"事物执行返回值："+obj);
                    Long res =(Long) obj;
                    if(res == 1){
                        continue;
                    }
                    commiteSuccess = false;
                }
                if(commiteSuccess){
                    System.out.println("序号【"+(--count)+"】线程："+Thread.currentThread().getName()+"获取锁【成功】");
                    // 超时时间，上锁后超过此时间则自动释放锁
                    for (String lockname:locknames){
                        conn.expire(lockname,lockExpire);
                    }
                    return locknameIdentifierMap;
                }else{
                    //没有全部成功,释放锁
                    for (String lockname:locknames){
                        conn.expire(lockname,0);
                    }
                }
            }
            throw new Exception("获取锁超时，用时："+acquireTimeout+"毫秒");
        } catch (JedisException e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
        return null;
    }

    /**
     * 被监视的锁，没有被占用，返回true，否则取消监视，返回false。
     * @param conn
     * @param locknames
     *@param lockExpire  @return
     */
    private boolean uniqueIdentify(Jedis conn, String[] locknames, int lockExpire) {
        conn.watch(locknames);
        for(String lockname:locknames){
            String s = conn.get(lockname);
            if(s != null){
                if(conn.ttl(lockname)==-1){ // 返回-1代表key没有设置超时时间，为key设置一个超时时间
                    conn.expire(lockname,lockExpire);
                }
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
                 conn.watch(lockKey);
                // 通过前面返回的value值判断是不是该锁，若是该锁，则删除，释放锁
                if (identifier.equals(conn.get(lockKey))) {
                    Transaction transaction = conn.multi();
                    transaction.del(lockKey);
                    List<Object> results = transaction.exec();
                    if (results == null) {
                        continue;
                    }
                    retFlag = true;
                }else{
                    //TODO 非本客户端创建的锁
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
    /**
     * 释放锁
     * @param  lockNameIdentifierMap key: 锁的key,value:释放锁的标识
     * @return
     */
    public boolean releaseLock(Map<String,String> lockNameIdentifierMap) throws Exception{
        Jedis conn = null;

        boolean retFlag = false;
        try {
            conn = jedisPool.getResource();
            lockNameIdentifierMap.entrySet();
            for(Map.Entry<String,String> entry:lockNameIdentifierMap.entrySet()){
                String lockKey = entry.getKey();
                String identifier = entry.getValue();
                while (true) {
                    // 监视lock，准备开始事务
                    String watch = conn.watch(lockKey);
                    // 通过前面返回的value值判断是不是该锁，若是该锁，则删除，释放锁
                    if (identifier.equals(conn.get(lockKey))) {
                        Transaction transaction = conn.multi();
                        transaction.del(lockKey);
                        List<Object> results = transaction.exec();
                        if (results == null ) {
                            continue;
                        }
                    }else{
                        //释放时，发现要释放的锁，不是该线程创建的锁对象，原因有可能业务逻辑用的时间超过，锁的有效时间，
                        // 或者某些原因长时间阻塞
                        throw new Exception("---释放的锁不是本线程创建的----");
                    }
                    conn.unwatch();
                    break;
                }

            }
            retFlag = true;
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
