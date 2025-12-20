package com.wininger.spring_ai_demo.conversations;

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
 * Custom advisor that performs vector store search with a rewritten query.
 * This advisor replaces QuestionAnswerAdvisor and uses a pre-rewritten query
 * optimized for matching narrative content in vector searches.
 * 
 * The rewritten query should be passed to the constructor. If not provided,
 * it falls back to extracting the query from the prompt messages.
 */
public class QueryRewritingVectorStoreAdvisor implements CallAdvisor, StreamAdvisor {
    private static final Logger log = LoggerFactory.getLogger(QueryRewritingVectorStoreAdvisor.class);
    
    private final VectorStore vectorStore;
    private final int topK;
    private final String filterExpression;
    private final String rewrittenQuery;
    
    public QueryRewritingVectorStoreAdvisor(
            VectorStore vectorStore,
            int topK,
            String filterExpression,
            String rewrittenQuery
    ) {
        this.vectorStore = vectorStore;
        this.topK = topK;
        this.filterExpression = filterExpression;
        this.rewrittenQuery = rewrittenQuery;
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
     * Gets the search query - uses the rewritten query if available, otherwise extracts from prompt.
     */
    private String getSearchQuery(ChatClientRequest request) {
        // Use the rewritten query passed to constructor
        if (rewrittenQuery != null && !rewrittenQuery.trim().isEmpty()) {
            return rewrittenQuery;
        }
        
        // Fallback: try to extract from prompt messages (last user message)
        try {
            var messages = request.prompt().getInstructions();
            if (messages != null && !messages.isEmpty()) {
                // Get the last message which should be the user's question
                var lastMessage = messages.get(messages.size() - 1);
                if (lastMessage != null && lastMessage.getText() != null) {
                    String content = lastMessage.getText();
                    if (!content.trim().isEmpty()) {
                        return content;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Could not extract query from prompt messages", e);
        }
        
        return null;
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
