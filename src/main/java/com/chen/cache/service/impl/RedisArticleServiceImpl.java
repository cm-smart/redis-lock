package com.chen.cache.service.impl;

import com.chen.cache.basic.Constants;
import com.chen.cache.service.RedisArticleService;
import com.chen.cache.util.JedisUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Service
public class RedisArticleServiceImpl implements RedisArticleService {

    @Resource
    private JedisUtil jedisUtil;

    /**
     * 发表文章
     * @param title
     * @param content
     * @param link
     * @param userId
     * @return
     */
    @Override
    public String postArticle(String title, String content, String link, String userId) {
        //article:001
        String articleId = String.valueOf(jedisUtil.incr("article:"));

        //投票键：voted:001
        String voted = "voted:"+articleId;

        jedisUtil.sadd(voted,userId);
        jedisUtil.expire(voted, Constants.ONE_WEEK_IN_SECONDS);

        long now = System.currentTimeMillis() / 1000;

        String article = "article:" + articleId;

        HashMap<String,String> articleData = new HashMap<>();
        articleData.put("title",title);
        articleData.put("link",link);
        articleData.put("user",userId);
        articleData.put("now",String.valueOf(now));
        articleData.put("votes","1");

        jedisUtil.hmset(article,articleData);
        jedisUtil.zadd("score:info",Constants.VOTE_SCORE,article);
        jedisUtil.zadd("time:",now,article);

        return articleId;
    }

    /**
     * 文章投票
     * @param userId
     * @param
     */
    @Override
    public void articleVote(String userId, String article) {
        //计算投票截止时间
        long cutoff = (System.currentTimeMillis() / 1000) - Constants.ONE_WEEK_IN_SECONDS;
        //检查是否还可以对文章进行投票,如果该文章的发布时间比截止时间小，则已过期，不能进行投票
        if(jedisUtil.zscore("time:",article) < cutoff){
            return;
        }
        //获取文章主键id
        String articleId = article.substring(article.indexOf(':') + 1);

        if(jedisUtil.sadd("voted:" + articleId,userId) == 1){
            jedisUtil.zincrby("score:info",Constants.VOTE_SCORE,article);
            jedisUtil.hincrBy(article,"votes",1L);//投票数加1
        }
    }

    @Override
    public String hget(String key, String field) {
        return jedisUtil.hget(key,field);
    }

    @Override
    public Map<String, String> hgetAll(String key) {
        return jedisUtil.hgetAll(key);
    }

    /**
     * 文章列表查询
     * @param page
     * @param 
     * @return
     */
    @Override
    public List<Map<String, String>> getArticles(int page, String key) {

        int start = (page - 1) * Constants.ARTICLES_PER_PAGE;
        int end = start + Constants.ARTICLES_PER_PAGE - 1;

        //倒序查询出投票数最高的文章，zset有序集合，分值递减
        Set<String> ids = jedisUtil.zrevrange(key,start,end);
        List<Map<String,String>> articles = new ArrayList<>();
        for(String id:ids){
            Map<String,String> articleData = jedisUtil.hgetAll(id);
            articleData.put("id",id);
            articles.add(articleData);
        }
        return articles;
    }
}
