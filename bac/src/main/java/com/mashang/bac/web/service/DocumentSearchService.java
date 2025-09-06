package com.mashang.bac.web.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 文档检索服务
 */
@Service
@Slf4j
public class DocumentSearchService {

    private final VectorStore pgVectorVectorStore;

    public DocumentSearchService(VectorStore pgVectorVectorStore) {
        this.pgVectorVectorStore = pgVectorVectorStore;
    }

    /**
     * 基础文档搜索
     *
     * @param query 查询文本
     * @return 搜索结果
     */
    public List<Document> searchDocuments(String query) {
        // 查询请求对象构造查询-Spring关键词 + 得分最高的前五个
        List<Document> results = pgVectorVectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(5)
                        .build()
        );
        log.info("搜索到 {} 条文档", results.size());
        return results;
    }

    /**
     * 带相似度阈值的文档搜索
     *
     * @param query 查询文本
     * @param similarityThreshold 相似度阈值
     * @param topK 返回数量
     * @return 搜索结果
     */
    public List<Document> searchDocumentsWithThreshold(String query, double similarityThreshold, int topK) {
        List<Document> results = pgVectorVectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .similarityThreshold(similarityThreshold)
                        .topK(topK)
                        .build()
        );
        log.info("搜索到 {} 条文档，相似度阈值: {}", results.size(), similarityThreshold);
        return results;
    }

    /**
     * 带过滤条件的文档搜索
     *
     * @param query 查询文本
     * @param filterExpression 过滤表达式
     * @return 搜索结果
     */
    public List<Document> searchDocumentsWithFilter(String query, String filterExpression) {
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
        List<Document> documents = retriever.retrieve(new Query(query));
        log.info("带过滤条件搜索到 {} 条文档", documents.size());
        return documents;
    }

    /**
     * 年龄范围过滤搜索
     *
     * @param query 查询文本
     * @param minAge 最小年龄
     * @param maxAge 最大年龄
     * @return 搜索结果
     */
    public List<Document> searchDocumentsByAgeRange(String query, int minAge, int maxAge) {
        DocumentRetriever retriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(pgVectorVectorStore)
                .similarityThreshold(0.7)
                .topK(5)
                .filterExpression(
                        new FilterExpressionBuilder()
                                .gte("min_age", minAge)
                                .build())
                .build();

        List<Document> documents = retriever.retrieve(new Query(query));
        log.info("年龄范围 {}-{} 搜索到 {} 条文档", minAge, maxAge, documents.size());
        return documents;
    }

    /**
     * 状态过滤搜索
     *
     * @param query 查询文本
     * @param status 恋爱状态
     * @return 搜索结果
     */
    public List<Document> searchDocumentsByStatus(String query, String status) {
        DocumentRetriever retriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(pgVectorVectorStore)
                .similarityThreshold(0.7)
                .topK(5)
                .filterExpression(
                        new FilterExpressionBuilder()
                                .eq("status", status)
                                .build())
                .build();

        List<Document> documents = retriever.retrieve(new Query(query));
        log.info("状态 {} 搜索到 {} 条文档", status, documents.size());
        return documents;
    }

    /**
     * 复合条件搜索
     *
     * @param query 查询文本
     * @param status 恋爱状态
     * @param minAge 最小年龄
     * @param maxAge 最大年龄
     * @param type 文档类型
     * @return 搜索结果
     */
    public List<Document> searchDocumentsWithMultipleFilters(String query, String status, 
                                                           int minAge, int maxAge, String type) {
        DocumentRetriever retriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(pgVectorVectorStore)
                .similarityThreshold(0.7)
                .topK(5)
                .filterExpression(
                        new FilterExpressionBuilder()
                                .eq("status", status)
                                .build())
                .build();

        List<Document> documents = retriever.retrieve(new Query(query));
        log.info("复合条件搜索到 {} 条文档", documents.size());
        return documents;
    }

    /**
     * 创建检索增强顾问
     *
     * @param similarityThreshold 相似度阈值
     * @param topK 返回数量
     * @return RetrievalAugmentationAdvisor
     */
    public RetrievalAugmentationAdvisor createRetrievalAdvisor(double similarityThreshold, int topK) {
        DocumentRetriever retriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(pgVectorVectorStore)
                .similarityThreshold(similarityThreshold)
                .topK(topK)
                .build();

        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(retriever)
                .build();
    }

    /**
     * 添加文档到向量库
     *
     * @param documents 文档列表
     */
    public void addDocuments(List<Document> documents) {
        pgVectorVectorStore.add(documents);
        log.info("成功添加 {} 条文档到向量库", documents.size());
    }

    /**
     * 删除文档
     *
     * @param documentIds 文档ID列表
     */
    public void deleteDocuments(List<String> documentIds) {
        pgVectorVectorStore.delete(documentIds);
        log.info("成功删除 {} 条文档", documentIds.size());
    }
}
