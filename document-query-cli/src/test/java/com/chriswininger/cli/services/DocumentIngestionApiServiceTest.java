package com.chriswininger.cli.services;

import com.chriswininger.client.ApiException;
import com.chriswininger.client.api.DocumentResourceApi;
import com.chriswininger.client.model.DocumentResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentIngestionApiServiceTest {

    @Mock
    DocumentResourceApi documentResourceApi;

    @InjectMocks
    DocumentIngestionApiService documentIngestionApiService;

    @Test
    void getDocuments_returnsDocumentsFromApi() throws ApiException {
        final DocumentResponse doc = DocumentResponse.builder()
                .id(1L)
                .title("Test Book")
                .build();
        when(documentResourceApi.listDocuments(false)).thenReturn(List.of(doc));

        final List<DocumentResponse> result = documentIngestionApiService.getDocuments();

        assertEquals(1, result.size());
        assertEquals(1L, result.getFirst().getId());
        assertEquals("Test Book", result.getFirst().getTitle());
        verify(documentResourceApi).listDocuments(false);
    }

    @Test
    void getDocuments_wrapsApiExceptionInRuntimeException() throws ApiException {
        when(documentResourceApi.listDocuments(false))
                .thenThrow(new ApiException(500, "Internal Server Error"));

        final RuntimeException thrown = assertThrows(
                RuntimeException.class,
                documentIngestionApiService::getDocuments
        );

        assertEquals("document-ingestion-api returned HTTP 500: Internal Server Error", thrown.getMessage());
    }
}
