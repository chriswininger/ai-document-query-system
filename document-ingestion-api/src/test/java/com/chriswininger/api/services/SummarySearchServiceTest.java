package com.chriswininger.api.services;

import com.chriswininger.api.dto.inferenceresults.BookMetadataAnalysisResult;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

@QuarkusTest
class SummarySearchServiceTest {

    @Inject
    SummarySearchService summarySearchService;

    @Inject
    BookMetaExtractionService bookMetaExtractionService;

    @Test
    void findSummaries_shouldReturnResult() throws IOException {
        final String content = new String(
                getClass().getClassLoader()
                        .getResourceAsStream("testDocuments/novels/Master and Commander (Vol. Book 1) (Aubrey - Patrick O'Brian.txt")
                        .readAllBytes(),
                StandardCharsets.UTF_8
        );

        final String result = summarySearchService.findSummaries(content);

        System.out.println("!!! result: " + result);
    }


    @Test
    void extractMetaDataFromTheFrontOfTheBook_withNoPathSep_shouldReturnResult() throws IOException, InterruptedException {
        final String content = new String(
                getClass().getClassLoader()
                        .getResourceAsStream("testDocuments/novels/Master and Commander (Vol. Book 1) (Aubrey - Patrick O'Brian.txt")
                        .readAllBytes(),
                StandardCharsets.UTF_8
        );

        final String frontSummary = bookMetaExtractionService
                .extractMetaDataFromTheFrontOfTheBook(content, null);

        System.out.printf("""
                ===== Front =====
                %s
                ==================
                %n""", frontSummary);

        final String backOfBook = bookMetaExtractionService.extractMetaDataFromTheBackTheBook(content, frontSummary);

        System.out.printf("""
                ===== Back =====
                %s
                ==================
                %n""", backOfBook);
    }

    @Test
    void extractMetaDataFromTheBook_shouldReturnResults() throws IOException, InterruptedException {
        final String content = new String(
                getClass().getClassLoader()
                        .getResourceAsStream("testDocuments/novels/Master and Commander (Vol. Book 1) (Aubrey - Patrick O'Brian.txt")
                        .readAllBytes(),
                StandardCharsets.UTF_8
        );

        final BookMetadataAnalysisResult summary = bookMetaExtractionService
                .extractMetaDataFromTheBook(content, null);

        System.out.printf("""
                ===== Summary JSON =====
                %s
                ==================
                %n""", summary);
    }

    @Test
    void extractMetaDataFromTheFrontOfTheBook_withPathSep_shouldReturnResult() throws IOException, InterruptedException {
        final String content = new String(
                getClass().getClassLoader()
                        .getResourceAsStream("testDocuments/novels/Master and Commander (Vol. Book 1) (Aubrey - Patrick O'Brian.txt")
                        .readAllBytes(),
                StandardCharsets.UTF_8
        );

        final String result = bookMetaExtractionService
                .extractMetaDataFromTheFrontOfTheBook(content, Pattern.compile("CHAPTER .*\\n\\n"));

        System.out.printf("""
                ===== Result =====
                %s
                ==================
                %n""", result);
    }
}
