package com.mashang.bac.web.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 查询转换服务
 */
@Service
@Slf4j
public class QueryTransformService {

    private final ChatModel chatModel;

    public QueryTransformService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * 改写用户提示词方法
     *
     * @param message 原始消息
     * @return 改写后的消息
     */
    public String rewriteQuery(String message) {
        // 创建 QueryTransformer
        QueryTransformer queryTransformer = RewriteQueryTransformer.builder()
                .chatClientBuilder(ChatClient.builder(chatModel))
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
     * @return 压缩后的查询
     */
    public String compressQuery() {
        // 当历史对话记录里面用户提示词是UserMessage的时候就不和用户继续废话了，直接固定写死回答
        Query query = Query.builder()
                .text("编程导航有啥内容？")
                .history(new UserMessage("谁是程序员鱼皮？"),
                        new AssistantMessage("编程导航的创始人 codefather.cn"))
                .build();

        QueryTransformer queryTransformer = CompressionQueryTransformer.builder()
                .chatClientBuilder(ChatClient.builder(chatModel))
                .build();

        Query transformedQuery = queryTransformer.transform(query);
        String rewrittenMessage = transformedQuery.text();
        return rewrittenMessage;
    }

    /**
     * 多查询扩展
     *
     * @param message 原始消息
     * @return 扩展后的查询列表
     */
    public List<Query> expandQuery(String message) {
        // 其实就是如果当前提示词查不到就改用户提示词，好听些就是换一种方式查,换关键词
        MultiQueryExpander queryExpander = MultiQueryExpander.builder()
                .chatClientBuilder(ChatClient.builder(chatModel))
                // 改写几次？
                .numberOfQueries(3)
                .build();
        List<Query> queries = queryExpander.expand(new Query(message));
        // 结果
        log.info("扩展查询结果: {}", queries);
        return queries;
    }
}
