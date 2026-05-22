package com.chriswininger.api.services;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ApplicationScoped
public class DocumentChunkerService {
    private static final Logger LOG = Logger.getLogger(DocumentChunkerService.class);

    private static final int DEFAULT_PARAGRAPHS_PER_CHUNK = 5;

    public List<String> chunkText(final String text) {
        return chunkText(text, DEFAULT_PARAGRAPHS_PER_CHUNK);
    }

    public List<String> chunkText(final String text, final int paragraphsPerChunk) {
        final Document document = Document.from(text);
        final DocumentByParagraphSplitter splitter = new DocumentByParagraphSplitter(Integer.MAX_VALUE, 0);

        final List<String> paragraphs = Arrays.stream(splitter.split(document.text()))
                .filter(s -> !s.isBlank())
                .toList();

        LOG.infof("(chunkText) split into %d paragraphs, grouping into chunks of %d",
                paragraphs.size(), paragraphsPerChunk);

        final List<String> chunks = new ArrayList<>();
        for (int i = 0; i < paragraphs.size(); i += paragraphsPerChunk) {
            final int end = Math.min(i + paragraphsPerChunk, paragraphs.size());
            chunks.add(String.join("\n\n", paragraphs.subList(i, end)));
        }

        LOG.infof("(chunkText) produced %d chunks", chunks.size());
        return chunks;
    }
}
