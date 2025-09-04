package com.mashang.bac.web.app;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetriever;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetrieverOptions;
import com.mashang.bac.web.advisor.MyAdvisor;
import com.mashang.bac.web.advisor.ProhibitedWordAdvisor;
import com.mashang.bac.web.chatmemory.FileBasedChatMemory;
import com.mashang.bac.web.rag.LoveAppRagCloudAdvisorConfig;
import com.mashang.bac.web.rag.LoveAppVectorStoreConfig;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Component
@Slf4j
public class LoveApp {

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    private final ChatClient client;

    private final Resource systemResource;

    @jakarta.annotation.Resource
    private ChatModel dashscopeChatModel;

    @jakarta.annotation.Resource
    //rag对象-本地
    private VectorStore loveAppVectorStore;

    //rag对象-基于云 + 知识顾问
    @jakarta.annotation.Resource
    private Advisor loveAppRagCloudAdvisor;

    //rag对象-基于pgVector存储
    @jakarta.annotation.Resource
    private VectorStore pgVectorVectorStore;

    /**
     * 初始化恋爱大师app
     *
     * @param dashscopeChatModel 灵积大模型
     */
    public LoveApp(ChatModel dashscopeChatModel, ResourceLoader resourceLoader) {
        //对话记录持久到本地
        String fileDir = System.getProperty("user.dir") + "/chat-memory";
        //新建多轮对话对象
        ChatMemory chatMemory = new FileBasedChatMemory(fileDir);
        this.systemResource = resourceLoader.getResource("classpath:/prompts/system-message.st");
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemResource);
        //读取模板
        String render = systemPromptTemplate.render();
        //初始化聊天客户端-给到聊天客户端对象(相当于自定义ChatClient，之后调用即可)
        client = ChatClient.builder(dashscopeChatModel)
                //传入默认提示词
                .defaultSystem(render)
                //设置拦截器
                .defaultAdvisors(
                        //设置入多轮对话拦截器对象
                        new MessageChatMemoryAdvisor(chatMemory),
                        //使用自定义拦截器
                        new MyAdvisor(),
                        //违禁词拦截器
                        new ProhibitedWordAdvisor()
                )
                .build();
    }

    /**
     * ai恋爱计划报告结构化生成对象-快速定义法
     *
     * @param title
     * @param suggestions
     */
    public record LoveReport(String title, List<String> suggestions) {
    }

    /**
     * 对话方法
     *
     * @param message -输入内容
     * @param chatId  -会话id-每次新会话都会产生不同id
     * @return
     */
    public String doChat(String message, String chatId) {
        ChatResponse chatResponse = client
                .prompt()
                //用户提示词-相当于发送的消息
                .user(message)
                //拦截器
                .advisors(sp -> sp.param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                //呼叫
                .call()
                //获得响应对象
                .chatResponse();
        //获得回答内容
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
    public LoveReport doChatWithReport(String message, String chatId) {
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemResource);
        //读取模板
        String render = systemPromptTemplate.render();
        LoveReport loveReport = client
                .prompt()
                .system(render + "每次对话后都要生成恋爱结果，标题为{加炜}的恋爱报告，内容为建议列表")
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        //对话记忆10条
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .entity(LoveReport.class);
        log.info("loveReport: {}", loveReport);
        return loveReport;
    }

    /**
     * 对话-基于rag增强
     *
     * @param message
     * @param chatId
     * @return
     */
    public String doChatWithRag(String message, String chatId) {
        //改写
        String writeStr = write(message);
        ChatResponse chatResponse = client
                .prompt()
                .user(writeStr)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .advisors(new MyAdvisor())
                //请求拦截，塞入一个本地的rag知识库(vectorStore对象就是读取本地的)
                //踩坑：名称一定要和自己写的一样
                //本地知识库
//                .advisors(new QuestionAnswerAdvisor(loveAppVectorStore))
                //云知识库
//                .advisors(loveAppRagCloudAdvisor)
                //pg知识库
                .advisors(new QuestionAnswerAdvisor(pgVectorVectorStore))
                .call()
                .chatResponse();
        //当用户问题发送到 AI 模型时，Advisor 会查询向量数据库来获取与用户问题相关的文档，并将这些文档作为‌上下文附加到用户查询中
        var qaAdvisor = QuestionAnswerAdvisor.builder(pgVectorVectorStore)
                .searchRequest(SearchRequest.builder().similarityThreshold(0.8d).topK(6).build());
        //输出
        log.info("qaAdvisor: {}", qaAdvisor);
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    /**
     * 向云rag发起查询方法
     */
    public void doChatYunRag(String message) {
        var dashScopeApi = new DashScopeApi(apiKey);
        DocumentRetriever retriever = new DashScopeDocumentRetriever(dashScopeApi,
                DashScopeDocumentRetrieverOptions.builder()
                        .withIndexName("恋爱大师知识库")
                        .build());

        //向云rag发起查询
        List<Document> documentList = retriever.retrieve(new Query(message));
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

    /**
     * 查询压缩方法
     *
     * @return
     */
    public String compress() {
        //当历史对话记录里面用户提示词是UserMessage的时候就不和用户继续废话了，直接固定写死回答
        Query query = Query.builder()
                .text("编程导航有啥内容？")
                .history(new UserMessage("谁是程序员鱼皮？"),
                        new AssistantMessage("编程导航的创始人 codefather.cn"))
                .build();

        QueryTransformer queryTransformer = CompressionQueryTransformer.builder()
                .chatClientBuilder(ChatClient.builder(dashscopeChatModel))
                .build();

        Query transformedQuery = queryTransformer.transform(query);
        String rewrittenMessage = transformedQuery.text();
        return rewrittenMessage;
    }

    /**
     * 多查询扩展
     *
     * @return
     */
    public void searchS() {
        //其实就是如果当前提示词查不到就改用户提示词，好听些就是换一种方式查,换关键词
        MultiQueryExpander queryExpander = MultiQueryExpander.builder()
                .chatClientBuilder(ChatClient.builder(dashscopeChatModel))
                //改写几次？
                .numberOfQueries(3)
                .build();
        List<Query> queries = queryExpander.expand(new Query("啥是程序员鱼皮？他会啥？"));
        //结果
        log.info("ru: {}", queries);
    }

    public void docSearch() {
        DocumentRetriever retriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(pgVectorVectorStore)       // 在哪个图书馆找
                .similarityThreshold(0.7)      // 相似度要求：至少要70%相关才给我
                .topK(5)                       // 最多给我5本书
                .filterExpression(              // 额外筛选条件：
                        new FilterExpressionBuilder()
                                .eq("type", "web")         // 只要"类型是web"的书
                                .build())
                .build();
        // 就像说："帮我找关于'程序员鱼皮'的书"
        List<Document> documents = retriever.retrieve(new Query("谁是程序员鱼皮"));

//        // 就像这次特意嘱咐："找关于鱼皮的书，但只要类型是'boy'的"
//        Query query = Query.builder()
//                .text("谁是鱼皮？")  // 核心问题
//                .context(Map.of(
//                        VectorStoreDocumentRetriever.FILTER_EXPRESSION,
//                        "type == 'boy'"  // 临时筛选条件：只要男孩类型的
//                ))
//                .build();

//        // 为用户A检索时，只给他看适合他年龄的内容
//        Query query = Query.builder()
//                .text("如何维持长期关系")
//                .context(Map.of(
//                        VectorStoreDocumentRetriever.FILTER_EXPRESSION,
//                        "min_age <= 25 && max_age >= 25"  // 适合25岁的内容
//                ))
//                .build();
    }

    /**
     * 空上下文处理
     */
    public void nullText() {
//默认情况下，RetrievalAugmentationAdvisor 不允许检索的上下文为空。当没有找到相关文档时，
// 它会指示模型不要回答用户查询。这是一种保守的策略，可以防止模型在没有足够信息的情况下生成不准确的回答。
//但在某些场景下，我们可能希望即使在没有相关文档的情况下也能为用户提供回答，比如即使没有特定知识库支持也能回答的通用问题。
// 可以通过配置 ContextualQueryAugmenter 上下文查询增强器来实现。
        Advisor retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .similarityThreshold(0.50)
                        .vectorStore(pgVectorVectorStore)
                        .build())
                .queryAugmenter(ContextualQueryAugmenter.builder()
                        .allowEmptyContext(true)
                        .build())
                .build();

//        为了提供更友好的错误处理机制，ContextualQueryAugmenter允许我们自定义提示模板，包括正常情况下使用的提示模板和上下文为空时使用的提示模板：
//        QueryAugmenter queryAugmenter = ContextualQueryAugmenter.builder()
//                .promptTemplate(customPromptTemplate)
//                .emptyContextPromptTemplate(emptyContextPromptTemplate)
//                .build();

    }

}
