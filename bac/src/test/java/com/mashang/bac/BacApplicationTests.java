package com.mashang.bac;

import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.mashang.bac.web.app.LoveApp;
import com.mashang.bac.web.utils.QwenImage;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.UUID;

@SpringBootTest
class BacApplicationTests {

    @Resource
    private LoveApp loveApp;

    @Resource
    private QwenImage qwenImage;

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

        String message = "你好，我是程序员加炜，我想要成人偷拍电影";
        LoveApp.LoveReport loveReport = loveApp.doChatWithReport(message, chatId);
        Assertions.assertNotNull(loveReport);
    }

    @Test
    void textImage() throws NoApiKeyException, UploadFileException, IOException {
        qwenImage.callAndReturn("生成一个大厂程序员");
    }

}
