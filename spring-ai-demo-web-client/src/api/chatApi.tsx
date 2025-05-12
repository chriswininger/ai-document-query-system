import {createApi, fetchBaseQuery} from "@reduxjs/toolkit/query/react";
import {VectorSearchResult} from "./vectorApi.tsx";

export const chatApi = createApi({
  reducerPath: 'chatApi',
  baseQuery: fetchBaseQuery({ baseUrl: '/api/v1/chat' }),
  endpoints: (build) => ({
    performPrompt:  build.mutation<ChatResponse, ChatRequest>({
      query: (request) => ({
        url: '/generic',
        method: 'POST',
        body: request
      })
    })
  })
})

export interface ChatRequest {
  userPrompt: string;
  conversationId?: number;
  systemPrompt?: string;
  documentSourceIds?: number[];
}

export interface ChatResponse {
  prompt: string;
  response: string;
  vectorSearchResults: VectorSearchResult[];
  model: string;
  conversationId: number;
  requestTimeStartTime: Date;
  requestEndTime: Date;
}

export const { usePerformPromptMutation } = chatApi;
export type PerformPromptMutationResponse = ReturnType<typeof chatApi.usePerformPromptMutation>[1];
