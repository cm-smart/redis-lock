import com.chen.cache.ApplicationRedisArticle;
import com.chen.cache.service.RedisArticleService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ApplicationRedisArticle.class)
public class TestRedisArticle {

    @Resource
    private RedisArticleService redisArticleService;

    /**
     * 发布文章
     */
    @Test
    public void postArticle(){
        String userId = "001";//用户id，001
        String title = "The road to west";
        String content = "About body and mental health";
        String link = "www.xx.com";
        //发布文章，返回文章id
        String articleId = redisArticleService.postArticle(title,content,link,userId);

        System.out.println("刚才发布了一篇文章，文章id为：" + articleId);
        System.out.println("文章所有属性值内容如下：");
        Map<String,String> articleData = redisArticleService.hgetAll("article:"+articleId);
        for(Map.Entry<String,String> entry:articleData.entrySet()){
            System.out.println(entry.getKey() + ":"+entry.getValue());
        }
    }
}
