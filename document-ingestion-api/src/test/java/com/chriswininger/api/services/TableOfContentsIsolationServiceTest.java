package com.chriswininger.api.services;

import com.chriswininger.api.documents.services.TableOfContentsIsolationService;
import com.chriswininger.api.dto.inferenceresults.TableOfContentsIsolationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class TableOfContentsIsolationServiceTest {

    private static final int NOVEL_RUN_COUNT = 5;

    private static final Path OUTPUT_YAML_PATH = Path.of(
            "src/test/resources/testDocuments/novels/TableOfContentsIsolationServiceTest-Output.yaml");

    @Inject
    TableOfContentsIsolationService tableOfContentsIsolationService;

    @ConfigProperty(name = "ollama.toc-isolation.model-name", defaultValue = "gemma4:e4b")
    String modelName;

    @ConfigProperty(name = "ollama.toc-isolation.num-ctx", defaultValue = "32768")
    int numCtx;

    @ConfigProperty(name = "ollama.toc-isolation.base-url", defaultValue = "https://ollama.com")
    String baseUrl;

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
    @Timeout(value = 30, unit = TimeUnit.MINUTES)
    Stream<DynamicTest> isolateTableOfContents_novels() throws IOException {
        final List<Map<String, Object>> testFileEntries = loadTestEntries();
        if (testFileEntries.isEmpty()) {
            return Stream.empty();
        }

        final String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));

        return testFileEntries.stream()
                .filter(entry -> {
                    final String fileName = (String) entry.get("fileName");
                    final Boolean skipFile = (Boolean) entry.getOrDefault("skipFile", false);

                    if (skipFile) {
                        System.out.println("Skipping (set to skip): " + fileName);
                        return false;
                    }

                    final boolean available = getClass().getClassLoader()
                            .getResource("testDocuments/novels/" + fileName) != null;
                    if (!available) {
                        System.out.println("Skipping (not found): " + fileName);
                        return false;
                    }

                    return true;
                })
                .map(testFileEntry -> {
                    final String fileName = (String) testFileEntry.get("fileName");

                    final List<String> tocContains =
                            (List<String>) testFileEntry.getOrDefault("tocContains", Collections.emptyList());
                    final List<String> tocDoesNotContainContains =
                            (List<String>) testFileEntry.getOrDefault("tocDoesNotContainContains", Collections.emptyList());

                    return DynamicTest.dynamicTest("isolateTableOfContents: " + fileName, () -> {
                        final String fullBook = loadNovelResource("testDocuments/novels/" + fileName);
                        final boolean expectedHasTableOfContents = (boolean) testFileEntry.get("hasToc");

                        final List<Map<String, Object>> perRunResults = new ArrayList<>();
                        final List<Long> runMillis = new ArrayList<>();
                        Throwable firstFailure = null;

                        for (int i = 0; i < NOVEL_RUN_COUNT; i++) {
                            System.out.printf("=== %s | run %d/%d ===%n", fileName, i + 1, NOVEL_RUN_COUNT);
                            final long start = System.currentTimeMillis();
                            Throwable failure = null;

                            try {
                                final TableOfContentsIsolationResult result =
                                        tableOfContentsIsolationService.isolateTableOfContents(fullBook);

                                printHelper(result, fileName, fullBook);
                                runAssertions(result, fullBook, expectedHasTableOfContents, tocContains, tocDoesNotContainContains);
                            } catch (final Throwable t) {
                                failure = t;
                                if (firstFailure == null) {
                                    firstFailure = t;
                                }
                                System.out.printf("  run %d FAILED: %s%n", i + 1, t.getMessage());
                            }

                            final long elapsed = System.currentTimeMillis() - start;
                            runMillis.add(elapsed);

                            final Map<String, Object> runEntry = new LinkedHashMap<>();
                            runEntry.put("time", formatDuration(elapsed));
                            runEntry.put("passed", failure == null);
                            perRunResults.add(runEntry);
                        }

                        final long avgMillis = runMillis.stream().mapToLong(Long::longValue).sum() / runMillis.size();
                        final long totalPassed = perRunResults.stream().filter(r -> Boolean.TRUE.equals(r.get("passed"))).count();
                        final long totalFailed = NOVEL_RUN_COUNT - totalPassed;

                        appendBookResultToOutputYaml(timestamp, fileName, perRunResults, avgMillis, totalPassed, totalFailed);

                        if (firstFailure != null) {
                            throw firstFailure;
                        }
                    });
                });
    }

    private void runAssertions(
            final TableOfContentsIsolationResult result,
            final String fullBook,
            final boolean expectedHasTableOfContents,
            final List<String> tocContains,
            final List<String> tocDoesNotContainContains
    ) {
        assertThat(result.containsTableOfContents()).isEqualTo(expectedHasTableOfContents);

        if (result.containsTableOfContents()) {
            final List<String> allLinesWithTextInFoundToc = Arrays.stream(result.tableOfContents().trim().split("\n"))
                    .filter(ln -> !ln.isBlank())
                    .toList();

            assertThat(result.tableOfContents()).contains(tocContains);

            tocContains.forEach(expectedTocEntry -> {
                final var numMatches = allLinesWithTextInFoundToc
                        .stream()
                        .filter(entry -> entry.equals(expectedTocEntry))
                        .toList()
                        .size();
                assertThat(numMatches).isEqualTo(1);
            });

            tocContains.forEach(tocLine -> {
                assertThat(result.tableOfContents()).doesNotContain(tocDoesNotContainContains);
                assertThat(result.documentWithoutTableOfContents()).contains(tocDoesNotContainContains);
            });

            assertThat(result.documentWithoutTableOfContents().length())
                    .isEqualTo(fullBook.length() - result.tableOfContents().length());
        }
    }

    @SuppressWarnings("unchecked")
    private void appendBookResultToOutputYaml(
            final String timestamp,
            final String fileName,
            final List<Map<String, Object>> perRunResults,
            final long avgMillis,
            final long totalPassed,
            final long totalFailed
    ) {
        try {
            final ObjectMapper yaml = new ObjectMapper(new YAMLFactory());

            Map<String, Object> root;
            if (Files.exists(OUTPUT_YAML_PATH)) {
                root = yaml.readValue(OUTPUT_YAML_PATH.toFile(), Map.class);
            } else {
                root = new LinkedHashMap<>();
            }

            // Find or create the run entry for this timestamp
            final List<Map<String, Object>> runs =
                    (List<Map<String, Object>>) root.computeIfAbsent("runs", k -> new ArrayList<>());

            Map<String, Object> currentRun = runs.stream()
                    .filter(r -> timestamp.equals(r.get("timestamp")))
                    .findFirst()
                    .orElse(null);

            if (currentRun == null) {
                currentRun = new LinkedHashMap<>();
                currentRun.put("timestamp", timestamp);
                currentRun.put("model", modelName);
                currentRun.put("numCtx", numCtx);
                currentRun.put("baseUrl", baseUrl);
                currentRun.put("books", new ArrayList<>());
                runs.add(currentRun);
            }

            final List<Map<String, Object>> books =
                    (List<Map<String, Object>>) currentRun.get("books");

            final Map<String, Object> bookEntry = new LinkedHashMap<>();
            bookEntry.put("fileName", fileName);
            bookEntry.put("runs", perRunResults);
            bookEntry.put("averageTime", formatDuration(avgMillis));
            bookEntry.put("totalPassed", totalPassed);
            bookEntry.put("totalFailed", totalFailed);
            books.add(bookEntry);

            final boolean allPass = books.stream()
                    .allMatch(b -> Long.valueOf(0L).equals(b.get("totalFailed")));
            currentRun.put("allPass", allPass);

            Files.createDirectories(OUTPUT_YAML_PATH.getParent());
            yaml.writeValue(OUTPUT_YAML_PATH.toFile(), root);

            System.out.printf("Output written to %s%n", OUTPUT_YAML_PATH.toAbsolutePath());
        } catch (final IOException e) {
            System.err.println("Failed to write output YAML: " + e.getMessage());
        }
    }

    private String formatDuration(final long millis) {
        final long totalSeconds = millis / 1000;
        final long minutes = totalSeconds / 60;
        final long seconds = totalSeconds % 60;
        return minutes > 0 ? "%dm %ds".formatted(minutes, seconds) : "%ds".formatted(seconds);
    }

    private void printHelper(final TableOfContentsIsolationResult result, final String fileName, final String fullBook) {
        System.out.println("=== " + fileName + " ===");
        System.out.println("containsTableOfContents: " + result.containsTableOfContents());
        if (result.tableOfContents() != null) {
            System.out.println("tableOfContents length: " + result.tableOfContents().length());
            System.out.println("tableOfContents preview: "
                    + result.tableOfContents().substring(0,
                    Math.min(5000, result.tableOfContents().length())));
        }
        System.out.println("original length: " + fullBook.length());
        System.out.println("cleaned length: " + result.documentWithoutTableOfContents().length());
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
