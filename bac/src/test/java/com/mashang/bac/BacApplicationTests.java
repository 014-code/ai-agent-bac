package com.mashang.bac;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.mashang.bac.web.app.LoveApp;
import com.mashang.bac.web.utils.QwenImage;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SpringBootTest
class BacApplicationTests {

    @Resource
    private LoveApp loveApp;

    @Resource
    private QwenImage qwenImage;

    @Resource
    private VectorStore pgVectorVectorStore;

    /**
     * 多轮对话测试
     */
    @Test
    void contextLoads() {
        //生成随机会话id
        String chatId = UUID.randomUUID().toString();
        String message = "你好，我是程序员加炜";
        String answer = loveApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);

        message = "我想让另一半（大勾吧）更爱我";
        answer = loveApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);

        message = "我的另一半叫什么来着？刚跟你说过，帮我回忆一下";
        answer = loveApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void doChatWithReport() {
        String chatId = UUID.randomUUID().toString();

        String message = "你好，我是程序员加炜，我想要偷拍电影";
        LoveApp.LoveReport loveReport = loveApp.doChatWithReport(message, chatId);
        Assertions.assertNotNull(loveReport);
    }

    @Test
    void textImage() throws NoApiKeyException, UploadFileException, IOException {
        qwenImage.callAndReturn("生成一个大厂程序员");
    }

    @Test
    void doChatWithRag() {
        String chatId = UUID.randomUUID().toString();
        String message = "我还没结婚，但是怕婚后关系不太亲密，怎么办？，此外我想找个心意之人，我是男的20岁";
        String answer = loveApp.doChatWithRag(message, chatId);
        Assertions.assertNotNull(answer);
    }

    /**
     * 通过云存储的rag进行问答
     */
    @Test
    void doChatYunRag() {
        List<Document> documents = loveApp.doChatYunRag("谁是加炜");
        Assertions.assertNotNull(documents);
        System.out.println("云RAG查询结果数量: " + documents.size());
        
        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            System.out.println("文档 " + (i + 1) + ": " + doc.getText());
            System.out.println("元数据: " + doc.getMetadata());
        }
    }

    /**
     * 云RAG对话测试 - 结合查询结果和AI回答
     */
    @Test
    void doChatWithYunRag() {
        String chatId = UUID.randomUUID().toString();
        String message = "谁是加炜？";
        String answer = loveApp.doChatWithYunRag(message, chatId);
        Assertions.assertNotNull(answer);
        System.out.println("云RAG对话回答: " + answer);
    }

    @Test
    void test() {
        //模拟md文档数据
        List<Document> documents = List.of(
                new Document("Spring AI rocks!! Spring AI rocks!! Spring AI rocks!! Spring AI rocks!! Spring AI rocks!!", Map.of("meta1", "meta1")),
                new Document("The World is Big and Salvation Lurks Around the Corner"),
                new Document("You walk forward facing the past and you turn back toward the future.", Map.of("meta2", "meta2")));
        //存入向量数据库
        pgVectorVectorStore.add(documents);
        //查询请求对象构造查询-Spring关键词 + 得分最高的前五个
        List<Document> results = pgVectorVectorStore.similaritySearch(SearchRequest.builder().query("Spring").topK(5).build());
        Assertions.assertNotNull(results);
    }

    /**
     * 测试智能状态识别检索器
     */
    @Test
    void testSmartStatusRag() {
        String chatId = UUID.randomUUID().toString();
        
        // 测试智能状态识别 - AI会自动分析用户状态
        String message1 = "我今年25岁，单身，不知道怎么追女生";
        String answer1 = loveApp.doChatWithSmartRag(message1, chatId);
        Assertions.assertNotNull(answer1);
        System.out.println("智能状态识别结果: " + answer1);
        
        // 测试多状态组合查询
        List<String> statusList = Arrays.asList("单身", "恋爱中");
        String message2 = "我想了解恋爱技巧";
        String answer2 = loveApp.doChatWithMultiStatusRag(message2, chatId, statusList);
        Assertions.assertNotNull(answer2);
        System.out.println("多状态组合结果: " + answer2);
        
        // 测试年龄+状态组合查询
        String message3 = "我和老公结婚3年了，最近总是吵架";
        String answer3 = loveApp.doChatWithAgeStatusRag(message3, chatId, "已婚", 25, 35);
        Assertions.assertNotNull(answer3);
        System.out.println("年龄+状态组合结果: " + answer3);
    }

}
