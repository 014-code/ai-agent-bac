package com.mashang.bac.web.rag.config;

import com.mashang.bac.web.rag.LoveAppDocumentLoader;
import com.mashang.bac.web.rag.MyKeywordEnricher;
import com.mashang.bac.web.rag.MyTokenTextSplitter;
import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType.COSINE_DISTANCE;
import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType.HNSW;

/**
 * rag本地向量存储
 */
@Configuration
public class LoveAppVectorStoreConfig {

    @Resource
    private LoveAppDocumentLoader loveAppDocumentLoader;

    @Resource
    private MyTokenTextSplitter myTokenTextSplitter;

    @Resource
    private MyKeywordEnricher myKeywordEnricher;

    /**
     * 本地向量存储方法
     *
     * @param dashscopeEmbeddingModel
     * @return
     */
//    @Bean
//    VectorStore loveAppVectorStore(EmbeddingModel dashscopeEmbeddingModel) {
//        //构建向量存储，传入灵积向量模型
//        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(dashscopeEmbeddingModel)
//                .build();
//        //获取转成Document后的md文档
//        List<Document> documents = loveAppDocumentLoader.loadMarkdowns();
//        //add向量存储documents
//        simpleVectorStore.add(documents);
//        return simpleVectorStore;
//    }

    /**
     * pg数据库向量存储方法
     *
     * @param dashscopeEmbeddingModel
     * @return
     */
    @Bean
    VectorStore loveAppVectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel dashscopeEmbeddingModel) {
        VectorStore vectorStore = PgVectorStore.builder(jdbcTemplate, dashscopeEmbeddingModel)
                .dimensions(1536)
                .distanceType(COSINE_DISTANCE)
                .indexType(HNSW)
                .initializeSchema(true)
                .schemaName("public")
                .vectorTableName("vector_store")
                .maxDocumentBatchSize(10000)
                .build();
        //获取转成Document后的md文档
        List<Document> documents = loveAppDocumentLoader.loadMarkdowns();
        //切分一下
        //存入切分后的到数据库-此切分不准确暂不使用
//        List<Document> splitDocuments = myTokenTextSplitter.splitDocuments(documents);
        //ai自动打标签-源信息
        List<Document> enrichedDocuments = myKeywordEnricher.enrichDocuments(documents);
        vectorStore.add(enrichedDocuments);
        return vectorStore;
    }

}
