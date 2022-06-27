package com.chen.lock.controller;

import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.*;

@RestController
public class LockController {

    private Logger logger = LoggerFactory.getLogger(LockController.class);

    private String maotaiLock = "maotaiLock";

    //模拟一下守护线程为其续期
    private ScheduledExecutorService executorService = null;
    ConcurrentSkipListSet<String> set = new ConcurrentSkipListSet<>();//队列

    @PostConstruct
    public void init(){
        //此处模拟从数据库中向缓存中存入库存数据
        redisTemplate.opsForValue().set(maotai,"100");
    }

    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    String maotai = "maotai20210321001";//茅台商品编号

    @RequestMapping("getMaotai")
    public Object getMaotai(){
        Integer count = Integer.parseInt(redisTemplate.opsForValue().get(maotai));
        //如果还有库存
        if(count > 0){
            //库存减1
            count--;
            redisTemplate.opsForValue().set(maotai,String.valueOf(count));
            logger.info(Thread.currentThread().getName() + "抢到茅台了" + count);
            return "ok";
        }else{
            return "no";
        }

    }

    @RequestMapping("getMaotai2")
    public Object getMaotai2(){
        //获取锁
        Boolean isLock = redisTemplate.opsForValue().setIfAbsent(maotaiLock,"1");
        if(isLock){
            //设置锁过期时间
            redisTemplate.expire(maotaiLock,5, TimeUnit.SECONDS);
            try{
                Integer count = Integer.parseInt(redisTemplate.opsForValue().get(maotai));
                if(count > 0){
                    count--;
                    redisTemplate.opsForValue().set(maotai,String.valueOf(count));
                    logger.info(Thread.currentThread().getName() + "抢到茅台了" + count);
                    return "ok";
                }else{
                    return "no";
                }
            }catch (Exception e){
                e.printStackTrace();

            }finally {
                //释放锁
                redisTemplate.delete(maotaiLock);
            }
        }

        return "dont get lock";
    }

    /**
     * 问题：
     *  1，setnx 和 expire是非原子性操作
     *   有两种解决方案：
     *    1，2.6以前可用使用lua脚本
     *    2，2.6以后可用set命令
     *
     *   2,错误解锁：
     *    如何保证解铃还须系铃人：给锁加一个唯一标识
     */
    @RequestMapping("/getMaotai3")
    public Object getMaotai3(){
        String requestId = UUID.randomUUID().toString() + Thread.currentThread().getId();

        Boolean islock = redisTemplate.opsForValue().setIfAbsent(maotaiLock,requestId,5,TimeUnit.SECONDS);

        if (islock){

            try{
                Integer count = Integer.parseInt(redisTemplate.opsForValue().get(maotai));

                if(count > 0){
                    count--;
                    redisTemplate.opsForValue().set(maotai,String.valueOf(count));
                    logger.info(Thread.currentThread().getName() + "抢到茅台了" + count);
                    return "ok";
                }else{
                    return "no";
                }
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                //释放锁
                /*
                String temp = redisTemplate.opsForValue().get(maotaiLock);
                if(requestId.equals(temp)){
                    //是自己的锁
                    redisTemplate.delete(maotaiLock);
                }
                */
                //原子性
                String unLockLua = "" +
                        "if redis.call('get',KEYS[1]) == ARGV[1] then redis.call('del',KEYS[1]) ; return true " +
                        "else return false " +
                        "end";
                redisTemplate.execute(new RedisCallback<Boolean>() {
                    @Override
                    public Boolean doInRedis(RedisConnection redisConnection) throws DataAccessException {
                        return redisConnection.eval(unLockLua.getBytes(), ReturnType.BOOLEAN,1,maotaiLock.getBytes(),requestId.getBytes());
                    }
                });

            }


        }

        return "dont get lock";
    }

    /**
     *  锁续期/锁续命
     *  拿到锁之后执行业务，业务的执行时间超过了锁的过期时间
     *
     *  如何做？
     *  给拿到锁的线程创建一个守护线程(看门狗)，守护线程定时/延迟 判断拿到锁的线程是否还继续持有锁，如果持有则为其续期
     *
     */
    @PostConstruct
    public void init2(){
        executorService = Executors.newScheduledThreadPool(1);
        //编写续期的lua脚本
        String expireNew = "" +
                "if redis.call('get',KEYS[1]) == ARGV[1] then redis.call('expire',KEYS[1],ARGV[2]);return true " +
                "else return false " +
                "end";
        executorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                Iterator<String> iterator = set.iterator();
                while (iterator.hasNext()){
                    String requestId = iterator.next();
                    redisTemplate.execute(new RedisCallback<Boolean>() {
                        @Override
                        public Boolean doInRedis(RedisConnection redisConnection) throws DataAccessException {
                            Boolean result = false;
                            try{
                                result = redisConnection.eval(expireNew.getBytes(),ReturnType.BOOLEAN,1,maotaiLock.getBytes(),requestId.getBytes(),"5".getBytes());
                            }catch (Exception e){
                                e.printStackTrace();
                                logger.error("锁续期失败,{}",e.getMessage());
                            }
                            return result;
                        }
                    });
                }
            }
        },0,1,TimeUnit.SECONDS);
    }

    @RequestMapping("/getMaotai4")
    public Object getMaotai4(){
        String requestId = UUID.randomUUID().toString();

        try{
            Boolean isLock = redisTemplate.opsForValue().setIfAbsent(maotaiLock,requestId);

            if(isLock){
                Integer count = Integer.parseInt(redisTemplate.opsForValue().get(maotai));
                set.add(requestId);
                if(count <= 0){
                    return "no";
                }else{
                    logger.info(Thread.currentThread().getName() + "抢到茅台了" + count);
                    count--;
                    //模拟业务超时
                    TimeUnit.SECONDS.sleep(10);
                    redisTemplate.opsForValue().set(maotai,String.valueOf(count));
                    return "ok";
                }

            }else{
                return "donot get lock";
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            set.remove(requestId);
            //释放锁
            String unLockLua = "" +
                    "if redis.call('get',KEYS[1]) == ARGV[1] then redis.call('del',KEYS[1]) ; return true " +
                    "else return false "+
                    "end";

            redisTemplate.execute(new RedisCallback<Boolean>() {
                @Override
                public Boolean doInRedis(RedisConnection redisConnection) throws DataAccessException {
                    Boolean result = false;
                    try{
                        result = redisConnection.eval(unLockLua.getBytes(),ReturnType.BOOLEAN,1,maotaiLock.getBytes(),requestId.getBytes());
                    }catch (Exception e){
                        e.printStackTrace();
                    }

                    return result;
                }
            });
        }

        return "error";
    }

    @Value("${spring.redis.host}")
    String host;
    @Value("${spring.redis.port}")
    String port;

    @Bean
    public RedissonClient redissonClient(){
        Config config = new Config();
        config.useSingleServer().setAddress("redis://"+host+":"+port).setPassword("cm021035");
        return Redisson.create(config);
    }

    @Autowired
    private RedissonClient redissonClient;

    @GetMapping("/getMaotai5")
    public Object getMaotai5(){
        //获取锁
        RLock lock = redissonClient.getLock(maotaiLock);
        lock.lock();
        try{
            Integer count = Integer.parseInt(redisTemplate.opsForValue().get(maotai));
            if(count > 0){
                count--;
                redisTemplate.opsForValue().set(maotai,String.valueOf(count));
                logger.info("我抢到茅台了!");
                return "ok";
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            lock.unlock();
        }

        return "";
    }

}
