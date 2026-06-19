package com.chriswininger.api.services;

import com.chriswininger.api.documents.services.ChapterSplitAIService;
import com.chriswininger.api.dto.inferenceresults.ChapterSplitterAIAnalysisResult;
import com.chriswininger.api.dto.inferenceresults.ChapterSplitterResult;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class ChapterSplitAIServiceTest {

    @Inject
    ChapterSplitAIService chapterSplitAIService;

    @Test
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    void detectSplitExpression_classicChapterHeadings() throws IOException, InterruptedException {
        final String bookContents = """
                INTRODUCTION

                This is the introduction to the novel, providing background and context.
                The author discusses their inspiration and dedication.

                CHAPTER ONE

                It was a bright cold day in April, and the clocks were striking thirteen.
                Winston Smith, his chin nuzzled into his breast in an effort to escape the
                vile wind, slipped quickly through the glass doors of Victory Mansions.

                CHAPTER TWO

                As he put his hand to the door-knob Winston saw that he had left the
                diary open on the table. DOWN WITH BIG BROTHER was written all over it.

                CHAPTER THREE

                Winston was dreaming of his mother. He must, he thought, have been ten
                or eleven years old when his mother had disappeared.
                """;

        final ChapterSplitterResult result = chapterSplitAIService.detectSplitExpression(bookContents);
        final var splitResults = result.aiAnalysisResult();

        assertNotNull(result, "Result should not be null");
        assertNotNull(splitResults.splitExpression(), "splitExpression should not be null");
        assertFalse(splitResults.splitExpression().isBlank(), "splitExpression should not be blank");

        System.out.println("Detected splitExpression: " + splitResults.splitExpression());

        final Pattern pattern = assertDoesNotThrow(
                () -> Pattern.compile(splitResults.splitExpression()),
                "splitExpression must be a valid Java regex");

        final Matcher matcher = pattern.matcher(bookContents);
        int matchCount = 0;
        while (matcher.find()) {
            matchCount++;
        }

        assertEquals(matchCount, 3);
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    void detectSplitExpression_numberedChapterHeadings() throws IOException, InterruptedException {
        final String bookFront = """
                Preface

                The author wishes to thank all those who contributed to this work.

                Chapter 1: The Beginning

                The village lay quiet under the summer sun. Nothing stirred in the dusty
                streets except a lone dog sleeping in the shade of the old oak.

                Chapter 2: The Stranger

                He arrived on the noon train with nothing but a leather satchel and a
                look in his eyes that said he had come a long way.

                Chapter 3: The Discovery

                It was Martha who found it first, half-buried in the garden behind the
                rectory wall.

                Chapter 4: The Reckoning

                By evening the whole town knew, and the knowing changed everything.
                """;

        final ChapterSplitterResult result = chapterSplitAIService.detectSplitExpression(bookFront);

        assertNotNull(result);
        assertNotNull(result.aiAnalysisResult().splitExpression());

        System.out.println("Detected splitExpression: " + result.aiAnalysisResult().splitExpression());

        final Pattern pattern = assertDoesNotThrow(
                () -> Pattern.compile(result.aiAnalysisResult().splitExpression()),
                "splitExpression must be a valid Java regex");

        final Matcher matcher = pattern.matcher(bookFront);
        int matchCount = 0;
        while (matcher.find()) {
            matchCount++;
            System.out.println("Match " + matchCount + ": [" + matcher.group().replace("\n", "\\n") + "]");
        }

        assertTrue(matchCount >= 4, "Expected at least 4 chapter heading matches, got " + matchCount);
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    void detectSplitExpression_sceneBreakMarkers() throws IOException, InterruptedException {
        final String bookFront = """
                The morning mist clung to the harbour like a veil. Ships rocked gently
                at their moorings while the gulls wheeled and cried overhead.

                * * *

                By afternoon the wind had shifted. The captain stood at the rail,
                watching the dark line of cloud building on the horizon.

                * * *

                Night fell swiftly. The lanterns were lit and the watch was set, and
                still the wind kept rising.

                * * *

                When dawn broke there was no land in sight, only grey water stretching
                to every quarter of the compass.
                """;

        final ChapterSplitterResult result = chapterSplitAIService.detectSplitExpression(bookFront);

        assertNotNull(result);
        assertNotNull(result.aiAnalysisResult().splitExpression());

        System.out.println("Detected splitExpression: " + result.aiAnalysisResult().splitExpression());

        final Pattern pattern = assertDoesNotThrow(
                () -> Pattern.compile(result.aiAnalysisResult().splitExpression()),
                "splitExpression must be a valid Java regex");

        final Matcher matcher = pattern.matcher(bookFront);
        int matchCount = 0;
        while (matcher.find()) {
            matchCount++;
            System.out.println("Match " + matchCount + ": [" + matcher.group().replace("\n", "\\n") + "]");
        }

        assertTrue(matchCount >= 3, "Expected at least 3 scene break matches, got " + matchCount);
    }


    // TODO: these will probably fail when we shift table of contents detection to ChapterService, likely we should
    // just be testing this as a function of ChapterService
    @TestFactory
    @Tag("manual")
    @Timeout(value = 10, unit = TimeUnit.MINUTES)
    Stream<DynamicTest> detectSplitExpression_novels() throws IOException {
        final List<Map<String, Object>> entries = loadNovelTestEntries();
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
                    return DynamicTest.dynamicTest("detectSplitExpression: " + fileName, () -> {
                        final String fullBook = loadNovelResource("testDocuments/novels/" + fileName);


                        final ChapterSplitterResult result =
                                chapterSplitAIService.detectSplitExpression(fullBook);

                        assertNotNull(result, "Result should not be null");
                        assertNotNull(result.aiAnalysisResult().splitExpression(), "splitExpression should not be null");
                        assertFalse(result.aiAnalysisResult().splitExpression().isBlank(), "splitExpression should not be blank");

                        System.out.println("=== " + fileName + " ===");
                        System.out.println("Detected splitExpression: " + result.aiAnalysisResult().splitExpression());

                        final Pattern pattern = assertDoesNotThrow(
                                () -> Pattern.compile(result.aiAnalysisResult().splitExpression()),
                                "splitExpression must be a valid Java regex");

                        final Matcher matcher = pattern.matcher(fullBook);
                        int matchCount = 0;
                        while (matcher.find()) {
                            matchCount++;
                            System.out.println("Match " + matchCount + ": ["
                                    + matcher.group().replace("\n", "\\n") + "]");
                        }

                        System.out.println("Total chapter matches: " + matchCount);
                        final int matchesGreaterThan = (int) entry.getOrDefault("matchesGreaterThan", 1);
                        assertTrue(matchCount > matchesGreaterThan,
                                "%s: expected >%d matches, got %d"
                                        .formatted(fileName, matchesGreaterThan, matchCount));
                    });
                });
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> loadNovelTestEntries() throws IOException {
        try (final InputStream is = getClass().getClassLoader()
                .getResourceAsStream("testDocuments/novels/ChapterSplitAIServiceTest.yaml")) {
            if (is == null) {
                System.out.println("ChapterSplitAIServiceTest.yaml not found, skipping novel tests");
                return Collections.emptyList();
            }
            final ObjectMapper yaml = new ObjectMapper(new YAMLFactory());
            final Map<String, Object> root = yaml.readValue(is, Map.class);
            return (List<Map<String, Object>>) root.getOrDefault("detectSplitExpression", Collections.emptyList());
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
