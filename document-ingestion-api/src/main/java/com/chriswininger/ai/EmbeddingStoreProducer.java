package com.chriswininger.ai;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

@ApplicationScoped
public class EmbeddingStoreProducer {

    private final AgroalDataSource dataSource;

    @Inject
    public EmbeddingStoreProducer(AgroalDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Produces
    @ApplicationScoped
    public EmbeddingStore<TextSegment> embeddingStore() {
        return PgVectorEmbeddingStore.datasourceBuilder()
                .datasource(dataSource)
                .table("vector_store")
                .dimension(1024)
                .createTable(false)
                .build();
    }
}
