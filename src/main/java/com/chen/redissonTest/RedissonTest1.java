package com.chen.redissonTest;

import com.chen.redissonTest.entity.City;
import com.chen.redissonTest.entity.Message;
import org.junit.Before;
import org.junit.Test;
import org.redisson.Redisson;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.MessageListener;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;

import java.util.concurrent.CountDownLatch;

public class RedissonTest1 {

    private RedissonClient redissonClient = null;

    @Before
    public void createRedissonClient(){
        String host = "127.0.0.1";
        String port = "6379";

        Config config = new Config();
        config.useSingleServer().setAddress("redis://"+host+":"+port).setPassword("cm021035");

        redissonClient =  Redisson.create(config);
    }

    @Test
    public void test01(){

        RBucket<Object> bucket = redissonClient.getBucket("city",new StringCodec("utf-8"));
        bucket.set("nanjing");
        Object o = bucket.get();
        System.out.println(o.getClass());
        System.out.println(o);
    }

    @Test
    public void test02(){

        RBucket<Object> bucket = redissonClient.getBucket("city");
        City city = new City();//对象必须实现序列化接口
        city.setName("nanjing");
        city.setProvince("jiangsu");
        bucket.set(city);
        City temp = (City) bucket.get();
        System.out.println(temp);
    }

    @Test
    public void test03() throws InterruptedException {

        RAtomicLong count = redissonClient.getAtomicLong("count");


        CountDownLatch countDownLatch = new CountDownLatch(100);
        for(int i = 0;i < 100;i++){
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    for(int j = 0;j < 100;j++){
                        count.incrementAndGet();
                    }

                    countDownLatch.countDown();
                }
            });

            thread.start();

        }

        countDownLatch.await();

        System.out.println(count.get());
    }

    @Test
    public void test04(){
        RTopic<Message> topic = redissonClient.getTopic("anyTopic");
        Message message = new Message();
        message.setTitle("呵呵");
        message.setArticle("ovnfoevnfeonvf");
        topic.publish(message);
    }

    @Test
    public void test05() throws InterruptedException {
        RTopic<Message> topic = redissonClient.getTopic("anyTopic");
        topic.addListenerAsync( new MessageListener<Message>() {
            @Override
            public void onMessage(CharSequence charSequence, Message message) {
                System.out.println(message.getTitle());
            }
        });

    }

}
