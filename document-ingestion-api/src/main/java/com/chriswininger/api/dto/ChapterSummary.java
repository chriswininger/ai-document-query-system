package com.chriswininger.api.dto;

import dev.langchain4j.model.output.structured.Description;

import java.util.List;

public record ChapterSummary(
        @Description("A concise summary of the chapter's content")
        String summary,
        @Description("Names of characters who appear or are mentioned in this chapter")
        List<String> characters,
        @Description("Questions that a reader might have that this chapter answers")
        List<String> possibleQuestionsThisAnswers
) {
        public ChapterSummary withCharacters(final List<String> characters) {
                return new ChapterSummary(
                        this.summary(),
                        characters,
                        this.possibleQuestionsThisAnswers());
        }
}
