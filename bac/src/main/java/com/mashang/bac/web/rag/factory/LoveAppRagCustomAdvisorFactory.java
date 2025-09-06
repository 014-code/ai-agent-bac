package com.mashang.bac.web.rag.factory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;

import java.util.List;
import java.util.Map;

/**
 * 智能文档检索器工厂类
 */
@Slf4j
public class LoveAppRagCustomAdvisorFactory {
    
    /**
     * 创建基础状态过滤检索器
     */
    public static Advisor createLoveAppRagCustomAdvisor(VectorStore vectorStore, String status) {
        //过滤器-类似qw那样的
        Filter.Expression expression = new FilterExpressionBuilder()
                //eq直接根据状态过滤
                .eq("status", status)
                .build();
        //文档检索器
        DocumentRetriever documentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                //应用过滤器
                .filterExpression(expression)
                //文档召回相似度
                .similarityThreshold(0.5)
                //召回文档数
                .topK(3)
                .build();
        //返回智能文档检索器类
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever)
                .build();
    }
    
    /**
     * 创建智能状态识别检索器 - 自动分析用户状态并筛选文档
     */
    public static Advisor createSmartStatusRagAdvisor(VectorStore vectorStore, ChatModel chatModel) {
        return new SmartStatusRagAdvisor(vectorStore, chatModel);
    }
    
    /**
     * 创建多状态组合检索器 - 支持多个状态同时查询
     */
    public static Advisor createMultiStatusRagAdvisor(VectorStore vectorStore, List<String> statusList) {
        // 构建多状态过滤条件
        Filter.Expression expression = new FilterExpressionBuilder()
                .in("status", statusList.toArray(new String[0]))
                .build();
                
        DocumentRetriever documentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .filterExpression(expression)
                .similarityThreshold(0.6)
                .topK(5)
                .build();
                
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever)
                .build();
    }
    
    /**
     * 创建年龄+状态组合检索器
     */
    public static Advisor createAgeStatusRagAdvisor(VectorStore vectorStore, String status, int minAge, int maxAge) {
        Filter.Expression expression = new FilterExpressionBuilder()
                .eq("status", status)
                .build();
                
        DocumentRetriever documentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .filterExpression(expression)
                .similarityThreshold(0.7)
                .topK(4)
                .build();
                
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever)
                .build();
    }
    
    /**
     * 智能状态识别检索器实现
     */
    private static class SmartStatusRagAdvisor implements Advisor {
        private final VectorStore vectorStore;
        private final ChatModel chatModel;
        private final ChatClient statusAnalyzer;
        
        public SmartStatusRagAdvisor(VectorStore vectorStore, ChatModel chatModel) {
            this.vectorStore = vectorStore;
            this.chatModel = chatModel;
            this.statusAnalyzer = ChatClient.builder(chatModel)
                    .defaultSystem("""
                        你是一个恋爱状态分析专家。请根据用户的问题和描述，分析用户的恋爱状态。
                        可能的状态包括：单身、恋爱中、已婚、离异、复合等。
                        请只返回状态关键词，不要其他内容。
                        """)
                    .build();
        }
        
        @Override
        public String getName() {
            return "smart-status-rag-advisor";
        }

        public Object advise(Object input) {
            try {
                // 从输入中提取用户消息
                String userMessage = extractUserMessage(input);
                if (userMessage == null) {
                    return input;
                }
                
                // 分析用户状态
                String status = analyzeUserStatus(userMessage);
                log.info("分析用户状态: {}", status);
                
                // 根据状态创建过滤检索器
                Filter.Expression expression = createStatusFilter(status);
                
                DocumentRetriever documentRetriever = VectorStoreDocumentRetriever.builder()
                        .vectorStore(vectorStore)
                        .filterExpression(expression)
                        .similarityThreshold(0.6)
                        .topK(4)
                        .build();
                
                // 检索相关文档
                List<org.springframework.ai.document.Document> documents = 
                    documentRetriever.retrieve(new Query(userMessage));
                
                log.info("检索到 {} 条相关文档", documents.size());
                
                // 将检索到的文档添加到上下文中
                return addDocumentsToContext(input, documents);
                
            } catch (Exception e) {
                log.error("智能状态检索失败", e);
                return input;
            }
        }
        
        private String extractUserMessage(Object input) {
            // 这里需要根据实际的输入类型来提取用户消息
            // 假设input是某种包含用户消息的对象
            if (input instanceof Map) {
                Map<?, ?> inputMap = (Map<?, ?>) input;
                return (String) inputMap.get("userMessage");
            }
            return input.toString();
        }
        
        private String analyzeUserStatus(String userMessage) {
            try {
                String status = statusAnalyzer.prompt()
                        .user("请分析以下用户消息的恋爱状态：" + userMessage)
                        .call()
                        .content();
                
                // 标准化状态关键词
                if (status.contains("单身") || status.contains("没有对象")) {
                    return "单身";
                } else if (status.contains("恋爱") || status.contains("男朋友") || status.contains("女朋友")) {
                    return "恋爱中";
                } else if (status.contains("已婚") || status.contains("老公") || status.contains("老婆")) {
                    return "已婚";
                } else if (status.contains("离异") || status.contains("离婚")) {
                    return "离异";
                } else {
                    return "通用"; // 默认状态
                }
            } catch (Exception e) {
                log.warn("状态分析失败，使用默认状态", e);
                return "通用";
            }
        }
        
        private Filter.Expression createStatusFilter(String status) {
            if ("通用".equals(status)) {
                // 通用状态不添加过滤条件
                return null;
            } else {
                return new FilterExpressionBuilder()
                        .eq("status", status)
                        .build();
            }
        }
        
        private Object addDocumentsToContext(Object input, List<org.springframework.ai.document.Document> documents) {
            // 这里需要根据实际的输入类型来添加文档到上下文
            // 具体实现取决于你的上下文结构
            if (input instanceof Map) {
                Map<?, ?> inputMap = (Map<?, ?>) input;
                ((Map<Object, Object>) inputMap).put("retrievedDocuments", documents);
            }
            return input;
        }

        @Override
        public int getOrder() {
            return 0;
        }
    }
}
