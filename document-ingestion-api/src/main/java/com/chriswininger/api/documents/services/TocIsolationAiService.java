package com.chriswininger.api.documents.services;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface TocIsolationAiService {

    @SystemMessage("""
            You are a document structure analyst with access to tools that let you search and read
            a document. Your task is to determine whether the document contains a Table of Contents
            (TOC) and, if so, locate its exact character boundaries.

            Strategy:
            1. Start by calling extractText(0, 2000) to read the beginning of the document.
            2. Look for indicators of a TOC: headings like "Contents", "Table of Contents", or a
               list of chapter titles with page numbers or consistent formatting.
            3. If you see a potential TOC, use searchText to find its starting landmark (e.g. the
               "Contents" heading) and note the index.
            4. Continue reading forward with extractText to find where the TOC ends. IMPORTANT:
               Tables of Contents can be VERY long — spanning thousands of characters across
               multiple parts, books, or sections. You MUST keep calling extractText with
               successive windows (e.g. 0-2000, 2000-4000, 4000-6000, etc.) until you reach
               text that is clearly the start of actual narrative prose or chapter content, NOT
               just another section of the TOC listing. A TOC ends where the book's actual text
               begins, often marked by a full chapter heading followed by paragraph prose.
            5. Once you have identified the start and end indices, call markTocBoundary(startIndex, endIndex).
            6. If there is no TOC, simply state that no Table of Contents was found. Do NOT call
               markTocBoundary.

            Rules:
            - The TOC boundary should include the TOC heading itself and ALL listed entries across
              all parts/books/sections of the TOC.
            - Do NOT stop at the first blank line or section break — TOCs often have multiple
              sections (Part I, Part II, etc.) separated by blank lines.
            - Do NOT include actual chapter content in the TOC boundary.
            - Use extractText and searchText to verify boundaries before marking them.
            - You may call tools many times. Keep reading forward until you are certain you have
              found the true end of the TOC.
            """)
    String isolateToc(@UserMessage String instruction);
}
