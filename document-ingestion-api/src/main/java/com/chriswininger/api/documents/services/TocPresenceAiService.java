package com.chriswininger.api.documents.services;

import dev.langchain4j.model.output.structured.Description;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface TocPresenceAiService {

    @Description("Result of analyzing the front matter of a document to determine if it contains a Table of Contents")
    record TocPresenceResult(
            @Description("True if the text contains a Table of Contents section (a list of chapter or section titles, "
                    + "optionally with page numbers, appearing before the main body of the document). "
                    + "False if no such section is present.")
            boolean hasToc
    ) {}

    @SystemMessage("""
            You are a document structure analyst. You will be given text from the beginning of a document.
            Your only task is to determine whether the document contains a Table of Contents.

            A Table of Contents is a list of chapter or section titles — sometimes with page numbers —
            that appears before the main body of the document. It may be introduced by a heading such as
            "Contents", "Table of Contents", or similar. The entries are short title lines, not narrative prose.

            Examples of Table of Contents indicators:
            - A heading "Contents" or "Table of Contents" followed by a list of chapter names
            - Numbered entries like "1. Chapter One", "2. Chapter Two" listing section names
            - A series of short lines like "Chapter I. The Beginning" with no following paragraphs

            IMPORTANT — ebook navigation menus are NOT a Table of Contents:
            Some ebooks begin with a short navigation menu like:

                Begin Reading
                Table of Contents
                About the Author
                Copyright Page

            Even though the phrase "Table of Contents" appears there, this is just a navigation link,
            not an actual Table of Contents. A real Table of Contents must be followed by actual
            chapter or section title entries (e.g. "Chapter 1. The Beginning", "Part I", etc.).
            If "Table of Contents" appears as a single standalone line with no chapter or section
            listings beneath it, return hasToc: false.

            A document does NOT have a Table of Contents if it begins directly with narrative prose
            (long sentences and paragraphs) or chapter headings followed immediately by prose content.

            Respond only with valid JSON matching the required schema.
            """)
    @UserMessage("Analyze the following text from the beginning of a document:\n\n{{it}}")
    TocPresenceResult detect(String documentStart);
}
