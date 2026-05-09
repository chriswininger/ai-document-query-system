package com.chriswininger.api;

import com.chriswininger.api.dto.requests.SubmitDocumentRequest;
import com.chriswininger.api.dto.requests.SubmitDocumentResponse;
import com.chriswininger.api.services.ChapterService;
import com.chriswininger.api.services.SummarySearchService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.Objects;
import java.util.regex.Pattern;

@Path(ApiConstants.BASE_REST_V1)
public class DocumentResource {

    private static final Logger LOG = Logger.getLogger(DocumentResource.class);

    @Inject
    SummarySearchService summarySearchService;

    @Inject
    ChapterService chapterService;

    @POST
    @Path("/submit-document")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response submitDocument(SubmitDocumentRequest request) {
        final String body = request.document();
        LOG.infof("POST /rest/v1/submit-document hit — document size: %d bytes", body.length());

        final String summary = summarySearchService.findSummaries(body);

        LOG.infof("Found metasummary %s", summary);

        final Pattern chapterSplitPattern = getChapterSplitPattern(request);
        final var chapters = chapterService.splitIntoChapters(body, chapterSplitPattern);
        for (int i = 0; i < chapters.size(); i++) {
            final long startTime = System.currentTimeMillis();
            if ("Intro".equals(chapters.get(i).label())) {
                // skip intro
                continue;
            }

            LOG.infof("Start Summarizing Chapter: %s -- %s", chapters.get(i).label(), chapters.get(i).label());
            final var chpSummary = chapterService.summarizeChapter(chapters.get(i));
            LOG.infof("Done Summarizing Chapter: %s -- %s -> too %s ms",
                    i, chapters.get(i).label(), System.currentTimeMillis() - startTime);
            LOG.infof("====== Chapter Summary =======\n%s\n=============", chpSummary);
        }

        //LOG.infof("!!! MUCH chp %s", chapters);
        return Response.accepted(new SubmitDocumentResponse("success", summary)).build();
    }

    private Pattern getChapterSplitPattern(SubmitDocumentRequest submitDocumentRequest) {
        final var pattern = submitDocumentRequest.chapterSplitPattern();

        if (Objects.isNull(pattern)) {
            return  null;
        } else {
            LOG.infof("Using chapterSplitPattern: %s", pattern);
            return Pattern.compile(pattern);
        }
    }
}
