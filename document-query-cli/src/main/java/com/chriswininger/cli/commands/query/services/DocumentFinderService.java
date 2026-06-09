package com.chriswininger.cli.commands.query.services;

import com.chriswininger.cli.commands.query.dto.PossibleDocument;
import com.chriswininger.cli.commands.query.dto.PossibleDocumentsResult;
import com.chriswininger.cli.services.DocumentIngestionApiService;
import com.chriswininger.client.model.DocumentResponse;
import com.chriswininger.ollama.OllamaApiService;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class DocumentFinderService {

    private static final Logger LOG = Logger.getLogger(DocumentFinderService.class);

    private static final String SYSTEM_PROMPT = """
            You are a document relevance assistant. You will be given a user's question and
            metadata about a document (title, summary, author, characters, and questions the
            document might answer).

            Determine whether the full document could possibly help answer the user's question.
            
            This is a first pass, to gather documents that we should search more thoroughly.

            Respond in plain text only with exactly two labeled sections:

            Answer: yes
            or
            Answer: no

            Explanation:
            A brief explanation of why this document might or might not answer the user's question.

            Do NOT use JSON, markdown, or code fences.
            """;

    private static final String SYSTEM_PROMPT_STRUCTURED = """
            You are a data formatting assistant. You will be given plain-text relevance evaluations
            for multiple documents. Each evaluation contains a document ID and title, followed by
            labeled sections "Answer:" (yes or no) and "Explanation:".

            Your job is to convert these evaluations into a JSON object.

            Provide the following fields:
            ```
            %s
            ```

            Rules:
            - Include only documents where the evaluation answer was yes.
            - documentId MUST be the numeric ID only (e.g. 1, 2, 3), NOT the document title or any other text.
            - Do not add, remove, or rephrase any content. Faithfully convert what is given.
            - Respond with ONLY the JSON object. No markdown, no explanation, no code fences.
            """.trim();

    private final DocumentIngestionApiService documentIngestionApiService;
    private final OllamaApiService ollamaApiService;

    public DocumentFinderService(
            final DocumentIngestionApiService documentIngestionApiService,
            final OllamaApiService ollamaApiService
    ) {
        this.documentIngestionApiService = documentIngestionApiService;
        this.ollamaApiService = ollamaApiService;
    }

    public List<PossibleDocument> findPossibleDocumentsForQuery(final String query)
            throws IOException, InterruptedException {
        LOG.infof("Finding possible documents for query: %s", query);

        if (query == null || query.isBlank()) {
            return List.of();
        }

        final List<DocumentResponse> documents = documentIngestionApiService.getDocuments();

        if (documents.isEmpty()) {
            return List.of();
        }

        final String evaluationsBlock = buildEvaluationsBlock(query, documents);
        LOG.infof("Document evaluations block:\n%s", evaluationsBlock);

        final List<PossibleDocument> possibleDocuments = parseEvaluationsStructured(evaluationsBlock);
        LOG.infof("Structured pass found %d possible documents", possibleDocuments.size());

        return possibleDocuments;
    }

    private String buildEvaluationsBlock(
            final String query,
            final List<DocumentResponse> documents
    ) throws IOException, InterruptedException {
        final StringBuilder block = new StringBuilder();

        for (final DocumentResponse doc : documents) {
            final String response = evaluateDocumentUnstructured(query, doc);
            LOG.infof("Document %d (%s) evaluation:\n%s", doc.getId(), doc.getTitle(), response);

            block.append("===== Document (ID: %d, Title: %s) =====\n".formatted(doc.getId(), doc.getTitle()));
            block.append(response);
            block.append("\n\n");
        }

        return block.toString().trim();
    }

    private String evaluateDocumentUnstructured(
            final String query,
            final DocumentResponse doc
    ) throws IOException, InterruptedException {
        final String userMessage = buildUserMessage(query, doc);
        return ollamaApiService.callOllamaPlainTextResponse(
                SYSTEM_PROMPT,
                userMessage,
                false
        );
    }

    private List<PossibleDocument> parseEvaluationsStructured(
            final String evaluationsBlock
    ) throws IOException, InterruptedException {
        final String userMessage = """
                ===== Document Evaluations =====
                %s
                ================================

                Based on the above evaluations please respond with structured JSON.
                """.formatted(evaluationsBlock).trim();

        final PossibleDocumentsResult result = ollamaApiService.callOllamaStructuredResponse(
                SYSTEM_PROMPT_STRUCTURED.formatted(
                        ollamaApiService.buildExampleJson(PossibleDocumentsResult.class)
                ),
                userMessage,
                false,
                PossibleDocumentsResult.class
        );

        return result.possibleDocuments() != null ? result.possibleDocuments() : List.of();
    }

    private static String buildUserMessage(final String query, final DocumentResponse doc) {
        final String characters = doc.getCharacters() != null && !doc.getCharacters().isEmpty()
                ? doc.getCharacters().stream().collect(Collectors.joining("\n"))
                : "none";

        final String possibleQuestions = doc.getPossibleQuestionsThisAnswers() != null
                && !doc.getPossibleQuestionsThisAnswers().isEmpty()
                ? doc.getPossibleQuestionsThisAnswers().stream().collect(Collectors.joining("\n"))
                : "none";

        return """
                ===== User Question =====
                %s
                =========================

                ===== Document Metadata =====
                ID: %s
                Title: %s
                Type: %s
                Author: %s
                Year Published: %s

                Summary:
                %s

                Characters:
                %s

                Questions That Might be Answered:
                %s
                =============================

                Could this document possibly answer the user's question?
                """.formatted(
                query,
                doc.getId(),
                doc.getTitle(),
                doc.getType(),
                doc.getAuthorName(),
                doc.getYearPublished(),
                doc.getSummary(),
                characters,
                possibleQuestions
        ).trim();
    }
}
