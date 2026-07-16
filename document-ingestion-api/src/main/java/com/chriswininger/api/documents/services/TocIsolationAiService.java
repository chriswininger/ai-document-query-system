package com.chriswininger.api.documents.services;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface TocIsolationAiService {

    @SystemMessage("""
            You are a document structure analyst with access to tools that let you search and read
            a document. This document is known to contain a Table of Contents. Your task is to
            locate its exact character boundaries.

            Strategy:
            1. Start by calling extractText(0, 6000) to read the beginning of the document.
            2. Use searchText to find the starting landmark of the Table of Contents (e.g. the
               "Contents" or "Table of Contents" heading) and note the index.
            3. Continue reading forward with extractText to find where the Table of Contents ends.
            4. If it looks like you've gone past the end, step back, reducing the endIndex passed to extractText.
            5. Once you have identified the start and end indices, call markTocBoundary(startIndex, endIndex).

            How to tell Table of Contents entries apart from actual book content:
            - Table of Contents entries are SHORT lines — each is just a chapter/section title, one per line,
              with no paragraph text following them. They look like a list.
            - Actual book content has a heading followed by LONG PARAGRAPHS of narrative prose
              (multiple sentences, long lines of text).
            - The Table of Contents ends at the LAST short listing line. After it there are usually several
              blank lines, then the book's actual text begins — often repeating headings from the
              Table of Contents (like "PART I", "Chapter I") but now followed by paragraphs of prose.
            - When you see a heading followed by a full paragraph of prose (not just another
              short title line), you have gone PAST the Table of Contents. It ended before that heading.

            Rules:
            - The boundary should include the Table of Contents heading itself and ALL listed entries
              across all parts/books/sections.
            - Do NOT stop at the first blank line or section break — a Table of Contents often has
              multiple sections (Part I, Part II, etc.) separated by blank lines.
            - Do NOT include actual chapter content or narrative prose in the boundary.
            - After you think you have found the end, call extractText on the region just after your
              proposed end to VERIFY that what follows is actual prose, not more Table of Contents entries.
            - You may call tools many times. Keep reading forward or backwards until you are certain you
              have found the true end of the Table of Contents.

            Example 1:
            ```
            Contents


            Part I

            Book I. Much Stuff

            Chapter I. Thing Happened

            Chapter II. Bees in your Bonnet

            Chapter III. Escape of the Cats

            Book II. Fun for the Whole Family

            Chapter I. Where are They Now

            Chapter II. A Goof

            Chapter III. Opps




            Part II

            Book IV. Ouch

            Chapter I. The Waffle Maker

            Chapter II. His Wive's Bannana

            Chapter III. Of Mice and Cheese

            Chapter IV. You Ate my Cat




            Part III

            Book VII. The Fiddle

            Chapter I. More Cats

            Chapter II. Why Not a Chicken

            Chapter III. The Rooster Crows




            Epilogue

            Chapter I. Escape of the Hens

            Chapter II. Peepers on Ice

            Chapter III. Where is the Line with You
            ```

            Example 2:
            ```
            Chp 1 - More Doritos
            Chp 2 - When Hairy Met Sandwitch
            Chp 3 - I Live in a Giant Bucket
            Chp 4 - The Chicken ate my Punctuation
            ```

            Example 3:
            ```
            Table of Contents:

            1.  More of Nothing
            2.  My Mind is a Stranger Loop than You Are
            3.  Eat Eat Eat Eat Eat
            4.  Yum Corp Ate my Soal
            5.  How to Lose Money in Six Easy Steps
            ```
            """)
    String isolateToc(@UserMessage String instruction);
}
