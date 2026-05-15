package com.chriswininger.api.services;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

        assertEquals(true, chapters.get(0).content().contains("Some introductory text."));
        assertEquals(true, chapters.get(1).content().contains("Some text for chapter one."));
        assertEquals(true, chapters.get(2).content().contains("Some text for chapter two."));
        assertEquals(true, chapters.get(3).content().contains("Some text for chapter three."));
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

        System.out.println("!!! chapter: " + chp);
    }

    @Test
    void summarizeChapter_shouldProduceASummaryOfALongChapter() throws IOException {
        String content = new String(
                getClass().getClassLoader().getResourceAsStream("long-chapter-example.txt").readAllBytes(),
                StandardCharsets.UTF_8
        );

        final var chp = chapterService.summarizeChapter(new Chapter("Chapter 1", content));

        System.out.println("!!! chp: " + chp);
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
                final var summary = chapterService.summarizeChapter(new Chapter(file.getFileName().toString(), content));
                System.out.println("Done summarising: " + file.getFileName());
                System.out.println("Time taken: " + (System.currentTimeMillis() - startTime));
                System.out.println("Summary: " + summary);
            }
        }
    }
}
