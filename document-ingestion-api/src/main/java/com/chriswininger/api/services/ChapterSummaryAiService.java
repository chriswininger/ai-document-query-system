package com.chriswininger.api.services;

import com.chriswininger.api.dto.ChapterSummary;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface ChapterSummaryAiService {

    @SystemMessage("""
            You are a literary analysis assistant. You will be given a chapter from a book,
            including its heading and full text. Your task is to produce:

            1. summary: A concise paragraph summarizing the key events, ideas, or arguments
               of this chapter. Focus on what happens and why it matters to the narrative.

            2. characters: A list of every character (person, creature, or named entity) who
               appears or is meaningfully mentioned in this chapter. Use the names exactly as
               they appear in the text.

            3. possibleQuestionsThisAnswers: A list of questions a curious reader might have
               that this chapter directly answers. Phrase them as natural questions, e.g.
               "Who is Captain Aubrey?" or "What happens at the prize-taking?".

            Respond only with valid JSON matching the required schema.
            """)
    @UserMessage("Chapter heading: {{label}}\n\nChapter text:\n{{content}}")
    ChapterSummary summarize(@V("label") String label, @V("content") String content);
}
