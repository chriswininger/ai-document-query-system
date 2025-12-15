import './StreamingChat.css'
import { useStreamingChat } from '../../api/streamingChatApi'

export default function StreamingChat() {
  const { streamChat, isLoading, error, streamedData } = useStreamingChat();

  const handleTestClick = () => {
    streamChat({
      userPrompt: 'Hello, this is a test prompt',
      documentSourceIds:[10]
    });
  };

  const answer = streamedData
    .filter(item => item.itemType == 'CONTENT')
    .map(entry => entry.output)
    .join('');

  const ragDocs = streamedData
    .filter(item => item.itemType == 'RAG_DOCUMENT');

  const isThinking = !!streamedData
    .find(item => item.itemType == 'THINKING') && answer.length == 0;

  return (
    <main className="streaming-chat-page">
      <button onClick={handleTestClick} disabled={isLoading}>
        'Test'
      </button>

      {isLoading && <p>Streaming...</p>}

      {error && (
        <div className="error">
          Error: {error instanceof Error ? error.message : String(error)}
        </div>
      )}

      <div className="streamed-content">
        { isThinking
          ? <div className="stream-item">
              <pre>Thinking...</pre>
            </div>
          : <div className="stream-item">
              <pre>{answer}</pre>
            </div>
        }

        {ragDocs.map((item, index) =>
          <div key={index} className="stream-item">
            <pre>{JSON.stringify(item, null, 2)}</pre>
          </div>
        )}
      </div>
    </main>
  )
}
