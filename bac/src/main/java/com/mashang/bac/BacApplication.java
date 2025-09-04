package com.mashang.bac;

import org.springframework.ai.autoconfigure.vectorstore.pgvector.PgVectorStoreAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = PgVectorStoreAutoConfiguration.class)
public class BacApplication {

    public static void main(String[] args) {
        SpringApplication.run(BacApplication.class, args);
    }

}
