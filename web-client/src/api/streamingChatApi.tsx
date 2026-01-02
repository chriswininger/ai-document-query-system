import { useState, useCallback } from 'react';
import { ChatRequest } from './chatApi';
import {VectorSearchResult} from "./vectorApi.tsx";

export interface ChatStreamingResponseItem {
  model: string;
  conversationId: number;
  itemType: 'CONTENT' | 'RAG_DOCUMENT' | 'THINKING' | 'META_DATA';
  output: string;
  vectorSearchResult: VectorSearchResult;
  totalTokensUsed: number | undefined;
  completionTokensUsed: number | undefined;
  promptTokensUsed: number | undefined;
  queryRewrite: string | undefined;
}

export function useStreamingChat() {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<any>(null);
  const [streamedData, setStreamedData] = useState<ChatStreamingResponseItem[]>([]);

  const streamChat = useCallback(async (
    request: ChatRequest,
    onComplete?: (data: ChatStreamingResponseItem[]) => void
  ) => {
    setIsLoading(true);
    setError(null);
    setStreamedData([]);

    // Track accumulated data for the callback
    const accumulatedData: ChatStreamingResponseItem[] = [];
    let streamError: any = null;

    try {
      const response = await fetch('/api/v1/chat/generic/stream', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(request),
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const reader = response.body?.getReader();
      const decoder = new TextDecoder();

      if (!reader) {
        throw new Error('No reader available');
      }

      let buffer = '';

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        // Decode the chunk and add to buffer
        buffer += decoder.decode(value, { stream: true });

        // Process complete SSE messages (separated by \n\n)
        let newlineIndex;
        while ((newlineIndex = buffer.indexOf('\n\n')) !== -1) {
          const message = buffer.slice(0, newlineIndex);
          buffer = buffer.slice(newlineIndex + 2);

          if (message.trim()) {
            // Parse SSE message - look for data: lines
            const lines = message.split('\n');
            for (const line of lines) {
              if (line.startsWith('data:')) {
                const jsonStr = line.slice(5).trim(); // Remove 'data:' prefix and trim
                if (jsonStr) {
                  try {
                    const data: ChatStreamingResponseItem = JSON.parse(jsonStr);

                    accumulatedData.push(data);
                    setStreamedData(prev => [...prev, data]);
                  } catch (e) {
                    console.error('Failed to parse SSE data:', e, 'Raw:', jsonStr);
                  }
                }
              }
            }
          }
        }
      }

      // Process any remaining buffer after stream ends
      if (buffer.trim()) {
        const lines = buffer.split('\n');
        for (const line of lines) {
          if (line.startsWith('data:')) {
            const jsonStr = line.slice(5).trim();
            if (jsonStr) {
              try {
                const data: ChatStreamingResponseItem = JSON.parse(jsonStr);
                accumulatedData.push(data);
                setStreamedData(prev => [...prev, data]);
              } catch (e) {
                console.error('Failed to parse final SSE data:', e, 'Raw:', jsonStr);
              }
            }
          }
        }
      }
    } catch (err) {
      console.error('Streaming error:', err);
      streamError = err;
      setError(err);
    } finally {
      setIsLoading(false);
      // Call onComplete callback if stream completed successfully (no error)
      if (!streamError && onComplete) {
        onComplete(accumulatedData);
      }
    }
  }, []);

  const reset = useCallback(() => {
    setStreamedData([]);
    setError(null);
  }, []);

  return { streamChat, isLoading, error, streamedData, reset };
}
