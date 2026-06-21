package com.chriswininger.api.services;

import com.chriswininger.api.documents.services.Chapter;
import com.chriswininger.api.documents.services.ChapterService;
import com.chriswininger.api.documents.services.ChapterSummaryAiServiceDirect;
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
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class ChapterServiceTest {

    @Inject
    ChapterService chapterService;

    @Inject
    ChapterSummaryAiServiceDirect chapterSummaryAiServiceDirect;

    @Test
    void splitIntoChapters_threeChapters_returnsCorrectLabelsAndContent() {
        String document = """
                Some introductory text.
                Chapter 1
                Some text for chapter one.
                Chapter 2
                Some text for chapter two.
                Chapter 3
                Some text for chapter three.
                """;

        Pattern pattern = Pattern.compile("Chapter \\d+");

        List<Chapter> chapters = chapterService.splitIntoChapters(document, pattern);

        assertEquals(4, chapters.size());

        assertEquals("Intro", chapters.get(0).label());
        assertEquals("Chapter 1", chapters.get(1).label());
        assertEquals("Chapter 2", chapters.get(2).label());
        assertEquals("Chapter 3", chapters.get(3).label());

        assertTrue(chapters.get(0).content().contains("Some introductory text."));
        assertTrue(chapters.get(1).content().contains("Some text for chapter one."));
        assertTrue(chapters.get(2).content().contains("Some text for chapter two."));
        assertTrue(chapters.get(3).content().contains("Some text for chapter three."));
    }

    @Test
    void summarizeChapter_shouldProduceASummaryOfAShortChapter() {
        final var chp = chapterService.summarizeChapter(new Chapter("Chapter 1", """
                The platform was empty except for Marcus and the hum of fluorescent lights that couldn't decide whether to flicker or die.
                
                  He checked his watch. 11:58. The last train to Harwick was supposed to leave at midnight, and he'd been standing here for twenty minutes already, rehearsing what he would say to his father.
                
                  I'm sorry felt too small. It wasn't my fault felt too large.
                
                  The rails shivered before the train appeared — a low vibration that moved up through the soles of his shoes and into his chest. When the doors slid open, warm air rolled out like a held breath
                  finally released. Marcus stepped on.
                
                  The car was not empty.
                
                  A woman sat near the back, coat folded neatly on the seat beside her, reading a book with no title on the spine. She didn't look up. Marcus took a seat near the door and watched the dark tunnel
                  swallow the platform whole.
                
                  "Harwick?" the woman asked, still reading.
                
                  "Yeah."
                
                  She turned a page. "Funny. It's usually people running from Harwick on this train."
                
                  Marcus looked at his hands. "Maybe I am."
                
                  The train picked up speed. Outside, the underground walls gave way to a stretch of open night sky, stars blinking on one by one as the city thinned behind them.
                
                  He still didn't know what he would say. But for the first time all evening, that felt like enough.
                """));
    }

    @Test
    void summarizeChapter_shouldProduceASummaryOfALongChapter() throws IOException {
        String content = new String(
                getClass().getClassLoader().getResourceAsStream("long-chapter-example.txt").readAllBytes(),
                StandardCharsets.UTF_8
        );

        final var chp = chapterService.summarizeChapter(new Chapter("Chapter 1", content));
    }


    // master_and_commander_vol_book_1_aubrey_patrick_o_brian_chapter_eleven.txt

    // ok strangely chpapter 9 seems to be where we bog down
    @Test
    @Timeout(value = 20, unit = TimeUnit.MINUTES)
    void summarizeChapter_shouldProduceASummaryOfChapter_nine() throws IOException {
        final String chapterName = "master_and_commander_vol_book_1_aubrey_patrick_o_brian_chapter_nine.txt";
        //final String chapterName = "master_and_commander_vol_book_1_aubrey_patrick_o_brian_chapter_one.txt";
        String content = new String(
                getClass().getClassLoader()
                        .getResourceAsStream("testDocuments/" +
                                "master_and_commander_vol_book_1_aubrey_patrick_o_brian/" +
                                chapterName).readAllBytes(),
                StandardCharsets.UTF_8
        );

        // time take 5376s
        for (int i = 0; i < 5; i++) {
            final long startTime = System.currentTimeMillis();
            System.out.println("Size: " + content.length());
            try {
                final var summary = chapterService.summarizeChapter(new Chapter("Chapter 1", content));
                System.out.println("Summary.characters.size: " + summary.characters().size());
            } catch (Exception ex){
                System.out.println("!!! error: " + ex.getMessage());
            }
            System.out.println("Time taken: " + (System.currentTimeMillis() - startTime));
        }
    }

    @Test
    @Timeout(value = 20, unit = TimeUnit.MINUTES)
    void summarizeChapter_shouldProduceASummaryOfChapter_nine_basic() throws IOException {
        final String chapterName = "master_and_commander_vol_book_1_aubrey_patrick_o_brian_chapter_nine.txt";
        //final String chapterName = "master_and_commander_vol_book_1_aubrey_patrick_o_brian_chapter_one.txt";
        String content = new String(
                getClass().getClassLoader()
                        .getResourceAsStream("testDocuments/" +
                                "master_and_commander_vol_book_1_aubrey_patrick_o_brian/" +
                                chapterName).readAllBytes(),
                StandardCharsets.UTF_8
        );

        // time take 5376s
        for (int i = 0; i <  15; i++) {
            final long startTime = System.currentTimeMillis();
            System.out.println("Size: " + content.length());
            try {
                final var summary = chapterSummaryAiServiceDirect.summarize("Chapter 1", content);
                System.out.println("Summary.characters.size: " + summary.characters().size());
                System.out.println("Characters: " + summary.characters());
            } catch (Exception ex){
                System.out.println("error: " + ex.getMessage());
            }
            System.out.println("Time taken: " + (System.currentTimeMillis() - startTime));
        }
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    void splitIntoChapters_tableOfContentsAtStart() throws IOException, InterruptedException {
        final String book = """
                Contents:

                Chapter I. Oh My
                Chapter II. The Birth of a Cat
                Chapter III. The Birds are Alright
                
                Much Intro

                The morning mist clung to the harbour like a veil. Ships rocked gently
                at their moorings while the gulls wheeled and cried overhead.

                Maybe Book
                
                Chapter I

                By afternoon the wind had shifted. The captain stood at the rail,
                watching the dark line of cloud building on the horizon.

                Chapter II

                Night fell swiftly. The lanterns were lit and the watch was set, and
                still the wind kept rising.

                Chapter III

                When dawn broke there was no land in sight, only grey water stretching
                to every quarter of the compass.
                """;

        final List<Chapter> result = chapterService.splitIntoChapters(book, null);

        assertThat(result).hasSize(4);
        assertEquals("Intro", result.getFirst().label());
        assertThat(result.getFirst().content())
                .contains("Much Intro")
                .contains("The morning mist clung to the harbour like a veil. Ships rocked gently")
                .contains("at their moorings while the gulls wheeled and cried overhead.")
                .contains("Maybe Book");
        assertEquals("Chapter I", result.get(1).label());
        assertThat(result.get(1).content())
                .contains("By afternoon the wind had shifted. The captain stood at the rail,")
                .contains("watching the dark line of cloud building on the horizon.");
        assertEquals("Chapter II", result.get(2).label());
        assertThat(result.get(2).content())
                .contains("Night fell swiftly. The lanterns were lit and the watch was set, and")
                .contains("still the wind kept rising");
        assertEquals("Chapter III", result.get(3).label());
        assertThat(result.get(3).content())
                .contains("When dawn broke there was no land in sight, only grey water stretching")
                .contains("to every quarter of the compass.");
    }

    @TestFactory
    @Tag("manual")
    @Timeout(value = 10, unit = TimeUnit.MINUTES)
    Stream<DynamicTest> splitIntoChapters_novels_aiDetection() throws IOException {
        final List<Map<String, Object>> entries = loadNovelTestEntries();
        if (entries.isEmpty()) {
            return Stream.empty();
        }

        return entries.stream()
                .filter(entry -> {
                    final Boolean skip = (Boolean) entry.getOrDefault("skip", false);
                    final String fileName = (String) entry.get("fileName");
                    final boolean available = getClass().getClassLoader()
                            .getResource("testDocuments/novels/" + fileName) != null;
                    if (!available) {
                        System.out.println("Skipping (not found): " + fileName);
                        return false;
                    } else if (skip) {
                        System.out.println("Skipping (disable): " + fileName);
                        return false;
                    }

                    return true;
                })
                .map(entry -> {
                    final String fileName = (String) entry.get("fileName");
                    return DynamicTest.dynamicTest("splitIntoChapters: " + fileName, () -> {
                        final String fullBook = loadNovelResource("testDocuments/novels/" + fileName);
                        final List<Chapter> chapters = chapterService.splitIntoChapters(fullBook, null);

                        System.out.println("=== " + fileName + " ===");
                        System.out.println("Total chapters detected: " + chapters.size());
                        for (int i = 0; i < chapters.size(); i++) {
                            final Chapter ch = chapters.get(i);
                            System.out.printf("  [%d] label=%-30s contentLength=%d%n",
                                    i, ch.label(), ch.content().length());
                        }

                        // read expectations from test file
                        final int chaptersGreaterThan = (int) entry.getOrDefault("chaptersGreaterThan", 1);


                        assertTrue(chapters.size() > chaptersGreaterThan,
                                "%s: expected >%d chapters, got %d"
                                        .formatted(fileName, chaptersGreaterThan, chapters.size()));

                        final long emptyChapters = chapters.stream()
                                .filter(ch -> ch.content().trim().length() < 100)
                                .count();
                        System.out.println("Chapters with content < 100 chars: " + emptyChapters);
                        final int emptyChaptersLessThan = (int) entry.getOrDefault("emptyChaptersLessThan", 5);
                        assertTrue(emptyChapters < emptyChaptersLessThan,
                                "%s: expected <%d near-empty chapters, got %d"
                                        .formatted(fileName, emptyChaptersLessThan, emptyChapters));

                        final long substantialChapters = chapters.stream()
                                .filter(ch -> ch.content().trim().length() > 500)
                                .count();
                        System.out.println("Chapters with content > 500 chars: " + substantialChapters);
                        final int substantialGreaterThan = (int) entry.getOrDefault("substantialChaptersGreaterThan", 1);
                        assertTrue(substantialChapters > substantialGreaterThan,
                                "%s: expected >%d substantial chapters, got %d"
                                        .formatted(fileName, substantialGreaterThan, substantialChapters));
                    });
                });
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> loadNovelTestEntries() throws IOException {
        try (final InputStream is = getClass().getClassLoader()
                .getResourceAsStream("testDocuments/novels/ChapterServiceTest.yaml")) {
            if (is == null) {
                System.out.println("ChapterServiceTest.yaml not found, skipping novel tests");
                return Collections.emptyList();
            }
            final ObjectMapper yaml = new ObjectMapper(new YAMLFactory());
            final Map<String, Object> root = yaml.readValue(is, Map.class);
            return (List<Map<String, Object>>) root.getOrDefault("splitIntoChapters", Collections.emptyList());
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

    @Test
    void summarizeChapters_shouldDynamicChaptersInResourceDir() throws IOException, URISyntaxException {
        final var testDocumentsUrl = getClass().getClassLoader().getResource("testDocuments");
        final var testDocumentsDir = Paths.get(testDocumentsUrl.toURI());

        try (final var fileStream = Files.walk(testDocumentsDir)) {
            final var chapterFiles = fileStream
                    .filter(Files::isRegularFile)
                    .toList();

            for (final var file : chapterFiles) {
                final String content = Files.readString(file, StandardCharsets.UTF_8);
                final long startTime = System.currentTimeMillis();
                System.out.println("Start summarising: " + file.getFileName());
                System.out.println("Size: " + content.length());
                final var summary = chapterSummaryAiServiceDirect.summarize(file.getFileName().toString(), content);
                System.out.println("Done summarising: " + file.getFileName());
                System.out.println("Time taken: " + (System.currentTimeMillis() - startTime));
                System.out.println("Summary: " + summary);
            }
        }
    }
}
