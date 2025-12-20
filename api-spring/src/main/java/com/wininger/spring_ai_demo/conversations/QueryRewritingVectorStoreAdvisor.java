package com.wininger.spring_ai_demo.conversations;

import com.wininger.spring_ai_demo.api.rag.VectorSearchResult;
import com.wininger.spring_ai_demo.api.rag.VectorSearchService;
import com.wininger.spring_ai_demo.rag.QueryRewritingService;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
 * QueryRewritingVectorStoreAdvisor.builder(vectorSearchService)
 *     .queryRewritingService(queryRewritingService)
 *     .topK(5)
 *     .documentSourceIds(List.of(1, 2, 3))
 *     .build();
 * }</pre>
 */
public class QueryRewritingVectorStoreAdvisor implements BaseAdvisor {
    private static final Logger log = LoggerFactory.getLogger(QueryRewritingVectorStoreAdvisor.class);

    public static final String RETRIEVED_DOCUMENTS = "qa_retrieved_documents";

    private static final PromptTemplate PROMPT_TEMPLATE = new PromptTemplate("""
			{query}

			Context information is below, surrounded by ---------------------

			---------------------
			{question_answer_context}
			---------------------

			Given the context and provided history information and not prior knowledge,
			reply to the user comment. If the answer is not in the context, inform
			the user that you can't answer the question.
			""");

    private final VectorSearchService vectorSearchService;
    private final int topK;
    private final List<Integer> documentSourceIds;
    private final QueryRewritingService queryRewritingService;

    private QueryRewritingVectorStoreAdvisor(QueryRewritingVectorStoreAdvisorBuilder queryRewritingVectorStoreAdvisorBuilder) {
        this.vectorSearchService = queryRewritingVectorStoreAdvisorBuilder.vectorSearchService;
        this.topK = queryRewritingVectorStoreAdvisorBuilder.topK;
        this.documentSourceIds = queryRewritingVectorStoreAdvisorBuilder.documentSourceIds;
        this.queryRewritingService = queryRewritingVectorStoreAdvisorBuilder.queryRewritingService;
    }

    /**
     * Creates a new builder for QueryRewritingVectorStoreAdvisor.
     *
     * @param vectorSearchService The vector search service to use for searching
     * @return A new builder instance
     */
    public static QueryRewritingVectorStoreAdvisorBuilder builder(VectorSearchService vectorSearchService) {
        return new QueryRewritingVectorStoreAdvisorBuilder(vectorSearchService);
    }

    /**
     * Builder for QueryRewritingVectorStoreAdvisor.
     */
    public static class QueryRewritingVectorStoreAdvisorBuilder {
        private final VectorSearchService vectorSearchService;
        private int topK = 5;
        private List<Integer> documentSourceIds;
        private QueryRewritingService queryRewritingService;

        private QueryRewritingVectorStoreAdvisorBuilder(VectorSearchService vectorSearchService) {
            this.vectorSearchService = vectorSearchService;
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
         * Sets the document source IDs to filter results by.
         * The filter expression will be built internally as "source_id == id1 || source_id == id2 || ..."
         *
         * @param documentSourceIds The list of document source IDs to filter by
         * @return This builder for method chaining
         */
        public QueryRewritingVectorStoreAdvisorBuilder documentSourceIds(List<Integer> documentSourceIds) {
            this.documentSourceIds = documentSourceIds;
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
    public ChatClientRequest before(
        final @NonNull ChatClientRequest chatClientRequest,
        final @NonNull AdvisorChain advisorChain
    ) {
        // Get the rewritten query from context or extract from prompt
        final String searchQuery = rewriteQuery(chatClientRequest.prompt().getUserMessage().getText());

        final var documents = performVectorSearch(searchQuery);

        // store the documents on the context
        final Map<String, Object> context = new HashMap<>(chatClientRequest.context());
        context.put(RETRIEVED_DOCUMENTS, documents);

        final String documentContext = documents.stream()
            .map(Document::getText)
            .collect(Collectors.joining(System.lineSeparator()));

        // modify the user prompt to inject the documents using our temlate
        final var userMessage = chatClientRequest.prompt().getUserMessage();
        String augmentedUserText = PROMPT_TEMPLATE
            .render(Map.of("query", userMessage.getText(), "question_answer_context", documentContext));

        return chatClientRequest.mutate()
            .prompt(chatClientRequest.prompt().augmentUserMessage(augmentedUserText))
            .context(context)
            .build();
    }

    // add documents to response metadata at the end of the chain,this
    // is another place we could hook our RAG docs int or maybe we could look
    // for our missing thinking tokens here
    @Override
    public ChatClientResponse after(
        @NonNull ChatClientResponse chatClientResponse,
        @NonNull AdvisorChain advisorChain
    ) {
        ChatResponse.Builder chatResponseBuilder;
        if (chatClientResponse.chatResponse() == null) {
            chatResponseBuilder = ChatResponse.builder();
        }
        else {
            chatResponseBuilder = ChatResponse.builder().from(chatClientResponse.chatResponse());
        }

        chatResponseBuilder.metadata(RETRIEVED_DOCUMENTS, chatClientResponse.context().get(RETRIEVED_DOCUMENTS));

        return ChatClientResponse.builder()
            .chatResponse(chatResponseBuilder.build())
            .context(chatClientResponse.context())
            .build();
    }

    private String rewriteQuery(final String originalQuery) {
        // Rewrite the query if QueryRewritingService is available
        if (queryRewritingService != null) {
            String rewrittenQuery = queryRewritingService.rewriteQuerySingle(originalQuery);
            log.info("Query rewriting: '{}' -> '{}'", originalQuery, rewrittenQuery);
            return rewrittenQuery;
        } else {
            log.warn("No queryRewritingService provided");
            log.info("Query rewriting disabled using: '{}'", originalQuery);
        }

        // Fallback to original query if no rewriting service
        return originalQuery;
    }

    private List<Document> performVectorSearch(String query) {
        try {
            log.debug("Performing vector search with query: '{}', topK: {}, documentSourceIds: {}",
                    query, topK, documentSourceIds);

            List<VectorSearchResult> searchResults = vectorSearchService.performSearch(
                    query,
                    topK,
                    documentSourceIds != null ? documentSourceIds : List.of()
            );

            // Convert VectorSearchResult to Document
            return searchResults.stream()
                    .map(result -> new Document(result.text(), result.metadata()))
                    .collect(Collectors.toList());

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
