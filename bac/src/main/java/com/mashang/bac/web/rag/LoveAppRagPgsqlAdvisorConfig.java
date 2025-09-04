//package com.mashang.bac.web.rag;
//
//import org.postgresql.ds.PGSimpleDataSource;
//import org.springframework.ai.chat.client.advisor.RetrievalAugmentationAdvisor;
//import org.springframework.ai.chat.client.advisor.api.Advisor;
//import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
//import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
///**
// * pgsql知识库顾问
// */
//@Configuration
//public class LoveAppRagPgsqlAdvisorConfig {
//
//    @Value("${spring.datasource.url}")
//    private String dataSourceUrl;
//
//    @Value("${spring.datasource.username}")
//    private String username;
//
//    @Value("${spring.datasource.password}")
//    private String password;
//
//    @Bean
//    public Advisor loveAppRagPgsqlAdvisor() {
//        // 创建 PostgreSQL 数据源
//        PGSimpleDataSource dataSource = new PGSimpleDataSource();
//        dataSource.setUrl(dataSourceUrl);
//        dataSource.setUser(username);
//        dataSource.setPassword(password);
//
//        // 创建 PostgreSQL 向量存储
//        PgVectorStore vectorStore = new PgVectorStore(dataSource);
//
//        // 正确的文档检索器创建方式 - 使用 VectorStoreRetriever
//        VectorStoreRetriever documentRetriever = new VectorStoreRetriever(vectorStore);
//
//        // 创建文档检索器 - 使用 VectorStoreSearchRetriever 而不是 VectorStoreRetriever
////        VectorStoreSearchRetriever documentRetriever = new VectorStoreSearchRetriever(vectorStore);
//
//        // 构造知识库顾问
//        return RetrievalAugmentationAdvisor.builder()
//                .documentRetriever(documentRetriever)
//                .build();
//    }
//}
