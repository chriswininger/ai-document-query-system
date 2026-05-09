package com.chriswininger.api.services;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class ChapterServiceTest {

    @Inject
    ChapterService chapterService;

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
}
