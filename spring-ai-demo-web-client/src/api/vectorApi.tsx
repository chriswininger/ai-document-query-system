import {createApi, fetchBaseQuery} from '@reduxjs/toolkit/query/react'

export const vectorApi = createApi({
  reducerPath: 'vectorApi',
  baseQuery: fetchBaseQuery({baseUrl: '/api/v1/rag/vectors'}),
  endpoints: (build) => ({
    performSearch:  build.mutation<VectorSearchResult[], VectorSearchRequest>({
      query: (request) => ({
        url: '/search',
        method: 'POST',
        body: request
      })
    })
  })
})

export interface VectorSearchRequest {
  query: string,
  numMatches: number,
  documentSourceIds: number[]
}

export interface VectorSearchResult {
  text: string,
  metadata: Map<string, unknown>,
  score: null
}

 export const { usePerformSearchMutation } = vectorApi;
