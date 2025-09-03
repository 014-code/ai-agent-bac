package com.mashang.bac.web.rag;

import com.mashang.bac.web.enums.ErrorCode;
import com.mashang.bac.web.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 恋爱助手rag
 */
@Component
@Slf4j
public class LoveAppDocumentLoader {

    private final ResourcePatternResolver resourcePatternResolver;

    LoveAppDocumentLoader(ResourcePatternResolver resourcePatternResolver) {
        this.resourcePatternResolver = resourcePatternResolver;
    }

    /**
     * 加载md文档方法
     *
     * @return
     */
    public List<Document> loadMarkdowns() {
        //用来存放md转成Document后的list
        List<Document> allDocuments = new ArrayList<>();
        try {
            //springai读取md的方法
            Resource[] resources = resourcePatternResolver.getResources("classpath:document/*.md");
            //依次读取
            for (Resource resource : resources) {
                //获取名称
                String fileName = resource.getFilename();
                //初始化mdconfig配置
                MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                        .withHorizontalRuleCreateDocument(true)
                        .withIncludeCodeBlock(false)
                        .withIncludeBlockquote(false)
                        .withAdditionalMetadata("filename", fileName)
                        .build();
                //转成mdDocument对象
                MarkdownDocumentReader reader = new MarkdownDocumentReader(resource, config);
                allDocuments.addAll(reader.get());
            }
        } catch (IOException e) {
            log.error("Markdown 文档加载失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Markdown 文档加载失败");
        }
        return allDocuments;
    }

}
