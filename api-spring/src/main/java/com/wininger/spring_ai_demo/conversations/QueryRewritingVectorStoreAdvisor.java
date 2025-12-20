package com.wininger.spring_ai_demo.conversations;

import com.wininger.spring_ai_demo.rag.QueryRewritingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * Custom advisor that performs vector store search with query rewriting.
 * This advisor replaces QuestionAnswerAdvisor and automatically rewrites queries
 * to better match narrative content in vector searches.
 *
 * The QueryRewritingService is used to rewrite the user's query before performing
 * the vector search. If not provided, it falls back to using the original query.
 *
 * Example usage:
 * <pre>{@code
 * QueryRewritingVectorStoreAdvisor.builder(vectorStore)
 *     .queryRewritingService(queryRewritingService)
 *     .topK(5)
 *     .filterExpression("source_id == 1")
 *     .build();
 * }</pre>
 */
public class QueryRewritingVectorStoreAdvisor implements CallAdvisor, StreamAdvisor {
    private static final Logger log = LoggerFactory.getLogger(QueryRewritingVectorStoreAdvisor.class);

    private final VectorStore vectorStore;
    private final int topK;
    private final String filterExpression;
    private final QueryRewritingService queryRewritingService;

    private QueryRewritingVectorStoreAdvisor(QueryRewritingVectorStoreAdvisorBuilder queryRewritingVectorStoreAdvisorBuilder) {
        this.vectorStore = queryRewritingVectorStoreAdvisorBuilder.vectorStore;
        this.topK = queryRewritingVectorStoreAdvisorBuilder.topK;
        this.filterExpression = queryRewritingVectorStoreAdvisorBuilder.filterExpression;
        this.queryRewritingService = queryRewritingVectorStoreAdvisorBuilder.queryRewritingService;
    }

    /**
     * Creates a new builder for QueryRewritingVectorStoreAdvisor.
     *
     * @param vectorStore The vector store to search
     * @return A new builder instance
     */
    public static QueryRewritingVectorStoreAdvisorBuilder builder(VectorStore vectorStore) {
        return new QueryRewritingVectorStoreAdvisorBuilder(vectorStore);
    }

    /**
     * Builder for QueryRewritingVectorStoreAdvisor.
     */
    public static class QueryRewritingVectorStoreAdvisorBuilder {
        private final VectorStore vectorStore;
        private int topK = 5;
        private String filterExpression;
        private QueryRewritingService queryRewritingService;

        private QueryRewritingVectorStoreAdvisorBuilder(VectorStore vectorStore) {
            this.vectorStore = vectorStore;
        }

        /**
         * Sets the query rewriting service to use for rewriting queries.
         *
         * @param queryRewritingService The service that rewrites queries for better matching
         * @return This builder for method chaining
         */
        public QueryRewritingVectorStoreAdvisorBuilder queryRewritingService(QueryRewritingService queryRewritingService) {
            this.queryRewritingService = queryRewritingService;
            return this;
        }

        /**
         * Sets the number of top results to retrieve (default: 5).
         *
         * @param topK The number of top results
         * @return This builder for method chaining
         */
        public QueryRewritingVectorStoreAdvisorBuilder topK(int topK) {
            this.topK = topK;
            return this;
        }

        /**
         * Sets the filter expression for filtering results.
         *
         * @param filterExpression The filter expression (e.g., "source_id == 1")
         * @return This builder for method chaining
         */
        public QueryRewritingVectorStoreAdvisorBuilder filterExpression(String filterExpression) {
            this.filterExpression = filterExpression;
            return this;
        }

        /**
         * Builds the QueryRewritingVectorStoreAdvisor instance.
         *
         * @return A new QueryRewritingVectorStoreAdvisor instance
         */
        public QueryRewritingVectorStoreAdvisor build() {
            return new QueryRewritingVectorStoreAdvisor(this);
        }
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        log.debug("QueryRewritingVectorStoreAdvisor: processing request");

        // Get the rewritten query from context, or extract from prompt
        String searchQuery = getSearchQuery(request);

        if (searchQuery == null || searchQuery.trim().isEmpty()) {
            log.debug("No search query found, proceeding without vector search");
            return chain.nextCall(request);
        }

        log.debug("Using search query: '{}'", searchQuery);

        // Perform vector search
        List<Document> documents = performVectorSearch(searchQuery);

        // Add documents to context (same key as QuestionAnswerAdvisor uses)
        // Try to modify context directly if mutable, otherwise pass request as-is
        // and let QuestionAnswerAdvisor handle it, or documents will be available via other means
        try {
            Map<String, Object> context = request.context();
            if (context != null) {
                context.put("qa_retrieved_documents", documents);
            }
        } catch (Exception e) {
            log.warn("Could not modify context directly, documents may not be available", e);
        }

        log.info("Found {} documents for query", documents.size());

        return chain.nextCall(request);
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        log.debug("QueryRewritingVectorStoreAdvisor: processing streaming request");

        // Get the rewritten query from context, or extract from prompt
        String searchQuery = getSearchQuery(request);

        if (searchQuery == null || searchQuery.trim().isEmpty()) {
            log.debug("No search query found, proceeding without vector search");
            return chain.nextStream(request);
        }

        log.debug("Using search query: '{}'", searchQuery);

        // Perform vector search
        List<Document> documents = performVectorSearch(searchQuery);

        // Add documents to context
        try {
            Map<String, Object> context = request.context();
            if (context != null) {
                context.put("qa_retrieved_documents", documents);
            }
        } catch (Exception e) {
            log.warn("Could not modify context directly, documents may not be available", e);
        }

        log.info("Found {} documents for query", documents.size());

        return chain.nextStream(request);
    }

    /**
     * Gets the search query - extracts from prompt and rewrites it if QueryRewritingService is available.
     */
    private String getSearchQuery(ChatClientRequest request) {
        // Extract the original query from prompt messages (last user message)
        String originalQuery = null;
        try {
            var messages = request.prompt().getInstructions();
            if (messages != null && !messages.isEmpty()) {
                // Get the last message which should be the user's question
                var lastMessage = messages.get(messages.size() - 1);
                if (lastMessage != null && lastMessage.getText() != null) {
                    String content = lastMessage.getText();
                    if (!content.trim().isEmpty()) {
                        originalQuery = content;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Could not extract query from prompt messages", e);
        }

        if (originalQuery == null || originalQuery.trim().isEmpty()) {
            return null;
        }

        // Rewrite the query if QueryRewritingService is available
        if (queryRewritingService != null) {
            String rewrittenQuery = queryRewritingService.rewriteQuerySingle(originalQuery);
            log.info("Query rewriting: '{}' -> '{}'", originalQuery, rewrittenQuery);
            return rewrittenQuery;
        }

        // Fallback to original query if no rewriting service
        return originalQuery;
    }

    /**
     * Performs vector search with the given query.
     */
    private List<Document> performVectorSearch(String query) {
        try {
            SearchRequest.Builder searchRequestBuilder = SearchRequest.builder()
                    .query(query)
                    .topK(topK);

            if (filterExpression != null && !filterExpression.trim().isEmpty()) {
                searchRequestBuilder.filterExpression(filterExpression);
            }

            SearchRequest searchRequest = searchRequestBuilder.build();

            log.debug("Performing vector search with query: '{}', topK: {}, filter: {}",
                    query, topK, filterExpression);

            return vectorStore.similaritySearch(searchRequest);

        } catch (Exception e) {
            log.error("Error performing vector search", e);
            return List.of();
        }
    }

    @Override
    public String getName() {
        return QueryRewritingVectorStoreAdvisor.class.getSimpleName();
    }

    @Override
    public int getOrder() {
        // Run at similar order to QuestionAnswerAdvisor
        return 1000;
    }
}
