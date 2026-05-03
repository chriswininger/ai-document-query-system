package com.chriswininger.api.services;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;
import org.jboss.logging.Logger;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@ApplicationScoped
public class ChapterService {

    private static final Logger LOG = Logger.getLogger(ChapterService.class);

    private static final int[] SLICE_SIZES = {50, 75, 100};
    private static final int MAX_RETRIES = SLICE_SIZES.length;

    @Inject
    ChapterDetectionAiService chapterDetectionAiService;

    private CompiledGraph<ChapterState> graph;

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    public static class ChapterState extends AgentState {

        static final Map<String, Channel<?>> SCHEMA = Map.of(
                "documentText",  Channels.base(() -> ""),
                "sentences",     Channels.base(ArrayList::new),
                "retryCount",    Channels.base(() -> 0),
                "splitString",   Channels.base(() -> ""),
                "chapters",      Channels.base(ArrayList::new),
                "done",          Channels.base(() -> false)
        );

        public ChapterState(Map<String, Object> initData) {
            super(initData);
        }

        public String documentText()  { return this.<String>value("documentText").orElse(""); }
        public List<String> sentences() {
            return this.<List<String>>value("sentences").orElseGet(ArrayList::new);
        }
        public int retryCount()       { return this.<Integer>value("retryCount").orElse(0); }
        public String splitString()   { return this.<String>value("splitString").orElse(""); }
        public List<Chapter> chapters() {
            List<?> raw = this.<List<?>>value("chapters").orElse(null);
            if (raw == null || raw.isEmpty()) return null;
            return raw.stream()
                    .filter(e -> e instanceof Map)
                    .map(e -> {
                        @SuppressWarnings("unchecked")
                        Map<String, String> m = (Map<String, String>) e;
                        return new Chapter(m.get("label"), m.get("content"));
                    })
                    .toList();
        }
        public boolean done() { return this.<Boolean>value("done").orElse(false); }
    }

    // -------------------------------------------------------------------------
    // Graph construction
    // -------------------------------------------------------------------------

    @PostConstruct
    void buildGraph() {
        try {
            graph = new StateGraph<>(ChapterState.SCHEMA, ChapterState::new)
                    .addEdge(START, "detectPattern")
                    .addNode("detectPattern", node_async(this::detectPatternNode))
                    .addEdge("detectPattern", "trySplit")
                    .addNode("trySplit", node_async(this::trySplitNode))
                    .addEdge("trySplit", "validate")
                    .addNode("validate", node_async(state -> Map.of()))
                    .addConditionalEdges("validate", edge_async(state -> {
                        if (state.done() || state.chapters() != null) {
                            return END;
                        }
                        if (state.retryCount() >= MAX_RETRIES) {
                            return END;
                        }
                        return "detectPattern";
                    }), Map.of(
                            "detectPattern", "detectPattern",
                            END, END
                    ))
                    .compile();
        } catch (GraphStateException e) {
            throw new RuntimeException("Failed to compile chapter detection graph", e);
        }
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public List<Chapter> splitIntoChapters(String plainText) {
        List<String> sentences = tokenizeToSentences(plainText);

        Map<String, Object> initialState = Map.of(
                "documentText", plainText,
                "sentences", sentences,
                "retryCount", 0,
                "splitString", ""
        );

        return graph.invoke(initialState)
                .map(ChapterState::chapters)
                .orElse(null);
    }

    // -------------------------------------------------------------------------
    // Nodes
    // -------------------------------------------------------------------------

    private Map<String, Object> detectPatternNode(ChapterState state) {
        int retry = state.retryCount();
        int sliceSize = SLICE_SIZES[Math.min(retry, SLICE_SIZES.length - 1)];
        List<String> sentences = state.sentences();
        String frontSlice = String.join(" ", sentences.subList(0, Math.min(sliceSize, sentences.size())));

        LOG.infof("detectPattern: attempt %d, slice size %d sentences", retry + 1, sliceSize);

        ChapterDetectionAiService.ChapterPatternAnalysis analysis;
        String previousSplit = state.splitString();

        if (previousSplit == null || previousSplit.isBlank()) {
            analysis = chapterDetectionAiService.detectPattern(frontSlice);
        } else {
            LOG.infof("detectPattern: retrying, previous split string '%s' was not found", previousSplit);
            analysis = chapterDetectionAiService.retryDetectPattern(frontSlice, previousSplit);
        }

        LOG.infof("detectPattern: isChapterBook=%b, splitString='%s', examples=%s",
                analysis.isChapterBook(), analysis.splitString(), analysis.examples());

        if (!analysis.isChapterBook()) {
            return Map.of("done", true, "retryCount", retry + 1);
        }

        return Map.of(
                "splitString", analysis.splitString() != null ? analysis.splitString() : "",
                "retryCount", retry + 1
        );
    }

    private Map<String, Object> trySplitNode(ChapterState state) {
        String splitString = state.splitString();
        String documentText = state.documentText();

        if (splitString == null || splitString.isBlank()) {
            LOG.warn("trySplit: splitString is blank, skipping split");
            return Map.of();
        }

        if (!documentText.contains(splitString)) {
            LOG.infof("trySplit: splitString '%s' not found in document", splitString);
            return Map.of("splitString", "");
        }

        String[] parts = documentText.split(Pattern.quote(splitString), -1);

        if (parts.length < 2) {
            LOG.infof("trySplit: split produced fewer than 2 chunks");
            return Map.of("splitString", "");
        }

        List<Map<String, String>> chapters = new ArrayList<>();
        for (int i = 1; i < parts.length; i++) {
            String chunk = splitString + parts[i].stripLeading();
            String label = chunk.lines().findFirst().orElse("Chapter " + i).strip();
            chapters.add(Map.of("label", label, "content", chunk));
        }

        LOG.infof("trySplit: successfully split into %d chapters", chapters.size());
        return Map.of("chapters", chapters);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private List<String> tokenizeToSentences(String plainText) {
        dev.langchain4j.data.document.splitter.DocumentBySentenceSplitter splitter =
                new dev.langchain4j.data.document.splitter.DocumentBySentenceSplitter(Integer.MAX_VALUE, 0);
        return Arrays.stream(splitter.split(plainText))
                .filter(s -> !s.isBlank())
                .toList();
    }
}
