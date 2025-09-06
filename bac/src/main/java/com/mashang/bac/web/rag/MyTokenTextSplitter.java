package com.mashang.bac.web.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 自定义切分器(官方切词器有问题，例如切词时有不明符号且不准确)
 */
@Component
@Slf4j
public class MyTokenTextSplitter {
    /**
     * 切分文档方法
     *
     * @param documents
     * @return
     */
    public List<Document> splitDocuments(List<Document> documents) {
        TokenTextSplitter splitter = new TokenTextSplitter();
        return splitter.apply(documents);
    }

    /**
     * 切分规则定义方法
     * 可以精细控制切割的参数：
     * <p>
     * •
     * 200：每块最大200个"token"(可以理解为200个词或字符单位)
     * <p>
     * •
     * 100：相邻两块之间可以有100个token的重叠(防止重要信息被切断)
     * <p>
     * •
     * 10：最小的块不能小于10个token
     * <p>
     * •
     * 5000：最多处理5000个token的文档
     * <p>
     * •
     * true：保持段落完整性(不在段落中间切断)
     *
     * @param documents
     * @return
     */
    public List<Document> splitCustomized(List<Document> documents) {
        TokenTextSplitter splitter = new TokenTextSplitter(200, 100, 10, 5000, true);
        return splitter.apply(documents);
    }
}
