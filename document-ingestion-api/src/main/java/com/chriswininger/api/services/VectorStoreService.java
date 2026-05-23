package com.chriswininger.api.services;

import com.chriswininger.api.dto.requests.SemanticSearchMatchResponse;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

@ApplicationScoped
public class VectorStoreService {

    private static final String METADATA_SECTION_ID = "sectionId";
    private static final String METADATA_CHAPTER_ID = "chapterId";
    private static final String METADATA_DOCUMENT_ID = "documentId";
    private static final String METADATA_BOOK_TITLE = "bookTitle";
    private static final String METADATA_CHAPTER_LABEL = "chapterLabel";

    private static final int DEFAULT_MAX_RESULTS = 10;

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;

    public VectorStoreService(
            final EmbeddingStore<TextSegment> embeddingStore,
            final EmbeddingModel embeddingModel
    ) {
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
    }

    public void storeVector(final String text, final Metadata metadata) {
        final TextSegment segment = TextSegment.from(text, metadata);
        final Embedding embedding = embeddingModel.embed(segment).content();
        embeddingStore.add(embedding, segment);
    }

    public List<SemanticSearchMatchResponse> search(
            final String phrase,
            final Long documentId,
            final Long chapterId,
            final Integer maxResults
    ) {
        final int effectiveMaxResults = maxResults != null ? maxResults : DEFAULT_MAX_RESULTS;
        final Embedding queryEmbedding = embeddingModel.embed(phrase).content();

        final var requestBuilder = EmbeddingSearchRequest.builder()
                .minScore(0.6)
                .queryEmbedding(queryEmbedding)
                .maxResults(effectiveMaxResults);

        final Filter filter = buildMetadataFilter(documentId, chapterId);
        if (filter != null) {
            requestBuilder.filter(filter);
        }

        final EmbeddingSearchResult<TextSegment> result = embeddingStore.search(requestBuilder.build());

        return result.matches().stream()
                .map(this::toMatchResponse)
                .toList();
    }

    private Filter buildMetadataFilter(final Long documentId, final Long chapterId) {
        Filter filter = null;

        if (documentId != null) {
            filter = metadataKey(METADATA_DOCUMENT_ID).isEqualTo(documentId);
        }
        if (chapterId != null) {
            final Filter chapterFilter = metadataKey(METADATA_CHAPTER_ID).isEqualTo(chapterId);
            filter = filter == null ? chapterFilter : filter.and(chapterFilter);
        }

        return filter;
    }

    private SemanticSearchMatchResponse toMatchResponse(final EmbeddingMatch<TextSegment> match) {
        final Metadata metadata = match.embedded().metadata();

        return new SemanticSearchMatchResponse(
                match.score(),
                match.embedded().text(),
                metadata.getLong(METADATA_SECTION_ID),
                metadata.getLong(METADATA_CHAPTER_ID),
                metadata.getLong(METADATA_DOCUMENT_ID),
                metadata.getString(METADATA_BOOK_TITLE),
                metadata.getString(METADATA_CHAPTER_LABEL)
        );
    }
}
