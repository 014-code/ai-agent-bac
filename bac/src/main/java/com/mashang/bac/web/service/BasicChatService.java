package com.mashang.bac.web.service;

import com.mashang.bac.web.app.LoveApp;
import com.mashang.bac.web.advisor.MyAdvisor;
import com.mashang.bac.web.advisor.ProhibitedWordAdvisor;
import com.mashang.bac.web.chatmemory.InMemoryChatMemory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

/**
 * 基础对话服务
 */
@Service
@Slf4j
public class BasicChatService {

    private final ChatClient client;
    private final Resource systemResource;

    public BasicChatService(ChatModel dashscopeChatModel, ResourceLoader resourceLoader) {
        // 使用内存存储，避免序列化问题
        ChatMemory chatMemory = new InMemoryChatMemory();
        this.systemResource = resourceLoader.getResource("classpath:/prompts/system-message.st");
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemResource);
        // 读取模板
        String render = systemPromptTemplate.render();
        // 初始化聊天客户端
        client = ChatClient.builder(dashscopeChatModel)
                // 传入默认提示词
                .defaultSystem(render)
                // 设置拦截器
                .defaultAdvisors(
                        // 设置入多轮对话拦截器对象
                        new MessageChatMemoryAdvisor(chatMemory),
                        // 使用自定义拦截器
                        new MyAdvisor(),
                        // 违禁词拦截器
                        new ProhibitedWordAdvisor()
                )
                .build();
    }

    /**
     * 基础对话方法
     *
     * @param message - 输入内容
     * @param chatId  - 会话id-每次新会话都会产生不同id
     * @return
     */
    public String doChat(String message, String chatId) {
        ChatResponse chatResponse = client
                .prompt()
                // 用户提示词-相当于发送的消息
                .user(message)
                // 拦截器
                .advisors(sp -> sp.param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                // 呼叫
                .call()
                // 获得响应对象
                .chatResponse();
        // 获得回答内容
        String text = chatResponse.getResult().getOutput().getText();
        System.out.println(text);
        return text;
    }

    /**
     * ai生成恋爱报告方法
     *
     * @param message
     * @param chatId
     * @return
     */
    public LoveApp.LoveReport doChatWithReport(String message, String chatId) {
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemResource);
        // 读取模板
        String render = systemPromptTemplate.render();
        LoveApp.LoveReport loveReport = client
                .prompt()
                .system(render + "每次对话后都要生成恋爱结果，标题为{加炜}的恋爱报告，内容为建议列表")
                .user(message)
                .advisors(spec -> spec.param("CHAT_MEMORY_CONVERSATION_ID_KEY", chatId)
                        // 对话记忆10条
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .entity(LoveApp.LoveReport.class);
        log.info("loveReport: {}", loveReport);
        return loveReport;
    }
}
