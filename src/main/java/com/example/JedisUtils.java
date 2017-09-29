package com.example;

import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisException;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * jedis工具类
 *
 * @author zhangwei
 * @version 2016-5-10
 */
public class JedisUtils {

    private static JedisPool pool = null;
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

    public static void main(String[] args) {
        String[] parametters = {"wzh10","wzh11","wzh12"};
        Service service = new Service();
        DistributedLock lock = new DistributedLock(pool) ;
        try {
            lock.lockWithTimeout(parametters,5000,6000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * 获取缓存
     *
     * @param key 键
     * @return 值
     */
    public static String get(String key) {
        Jedis jedis = null;
        try {
            jedis = getResource();
            return jedis.get(key);
        } finally {
            returnResource(jedis);
        }
    }

    /**
     * 设置缓存
     *
     * @param key          键
     * @param value        值
     * @param cacheSeconds 超时时间，0为不超时
     * @return
     */
    public static String set(String key, String value, int cacheSeconds) {
        String result = null;
        Jedis jedis = null;
        try {
            jedis = getResource();
            jedis.setex("12",10,"12");
            result = jedis.set(key, value);
            if (cacheSeconds != 0) {
                jedis.expire(key, cacheSeconds);
            }
        } finally {
            returnResource(jedis);
        }
        return result;
    }


    

    /**
     * 获取List缓存
     *
     * @param key 键
     * @return 值
     */
    public static List<String> getList(String key) {
        List<String> value = null;
        Jedis jedis = null;
        try {
            jedis = getResource();
            if (jedis.exists(key)) {
                value = jedis.lrange(key, 0, -1);
            }
        } finally {
            returnResource(jedis);
        }
        return value;
    }

    /**
     * 设置List缓存
     *
     * @param key          键
     * @param value        值
     * @param cacheSeconds 超时时间，0为不超时
     * @return
     */
    public static long setList(String key, List<String> value, int cacheSeconds) {
        long result = 0;
        Jedis jedis = null;
        try {
            jedis = getResource();
            if (jedis.exists(key)) {
                jedis.del(key);
            }
            result = jedis.rpush(key, value.toArray(new String[value.size()]));
            if (cacheSeconds != 0) {
                jedis.expire(key, cacheSeconds);
            }
        }finally {
            returnResource(jedis);
        }
        return result;
    }

    /**
     * 删除缓存
     *
     * @param key 键
     * @return
     */
    public static long del(String key) {
        long result = 0;
        Jedis jedis = null;
        try {
            jedis = getResource();
            if (jedis.exists(key)) {
                result = jedis.del(key);
            } 
        } finally {
            returnResource(jedis);
        }
        return result;
    }

    /**
     * 设置缓存时间
     *
     * @param key     键
     * @param seconds 缓存时间
     */
    public static void expire(String key, int seconds) {
        Jedis jedis = null;
        try {
            jedis = getResource();
            jedis.expire(key, seconds);
        } finally {
            returnResource(jedis);
        }
    }

    /**
     * 缓存是否存在
     *
     * @param key 键
     * @return
     */
    public static boolean exists(String key) {
        boolean result = false;
        Jedis jedis = null;
        try {
            jedis = getResource();
            result = jedis.exists(key);
        } finally {
            returnResource(jedis);
        }
        return result;
    }


    

    /**
     * 获取资源
     *
     * @return
     * @throws JedisException
     */
    public static Jedis getResource() throws JedisException {
        return pool.getResource();
    }

    /**
     * 释放资源,已过期，官方重写了jedis.close()
     *
     * @param jedis
     */
    public static void returnResource(Jedis jedis) {
        try {
            if (jedis != null) {
                jedis.close();
            }
        } catch (Exception e) {
            System.out.println("关闭Jedis资源异常");
        }
    }

    

}
