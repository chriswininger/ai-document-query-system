package com.chriswininger.api.services;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentBySentenceSplitter;
import dev.langchain4j.model.chat.ChatModel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.Arrays;
import java.util.List;

@ApplicationScoped
public class SummarySearchService {

    private static final Logger LOG = Logger.getLogger(SummarySearchService.class);

    @Inject
    ChatModel chatModel;

    @Inject
    SummarySearchAiService summarySearchAiService;

    // TODO, extract title in summary, include file name at front half
    public String findSummaries(String plainText) {
        Document document = Document.from(plainText);

        DocumentBySentenceSplitter sentenceSplitter = new DocumentBySentenceSplitter(Integer.MAX_VALUE, 0);
        List<String> sentences = Arrays.stream(sentenceSplitter.split(document.text()))
                .filter(s -> !s.isBlank())
                .toList();

        String front = takeSentencesFromFront(sentences, 30);
        String back = takeSentencesFromBack(sentences, 30);

        LOG.debugf("=== Front 30 sentences ===%n%s", front);
        LOG.debugf("=== Back 30 sentences ===%n%s", back);

        SummarySearchAiService.SummaryAnalysis frontAnalysis = summarySearchAiService.analyze(front);
        SummarySearchAiService.SummaryAnalysis backAnalysis = summarySearchAiService.analyze(back);

        LOG.infof("=== Front analysis: hasSummaryInformation=%b, isCutOff=%b",
                frontAnalysis.hasSummaryInformation(), frontAnalysis.isCutOff());
        LOG.infof("=== Back analysis:  hasSummaryInformation=%b, isCutOff=%b",
                backAnalysis.hasSummaryInformation(), backAnalysis.isCutOff());

        String combinedMeta = buildCombinedMeta(frontAnalysis, backAnalysis);
        if (combinedMeta == null) {
            LOG.info("=== No summary information found in front or back matter");
            return null;
        }

        String summary = summarySearchAiService.summaryFromMetaContents(combinedMeta);
        LOG.infof("=== Summary ===%n%s", summary);
        return summary;
    }

    private String buildCombinedMeta(
            SummarySearchAiService.SummaryAnalysis frontAnalysis,
            SummarySearchAiService.SummaryAnalysis backAnalysis) {
        boolean hasFront = frontAnalysis.hasSummaryInformation() && !frontAnalysis.summaryInformation().isBlank();
        boolean hasBack = backAnalysis.hasSummaryInformation() && !backAnalysis.summaryInformation().isBlank();

        if (!hasFront && !hasBack) {
            return null;
        }
        if (hasFront && hasBack) {
            return frontAnalysis.summaryInformation() + "\n\n" + backAnalysis.summaryInformation();
        }
        return hasFront ? frontAnalysis.summaryInformation() : backAnalysis.summaryInformation();
    }

    public void testService() {
        findSummaries(
            "This is sentence one. This is sentence two. This is sentence three. " +
            "This is sentence four. This is sentence five. This is sentence six. " +
            "This is sentence seven. This is sentence eight. This is sentence nine. " +
            "This is sentence ten. This is sentence eleven. This is sentence twelve."
        );
    }

    private String takeSentencesFromFront(List<String> sentences, int count) {
        return String.join(" ", sentences.subList(0, Math.min(count, sentences.size())));
    }

    private String takeSentencesFromBack(List<String> sentences, int count) {
        int size = sentences.size();
        return String.join(" ", sentences.subList(Math.max(0, size - count), size));
    }
}
