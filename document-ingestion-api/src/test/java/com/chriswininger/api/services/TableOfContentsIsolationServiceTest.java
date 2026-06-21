package com.chriswininger.api.services;

import com.chriswininger.api.documents.services.TableOfContentsIsolationService;
import com.chriswininger.api.dto.inferenceresults.TableOfContentsIsolationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class TableOfContentsIsolationServiceTest {

    @Inject
    TableOfContentsIsolationService tableOfContentsIsolationService;

    @Test
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    void isolateTableOfContents_inlineDocumentWithToc() {
        final String document = """
                Title Page

                Some Publisher Info

                Contents

                Chapter I. Oh My
                Chapter II. The Birth of a Cat
                Chapter III. The Birds are Alright

                CHAPTER I

                The morning mist clung to the harbour like a veil. Ships rocked gently
                at their moorings while the gulls wheeled and cried overhead.

                CHAPTER II

                By afternoon the wind had shifted. The captain stood at the rail,
                watching the dark line of cloud building on the horizon.

                CHAPTER III

                When dawn broke there was no land in sight, only grey water stretching
                to every quarter of the compass.
                """;

        final TableOfContentsIsolationResult result =
                tableOfContentsIsolationService.isolateTableOfContents(document);

        System.out.println("=== Inline TOC Test ===");
        System.out.println("containsTableOfContents: " + result.containsTableOfContents());
        System.out.println("tableOfContents: " + result.tableOfContents());
        System.out.println("cleaned length: " + result.documentWithoutTableOfContents().length());

        assertThat(result.containsTableOfContents()).isTrue();
        assertThat(result.tableOfContents())
                .contains("Chapter I. Oh My")
                .contains("Chapter III. The Birds are Alright");
        assertThat(result.documentWithoutTableOfContents())
                .contains("CHAPTER I")
                .contains("The morning mist clung to the harbour")
                .contains("CHAPTER III")
                .contains("When dawn broke there was no land in sight");
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    void isolateTableOfContents_noToc() {
        final String document = """
                CHAPTER ONE

                It was a bright cold day in April, and the clocks were striking thirteen.

                CHAPTER TWO

                As he put his hand to the door-knob Winston saw that he had left the
                diary open on the table.

                CHAPTER THREE

                Winston was dreaming of his mother.
                """;

        final TableOfContentsIsolationResult result =
                tableOfContentsIsolationService.isolateTableOfContents(document);

        System.out.println("=== No TOC Test ===");
        System.out.println("containsTableOfContents: " + result.containsTableOfContents());

        assertThat(result.containsTableOfContents()).isFalse();
        assertThat(result.documentWithoutTableOfContents()).isEqualTo(document);
    }

    @TestFactory
    @Tag("manual")
    @Timeout(value = 10, unit = TimeUnit.MINUTES)
    Stream<DynamicTest> isolateTableOfContents_novels() throws IOException {
        final List<Map<String, Object>> entries = loadTestEntries();
        if (entries.isEmpty()) {
            return Stream.empty();
        }

        return entries.stream()
                .filter(entry -> {
                    final String fileName = (String) entry.get("fileName");
                    final boolean available = getClass().getClassLoader()
                            .getResource("testDocuments/novels/" + fileName) != null;
                    if (!available) {
                        System.out.println("Skipping (not found): " + fileName);
                    }
                    return available;
                })
                .map(entry -> {
                    final String fileName = (String) entry.get("fileName");
                    return DynamicTest.dynamicTest("isolateTableOfContents: " + fileName, () -> {
                        final String fullBook = loadNovelResource("testDocuments/novels/" + fileName);

                        final TableOfContentsIsolationResult result =
                                tableOfContentsIsolationService.isolateTableOfContents(fullBook);

                        System.out.println("=== " + fileName + " ===");
                        System.out.println("containsTableOfContents: " + result.containsTableOfContents());
                        if (result.tableOfContents() != null) {
                            System.out.println("tableOfContents length: " + result.tableOfContents().length());
                            System.out.println("tableOfContents preview: "
                                    + result.tableOfContents().substring(0,
                                    Math.min(500, result.tableOfContents().length())));
                        }
                        System.out.println("original length: " + fullBook.length());
                        System.out.println("cleaned length: " + result.documentWithoutTableOfContents().length());

                        final boolean expectedHasToc = (boolean) entry.get("hasToc");
                        assertThat(result.containsTableOfContents())
                                .as("hasToc for %s", fileName)
                                .isEqualTo(expectedHasToc);

                        if (expectedHasToc) {
                            @SuppressWarnings("unchecked")
                            final List<String> tocContains =
                                    (List<String>) entry.getOrDefault("tocContains", Collections.emptyList());
                            for (final String expected : tocContains) {
                                assertThat(result.tableOfContents())
                                        .as("TOC should contain '%s' in %s", expected, fileName)
                                        .contains(expected);
                            }
                        }

                        @SuppressWarnings("unchecked")
                        final List<String> cleanedDocContains =
                                (List<String>) entry.getOrDefault("cleanedDocContains", Collections.emptyList());
                        for (final String expected : cleanedDocContains) {
                            assertThat(result.documentWithoutTableOfContents())
                                    .as("Cleaned doc should contain '%s' in %s", expected, fileName)
                                    .contains(expected);
                        }
                    });
                });
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> loadTestEntries() throws IOException {
        try (final InputStream is = getClass().getClassLoader()
                .getResourceAsStream("testDocuments/novels/TableOfContentsIsolationServiceTest.yaml")) {
            if (is == null) {
                System.out.println("TableOfContentsIsolationServiceTest.yaml not found, skipping novel tests");
                return Collections.emptyList();
            }
            final ObjectMapper yaml = new ObjectMapper(new YAMLFactory());
            final Map<String, Object> root = yaml.readValue(is, Map.class);
            return (List<Map<String, Object>>) root.getOrDefault("isolateTableOfContents", Collections.emptyList());
        }
    }

    private String loadNovelResource(final String resourcePath) throws IOException {
        try (final InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
