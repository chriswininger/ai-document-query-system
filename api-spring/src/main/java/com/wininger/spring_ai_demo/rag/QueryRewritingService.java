package com.wininger.spring_ai_demo.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service that rewrites user questions into forms more likely to match narrative content
 * in a vector database. For example, converts "Does Steven Maturin play music?" into
 * search-friendly forms like "Steven Maturin plays music" or "Steven Maturin musical instrument".
 */
@Service
public class QueryRewritingService {
    private static final Logger log = LoggerFactory.getLogger(QueryRewritingService.class);

    private final OllamaChatModel ollamaChatModel;

    public QueryRewritingService(OllamaChatModel ollamaChatModel) {
        this.ollamaChatModel = ollamaChatModel;
    }

    /**
     * Rewrites a question into multiple search-friendly query forms optimized for
     * finding relevant passages in narrative text (like novels).
     *
     * @param originalQuery The original user question
     * @return A list of rewritten queries optimized for semantic search
     */
    public List<String> rewriteQuery(String originalQuery) {
        log.debug("Rewriting query: '{}'", originalQuery);

        String rewritingPrompt = """
            You are helping to rewrite questions into forms that will better match narrative prose 
            in a vector database search. The goal is to transform questions into statements and 
            phrases that are more likely to appear in narrative text.
            
            Original question: %s
            
            Generate 3-5 alternative query forms that:
            1. Convert questions to declarative statements (e.g., "Does X do Y?" -> "X does Y")
            2. Extract key entities and relationships
            3. Use natural language that might appear in narrative prose
            4. Include variations focusing on different aspects (actions, descriptions, relationships)
            5. Preserve propper names in the rewritten query
            
            Return ONLY the rewritten queries, one per line, without numbering or bullets.
            Keep each query concise (under 10 words when possible).
            """.formatted(originalQuery);

        try {
            var response = ollamaChatModel.call(
                new Prompt(rewritingPrompt, OllamaChatOptions.builder().build())
            );

            String rewrittenText = response.getResult().getOutput().getText();
            log.debug("Raw rewriting response: '{}'", rewrittenText);

            // Parse the response into individual queries
            List<String> rewrittenQueries = rewrittenText.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .filter(line -> !line.matches("^\\d+[.)]\\s*")) // Remove numbered lists
                .filter(line -> !line.startsWith("-") && !line.startsWith("*")) // Remove bullets
                .map(line -> line.replaceFirst("^[-*]\\s*", "")) // Clean up any remaining bullets
                .map(line -> line.replaceFirst("^\\d+[.)]\\s*", "")) // Clean up any remaining numbers
                .filter(line -> line.length() > 5) // Filter out very short lines
                .limit(5) // Take up to 5 queries
                .toList();

            // Always include the original query as the first option
            if (rewrittenQueries.isEmpty()) {
                log.warn("No rewritten queries generated, using original query");
                return List.of(originalQuery);
            }

            // Prepend original query to the list
            List<String> allQueries = new java.util.ArrayList<>();
            allQueries.add(originalQuery);
            allQueries.addAll(rewrittenQueries);

            log.info("Rewritten queries ({} total): {}", allQueries.size(), allQueries);
            return allQueries;

        } catch (Exception e) {
            log.error("Error rewriting query, falling back to original", e);
            return List.of(originalQuery);
        }
    }

    /**
     * Rewrites a query into a single optimized search query.
     * This is a simpler version that returns one best rewrite.
     *
     * @param originalQuery The original user question
     * @return A single rewritten query optimized for semantic search
     */
    public String rewriteQuerySingle(String originalQuery) {
        log.debug("Rewriting query (single): '{}'", originalQuery);

        String rewritingPrompt = """
            Rewrite this question into a declarative statement or phrase that would better match 
            narrative prose in a vector database search. Convert questions to statements, extract 
            key entities and relationships, and use natural language that might appear in a novel.
            
            1. Convert questions to declarative statements (e.g., "Does X do Y?" -> "X does Y")
            2. Extract key entities and relationships
            3. Use natural language that might appear in narrative prose
            4. Include variations focusing on different aspects (actions, descriptions, relationships)
            5. Preserve propper names in the rewritten query
            
            Question: %s
            
            Return ONLY the rewritten query, nothing else.
            """.formatted(originalQuery);

        try {
            var response = ollamaChatModel.call(
                new Prompt(rewritingPrompt, OllamaChatOptions.builder().build())
            );

            String rewritten = response.getResult().getOutput().getText().trim();

            // Clean up any formatting artifacts
            rewritten = rewritten.replaceFirst("^[-*]\\s*", "")
                                 .replaceFirst("^\\d+[.)]\\s*", "")
                                 .replaceAll("\"", "")
                                 .trim();

            if (rewritten.isEmpty() || rewritten.length() < 5) {
                log.warn("Invalid rewritten query, using original");
                return originalQuery;
            }

            log.info("Rewritten query: '{}' -> '{}'", originalQuery, rewritten);
            return rewritten;

        } catch (Exception e) {
            log.error("Error rewriting query, falling back to original", e);
            return originalQuery;
        }
    }
}
