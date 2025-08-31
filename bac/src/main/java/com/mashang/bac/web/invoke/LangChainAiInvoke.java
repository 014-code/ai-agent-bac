package com.mashang.bac.web.invoke;

import com.mashang.bac.web.common.TestApiKey;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class LangChainAiInvoke implements CommandLineRunner {
    @Override
    public void run(String... args) throws Exception {
        ChatLanguageModel qwenModel = QwenChatModel.builder()
                .apiKey(TestApiKey.API_KEY)
                .modelName("qwen-max")
                .build();
        String answer = qwenModel.chat("我是哇哈哈哈");
        System.out.println(answer);
    }
}
