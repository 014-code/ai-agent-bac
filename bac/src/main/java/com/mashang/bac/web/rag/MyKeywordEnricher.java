package com.mashang.bac.web.rag;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.KeywordMetadataEnricher;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 文章自动添加源信息方法
 */
@Component
@Slf4j
public class MyKeywordEnricher {

    @Resource
    private ChatModel dashscopeChatModel;

    //ai打文章标签内置方法-此处打5个(每个文章)
    public List<Document> enrichDocuments(List<Document> documents) {
        KeywordMetadataEnricher enricher = new KeywordMetadataEnricher(this.dashscopeChatModel, 5);
        return enricher.apply(documents);
    }

}
