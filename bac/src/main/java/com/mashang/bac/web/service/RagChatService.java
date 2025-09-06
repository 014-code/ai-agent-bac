package com.mashang.bac.web.service;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetriever;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetrieverOptions;
import com.mashang.bac.web.advisor.MyAdvisor;
import com.mashang.bac.web.chatmemory.InMemoryChatMemory;
import com.mashang.bac.web.rag.factory.LoveAppRagCustomAdvisorFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

/**
 * RAG对话服务
 */
@Service
@Slf4j
public class RagChatService {

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    private final ChatClient client;
    private final Resource systemResource;
    private final ChatModel dashscopeChatModel;
    private final VectorStore loveAppVectorStore;
    private final VectorStore pgVectorVectorStore;

    public RagChatService(ChatModel dashscopeChatModel, ResourceLoader resourceLoader, 
                         VectorStore loveAppVectorStore, VectorStore pgVectorVectorStore) {
        this.dashscopeChatModel = dashscopeChatModel;
        this.loveAppVectorStore = loveAppVectorStore;
        this.pgVectorVectorStore = pgVectorVectorStore;
        
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
                        new MyAdvisor()
                )
                .build();
    }

    /**
     * 对话-基于rag增强
     *
     * @param message
     * @param chatId
     * @return
     */
    public String doChatWithRag(String message, String chatId) {
        // 改写
        String writeStr = write(message);
        ChatResponse chatResponse = client
                .prompt()
                .user(writeStr)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .advisors(new MyAdvisor())
                // 请求拦截，塞入一个本地的rag知识库(vectorStore对象就是读取本地的)
                // 踩坑：名称一定要和自己写的一样
                // 本地知识库
//                .advisors(new QuestionAnswerAdvisor(loveAppVectorStore))
                // 云知识库
//                .advisors(loveAppRagCloudAdvisor)
                // pg知识库
                .advisors(new QuestionAnswerAdvisor(pgVectorVectorStore))
                .advisors(
                        // 智能文档检索器对象-也是拦截器
                        LoveAppRagCustomAdvisorFactory.createLoveAppRagCustomAdvisor(
                                loveAppVectorStore, "已婚"
                        )
                )
                .call()
                .chatResponse();
        // 当用户问题发送到 AI 模型时，Advisor 会查询向量数据库来获取与用户问题相关的文档，并将这些文档作为上下文附加到用户查询中
        var qaAdvisor = QuestionAnswerAdvisor.builder(pgVectorVectorStore)
                .searchRequest(SearchRequest.builder().similarityThreshold(0.8d).topK(6).build());
        // 输出
        log.info("qaAdvisor: {}", qaAdvisor);
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    /**
     * 智能状态识别对话 - AI自动分析用户状态并筛选相关文档
     */
    public String doChatWithSmartRag(String message, String chatId) {
        String writeStr = write(message);
        ChatResponse chatResponse = client
                .prompt()
                .user(writeStr)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .advisors(new MyAdvisor())
                // 智能状态识别检索器 - AI自动分析用户状态
                .advisors(LoveAppRagCustomAdvisorFactory.createSmartStatusRagAdvisor(
                        loveAppVectorStore, dashscopeChatModel))
                .call()
                .chatResponse();

        String content = chatResponse.getResult().getOutput().getText();
        log.info("智能状态识别对话结果: {}", content);
        return content;
    }

    /**
     * 多状态组合对话 - 支持多个恋爱状态同时查询
     */
    public String doChatWithMultiStatusRag(String message, String chatId, List<String> statusList) {
        String writeStr = write(message);
        ChatResponse chatResponse = client
                .prompt()
                .user(writeStr)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .advisors(new MyAdvisor())
                // 多状态组合检索器
                .advisors(LoveAppRagCustomAdvisorFactory.createMultiStatusRagAdvisor(
                        loveAppVectorStore, statusList))
                .call()
                .chatResponse();

        String content = chatResponse.getResult().getOutput().getText();
        log.info("多状态组合对话结果: {}", content);
        return content;
    }

    /**
     * 年龄+状态组合对话 - 根据年龄和状态精确筛选
     */
    public String doChatWithAgeStatusRag(String message, String chatId, String status, int minAge, int maxAge) {
        String writeStr = write(message);
        ChatResponse chatResponse = client
                .prompt()
                .user(writeStr)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .advisors(new MyAdvisor())
                // 年龄+状态组合检索器
                .advisors(LoveAppRagCustomAdvisorFactory.createAgeStatusRagAdvisor(
                        loveAppVectorStore, status, minAge, maxAge))
                .call()
                .chatResponse();

        String content = chatResponse.getResult().getOutput().getText();
        log.info("年龄+状态组合对话结果: {}", content);
        return content;
    }

    /**
     * 向云rag发起查询方法
     */
    public List<Document> doChatYunRag(String message) {
        var dashScopeApi = new DashScopeApi(apiKey);
        DocumentRetriever retriever = new DashScopeDocumentRetriever(dashScopeApi,
                DashScopeDocumentRetrieverOptions.builder()
                        .withIndexName("恋爱大师知识库")
                        .build());

        // 向云rag发起查询
        List<Document> documentList = retriever.retrieve(new Query(message));
        log.info("云RAG查询到 {} 条文档", documentList.size());
        
        // 打印查询结果
        for (int i = 0; i < documentList.size(); i++) {
            Document doc = documentList.get(i);
            log.info("文档 {}: {}", i + 1, doc.getText());
            log.info("元数据: {}", doc.getMetadata());
        }
        
        return documentList;
    }

    /**
     * 云RAG对话方法 - 结合查询结果和AI回答
     */
    public String doChatWithYunRag(String message, String chatId) {
        // 先查询云RAG获取相关文档
        List<Document> documents = doChatYunRag(message);
        
        // 构建包含文档内容的提示词
        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("基于以下知识库信息回答问题：\n\n");
        
        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            contextBuilder.append("知识 ").append(i + 1).append(": ").append(doc.getText()).append("\n\n");
        }
        
        contextBuilder.append("问题：").append(message);
        
        // 使用查询到的文档进行对话
        ChatResponse chatResponse = client
                .prompt()
                .user(contextBuilder.toString())
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .advisors(new MyAdvisor())
                .call()
                .chatResponse();
        
        String content = chatResponse.getResult().getOutput().getText();
        log.info("云RAG对话结果: {}", content);
        return content;
    }

    /**
     * 改写用户提示词方法
     *
     * @return
     */
    public String write(String message) {
        // 创建 QueryTransformer
        QueryTransformer queryTransformer = RewriteQueryTransformer.builder()
                .chatClientBuilder(ChatClient.builder(dashscopeChatModel))
                .build();

        // 改写用户查询
        Query originalQuery = new Query(message);
        Query transformedQuery = queryTransformer.transform(originalQuery);
        String rewrittenMessage = transformedQuery.text();
        return rewrittenMessage;
    }
}
