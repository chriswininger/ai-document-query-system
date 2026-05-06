package com.chriswininger.api.services;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChapterServiceTest {

    private final ChapterService chapterService = new ChapterService();

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
        assertEquals(true, chapters.get(0).content().contains("Some text for chapter one."));
        assertEquals(true, chapters.get(1).content().contains("Some text for chapter two."));
        assertEquals(true, chapters.get(2).content().contains("Some text for chapter three."));
    }
}
