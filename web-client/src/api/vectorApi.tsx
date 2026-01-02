import {createApi, fetchBaseQuery} from '@reduxjs/toolkit/query/react'

export const vectorApi = createApi({
  reducerPath: 'vectorApi',
  baseQuery: fetchBaseQuery({baseUrl: '/api/v1/rag/vectors'}),
  endpoints: (build) => ({
    performSearch:  build.mutation<VectorSearchResponse, VectorSearchRequest>({
      query: (request) => {
        const params = new URLSearchParams();
        if (request.useRAGRewrite !== undefined) {
          params.append('useRAGRewrite', String(request.useRAGRewrite));
        }
        const queryString = params.toString();
        return {
          url: `/search${queryString ? `?${queryString}` : ''}`,
          method: 'POST',
          body: {
            query: request.query,
            numMatches: request.numMatches,
            documentSourceIds: request.documentSourceIds
          }
        }
      }
    })
  })
})

export interface VectorSearchRequest {
  query: string,
  numMatches: number,
  documentSourceIds: number[],
  useRAGRewrite?: boolean
}

export interface VectorSearchResult {
  text: string,
  metadata: Map<string, unknown>,
  score: number | null
}

export interface VectorSearchResponse {
  rewrittenQuery: string,
  searchResults: VectorSearchResult[]
}

export const { usePerformSearchMutation } = vectorApi;
export type PerformSearchMutationResponse = ReturnType<typeof vectorApi.usePerformSearchMutation>[1];
