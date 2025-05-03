import {createApi, fetchBaseQuery} from '@reduxjs/toolkit/query/react'

export const documentsApi = createApi({
  reducerPath: 'documentsApi',
  baseQuery: fetchBaseQuery({ baseUrl: "/api/v1/rag/document" }),
  endpoints: (build) =>({
    getAllImportedDocuments: build.query<DocumentImport, void>({
      query: () => '/imported/all'
    })
  })
})

export interface DocumentImport {
  id: number,
  sourceName: string,
  nonChunkedContent: string,
  metadata: string,
  createdAt: number,
  updatedAt: number
}

export const { useGetAllImportedDocumentsQuery } = documentsApi;
