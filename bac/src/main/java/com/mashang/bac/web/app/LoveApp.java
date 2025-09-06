package com.mashang.bac.web.app;

import com.mashang.bac.web.service.BasicChatService;
import com.mashang.bac.web.service.DocumentSearchService;
import com.mashang.bac.web.service.QueryTransformService;
import com.mashang.bac.web.service.RagChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 恋爱大师应用 - 重构后的简化版本
 */
@Component
@Slf4j
public class LoveApp {

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    // 注入各个服务
    private final BasicChatService basicChatService;
    private final RagChatService ragChatService;
    private final QueryTransformService queryTransformService;
    private final DocumentSearchService documentSearchService;

    // 向量存储
    @jakarta.annotation.Resource
    private VectorStore pgVectorVectorStore;

    /**
     * 初始化恋爱大师app
     */
    public LoveApp(BasicChatService basicChatService, 
                   RagChatService ragChatService,
                   QueryTransformService queryTransformService,
                   DocumentSearchService documentSearchService) {
        this.basicChatService = basicChatService;
        this.ragChatService = ragChatService;
        this.queryTransformService = queryTransformService;
        this.documentSearchService = documentSearchService;
    }

    /**
     * ai恋爱计划报告结构化生成对象-快速定义法
     */
    public record LoveReport(String title, List<String> suggestions) {
    }

    // ==================== 基础对话功能 ====================

    /**
     * 基础对话方法
     */
    public String doChat(String message, String chatId) {
        return basicChatService.doChat(message, chatId);
    }

    /**
     * ai生成恋爱报告方法
     */
    public LoveReport doChatWithReport(String message, String chatId) {
        return basicChatService.doChatWithReport(message, chatId);
    }

    // ==================== RAG对话功能 ====================

    /**
     * 对话-基于rag增强
     */
    public String doChatWithRag(String message, String chatId) {
        return ragChatService.doChatWithRag(message, chatId);
    }

    /**
     * 智能状态识别对话 - AI自动分析用户状态并筛选相关文档
     */
    public String doChatWithSmartRag(String message, String chatId) {
        return ragChatService.doChatWithSmartRag(message, chatId);
    }

    /**
     * 多状态组合对话 - 支持多个恋爱状态同时查询
     */
    public String doChatWithMultiStatusRag(String message, String chatId, List<String> statusList) {
        return ragChatService.doChatWithMultiStatusRag(message, chatId, statusList);
    }

    /**
     * 年龄+状态组合对话 - 根据年龄和状态精确筛选
     */
    public String doChatWithAgeStatusRag(String message, String chatId, String status, int minAge, int maxAge) {
        return ragChatService.doChatWithAgeStatusRag(message, chatId, status, minAge, maxAge);
    }

    /**
     * 向云rag发起查询方法
     */
    public List<Document> doChatYunRag(String message) {
        return ragChatService.doChatYunRag(message);
    }

    /**
     * 云RAG对话方法 - 结合查询结果和AI回答
     */
    public String doChatWithYunRag(String message, String chatId) {
        return ragChatService.doChatWithYunRag(message, chatId);
    }

    // ==================== 查询转换功能 ====================

    /**
     * 改写用户提示词方法
     */
    public String write(String message) {
        return queryTransformService.rewriteQuery(message);
    }

    /**
     * 查询压缩方法
     */
    public String compress() {
        return queryTransformService.compressQuery();
    }

    /**
     * 多查询扩展
     */
    public void moreSearch(String message) {
        queryTransformService.expandQuery(message);
    }

    // ==================== 文档检索功能 ====================

    /**
     * 基础文档搜索
     */
    public List<Document> searchDocuments(String query) {
        return documentSearchService.searchDocuments(query);
    }

    /**
     * 带相似度阈值的文档搜索
     */
    public List<Document> searchDocumentsWithThreshold(String query, double similarityThreshold, int topK) {
        return documentSearchService.searchDocumentsWithThreshold(query, similarityThreshold, topK);
    }

    /**
     * 带过滤条件的文档搜索
     */
    public List<Document> searchDocumentsWithFilter(String query, String filterExpression) {
        return documentSearchService.searchDocumentsWithFilter(query, filterExpression);
    }

    /**
     * 年龄范围过滤搜索
     */
    public List<Document> searchDocumentsByAgeRange(String query, int minAge, int maxAge) {
        return documentSearchService.searchDocumentsByAgeRange(query, minAge, maxAge);
    }

    /**
     * 状态过滤搜索
     */
    public List<Document> searchDocumentsByStatus(String query, String status) {
        return documentSearchService.searchDocumentsByStatus(query, status);
    }

    /**
     * 复合条件搜索
     */
    public List<Document> searchDocumentsWithMultipleFilters(String query, String status, 
                                                           int minAge, int maxAge, String type) {
        return documentSearchService.searchDocumentsWithMultipleFilters(query, status, minAge, maxAge, type);
    }

    /**
     * 添加文档到向量库
     */
    public void addDocuments(List<Document> documents) {
        documentSearchService.addDocuments(documents);
    }

    /**
     * 删除文档
     */
    public void deleteDocuments(List<String> documentIds) {
        documentSearchService.deleteDocuments(documentIds);
    }

    // ==================== 测试和演示功能 ====================

    /**
     * 测试文档搜索功能
     */
    public void testDocumentSearch() {
        // 模拟md文档数据
        List<Document> documents = List.of(
                new Document("Spring AI rocks!! Spring AI rocks!! Spring AI rocks!! Spring AI rocks!! Spring AI rocks!!", Map.of("meta1", "meta1")),
                new Document("The World is Big and Salvation Lurks Around the Corner"),
                new Document("You walk forward facing the past and you turn back toward the future.", Map.of("meta2", "meta2"))
        );
        
        // 存入向量数据库
        addDocuments(documents);
        
        // 查询请求对象构造查询-Spring关键词 + 得分最高的前五个
        List<Document> results = searchDocuments("Spring");
        log.info("搜索结果: {}", results);
    }

    /**
     * 空上下文处理演示
     */
    public void nullText() {
        // 默认情况下，RetrievalAugmentationAdvisor 不允许检索的上下文为空。当没有找到相关文档时，
        // 它会指示模型不要回答用户查询。这是一种保守的策略，可以防止模型在没有足够信息的情况下生成不准确的回答。
        // 但在某些场景下，我们可能希望即使在没有相关文档的情况下也能为用户提供回答，比如即使没有特定知识库支持也能回答的通用问题。
        // 可以通过配置 ContextualQueryAugmenter 上下文查询增强器来实现。
        
        // 这里可以添加具体的实现逻辑
        log.info("空上下文处理演示");
    }
}
