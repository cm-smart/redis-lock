package com.chen.cache.util;

import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Map;
import java.util.Set;

@Component
public class JedisUtil {
    private JedisPool pool;
    private String ip = "192.168.29.128";
    private int port = 6379;

    public JedisUtil(){
        if(pool == null){
            JedisPoolConfig config = new JedisPoolConfig();
            config.setMaxTotal(500);
            config.setMaxIdle(5);
            config.setMaxWaitMillis(100);
            config.setTestOnBorrow(true);
            pool = new JedisPool(config,this.ip,this.port,100000);
        }
    }

    /**
     * 通过key获取储存在redis中的value 并释放连
     * @param key
     * @return
     */
    public String get(String key){
        Jedis jedis = null;

        String value = null;

        try{
            jedis = pool.getResource();
            value = jedis.get(key);
        }catch (Exception e){
            pool.returnBrokenResource(jedis);
            e.printStackTrace();
        }finally {
            returnResource(pool,jedis);
        }

        return value;
    }

    /**
     * 通过key 对value进行加?+1操作,当value不是int类型时会返回错误,当key不存在是则value1
     * @param key
     * @return
     */
    public Long incr(String key){
        Jedis jedis = null;
        Long res = null;
        try{
            jedis = pool.getResource();
            res = jedis.incr(key);
        }catch (Exception e){
            pool.returnResourceObject(jedis);
            e.printStackTrace();
        }finally {
            returnResource(pool,jedis);
        }
        return res;
    }

    /**
     * 通过key向指定的set中添加value
     * @param key
     * @param members
     * @return
     */
    public Long sadd(String key,String... members){
        Jedis jedis = null;
        Long res = null;
        try{
            jedis = pool.getResource();
            res = jedis.sadd(key,members);
        }catch (Exception e){
            pool.returnResourceObject(jedis);
            e.printStackTrace();
        }finally {
            returnResource(pool,jedis);
        }

        return res;
    }

    /**
     * 设置key过期时间
     * @param key
     * @param times
     */
    public void expire(String key,int times){
        Jedis jedis = null;
        try{
            jedis = pool.getResource();
            jedis.expire(key,times);
        }catch (Exception e){
            pool.returnResourceObject(jedis);
            e.printStackTrace();
        }finally {
            returnResource(pool,jedis);
        }
    }

    /**
     * 通过key同时设置hash的多个field
     * @param key
     * @param hash
     * @return
     */
    public String hmset(String key, Map<String,String> hash){
        Jedis jedis = null;
        String res = null;
        try{
            jedis = pool.getResource();
            res = jedis.hmset(key,hash);
        }catch (Exception e){
            pool.returnResourceObject(jedis);
            e.printStackTrace();
        }finally {
            returnResource(pool,jedis);
        }
        return res;
    }

    /**
     * 通过key向zset中添加value,score,其中score就是用来排序 如果该value已经存在则根据score更新元素
     *
     * @param key
     * @param scoreMembers
     * @return
     */
    public Long zadd(String key, Map<String,Double> scoreMembers) {
        Jedis jedis = null;
        Long res = null;
        try {
            jedis = pool.getResource();
            res = jedis.zadd(key, scoreMembers);
        } catch (Exception e) {
            pool.returnResourceObject(jedis);
            e.printStackTrace();
        } finally {
            returnResource(pool, jedis);
        }
        return res;
    }

    /**
     * 通过key向zset中添加value,score,其中score就是用来排序 如果该value已经存在则根据score更新元素
     *
     * @param key
     * @param score
     * @param member
     * @return
     */
    public Long zadd(String key, double score, String member) {
        Jedis jedis = null;
        Long res = null;
        try {
            jedis = pool.getResource();
            res = jedis.zadd(key, score, member);
        } catch (Exception e) {
            pool.returnResourceObject(jedis);
            e.printStackTrace();
        } finally {
            returnResource(pool, jedis);
        }
        return res;
    }

    /**
     * 通过key获取zset中value的score
     *
     * @param key
     * @param member
     * @return
     */
    public Double zscore(String key, String member) {
        Jedis jedis = null;
        Double res = null;
        try {
            jedis = pool.getResource();
            res = jedis.zscore(key, member);
        } catch (Exception e) {
            pool.returnResourceObject(jedis);
            e.printStackTrace();
        } finally {
            returnResource(pool, jedis);
        }
        return res;
    }

    /**
     * 通过key增加该zset中value的score的?
     *
     * @param key
     * @param score
     * @param member
     * @return
     */
    public Double zincrby(String key, double score, String member) {
        Jedis jedis = null;
        Double res = null;
        try {
            jedis = pool.getResource();
            res = jedis.zincrby(key, score, member);
        } catch (Exception e) {
            pool.returnResourceObject(jedis);
            e.printStackTrace();
        } finally {
            returnResource(pool, jedis);
        }
        return res;
    }

    public long hincrBy(String key, String field, long value) {
        Jedis jedis = null;
        long res = 0l;
        try {
            jedis = pool.getResource();
            res = jedis.hincrBy(key, field, value);
        } catch (Exception e) {
            pool.returnBrokenResource(jedis);
            e.printStackTrace();
        } finally {
            returnResource(pool, jedis);
        }
        return res;
    }

    /**
     * 通过key  field 获取指定 value
     *
     * @param key
     * @param field
     * @return 没有返回null
     */
    public String hget(String key, String field) {
        Jedis jedis = null;
        String res = null;
        try {
            jedis = pool.getResource();
            res = jedis.hget(key, field);
        } catch (Exception e) {
            pool.returnResourceObject(jedis);
            e.printStackTrace();
        } finally {
            returnResource(pool, jedis);
        }
        return res;
    }

    public Map<String,String> hgetAll(String key) {
        Jedis jedis = null;
        Map<String,String> res = null;
        try {
            jedis = pool.getResource();
            res = jedis.hgetAll(key);
        } catch (Exception e) {
            pool.returnResourceObject(jedis);
            e.printStackTrace();
        } finally {
            returnResource(pool, jedis);
        }
        return res;

    }

    /**
     * 通过key将获取score从start到end中zset的value socre从大到小排序 当start0 end-1时返回全
     *
     * @param key
     * @param start
     * @param end
     * @return
     */
    public Set<String> zrevrange(String key, long start, long end) {
        Jedis jedis = null;
        Set<String> res = null;
        try {
            jedis = pool.getResource();
            res = jedis.zrevrange(key, start, end);
        } catch (Exception e) {
            pool.returnResourceObject(jedis);
            e.printStackTrace();
        } finally {
            returnResource(pool, jedis);
        }
        return res;
    }

    /**
     * 返还到连接池
     *
     * @param pool
     * @param redis
     */
    public static void returnResource(JedisPool pool, Jedis jedis) {
        if (jedis != null) {
            pool.returnResource(jedis);
        }
    }
}
