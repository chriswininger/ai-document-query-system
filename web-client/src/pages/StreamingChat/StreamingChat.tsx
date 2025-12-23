import './StreamingChat.css'
import '../Chat/ChatPage.css'
import {ChatStreamingResponseItem, useStreamingChat} from '../../api/streamingChatApi'
import DocumentList from "../../components/DocumentList/DocumentList.tsx";
import {
  documentSelected, documentUnSelected,
  numberOfRagDocumentsToIncludeUpdated,
  systemPromptUpdated,
  userPromptUpdated
} from "../Chat/chatPageSlice.tsx";
import {DocumentImport} from "../../api/apiBase.tsx";
import {useDispatch} from "react-redux";
import {AppDispatch} from "../../store/store.tsx";
import {
  useNumberOfRagDocumentsToInclude,
  useSelectedDocuments,
  useSystemPrompt,
  useUserPrompt
} from "../../api/selectors.tsx";
import {useState} from "react";
import {ChatResponse} from "../../api/chatApi.tsx";
import {ConversationExchange} from "../Chat/ConversationExchange.tsx";

export default function StreamingChat() {
  const { streamChat, isLoading, error, streamedData } = useStreamingChat();
  const dispatch = useDispatch<AppDispatch>();

  const selectedDocuments = useSelectedDocuments();
  const systemPrompt = useSystemPrompt();
  const numberOfRagDocumentsToInclude = useNumberOfRagDocumentsToInclude();
  const userPrompt = useUserPrompt();

  const answer = getAnswer(streamedData);
  const isThinking = isStillThinking();


  // TODO: Move to Redux
  const [pastResponses, setPastResponses] = useState<ChatResponse []>([]);
  const [streamingConversationId, setStreamingConversationId] = useState<number | undefined>(undefined);
  const [metaDataFromLastStream, setMetaDataFromLastStream] = useState<FinalMetadata | undefined>();
  const [streamStartTime, setStreamStartTime] = useState<Date | undefined>()

  if (metaDataFromLastStream) {
    // TODO: Display this somewhere
    console.log('metadata: ', metaDataFromLastStream)
  }

  return (
    <main className="chat-page">
      <DocumentList selectedDocuments={selectedDocuments} onDocumentSelected={onDocumentSelected} />

      <div className="conversation-exchange-area">
        <label htmlFor="system-prompt">System Prompt:</label>

        <textarea
          name="system-prompt"
          value={systemPrompt}
          className="post-input system-prompt-input"
          onChange={(e) =>
            dispatch(systemPromptUpdated(e.target.value))}
        />

        <label htmlFor="system-prnumberOfRagDocumentsToIncludeompt">Num RAG Documents to Include:</label>

        <input
          name="numberOfRagDocumentsToInclude"
          value={numberOfRagDocumentsToInclude}
          className="post-input"
          onChange={(e) => dispatch(numberOfRagDocumentsToIncludeUpdated(e.target.value))}
        />

        {pastResponses.map(resp => <ConversationExchange exchange={resp} />)}

        {/*TODO separate this userPrompt from the current user prompt*/}
        {/* In progress Streaming Response */}
        <StreamingConversationExchange
          prompt={userPrompt}
          response={answer}
          isLoading={isLoading}
          error={error}
          isThinking={isThinking}
        />

        <div className="post-area">
          <textarea
            name="prompt"
            value={userPrompt}
            className="post-input prompt"
            onChange={(e) => dispatch(userPromptUpdated(e.target.value))}
          />

          <button
            className="send-prompt-btn"
            name="send-prompt"
            onClick={sendPromptClicked}
          >
            Post
          </button>
        </div>

        {
          streamingConversationId !== undefined &&
          (
            <div className="conversation-exchange-area--conversation-instance-info">
              <span className="conversation-exchange-area--conversation-instance-info-id">
                Existing Conversation ID: {streamingConversationId}
              </span>

              <button onClick={resetConversationClicked}>New Conversation</button>
            </div>
          )
        }
      </div>
    </main>
  )

  function onDocumentSelected(document: DocumentImport, newValue: boolean) {
    if (newValue) {
      dispatch(documentSelected(document));
    } else {
      dispatch(documentUnSelected(document));
    }
  }

  function getAnswer(responses: ChatStreamingResponseItem []): string {
    return responses
      .filter(item => item.itemType == 'CONTENT')
      .map(entry => entry.output)
      .join('');
  }

  function getThinking(responses: ChatStreamingResponseItem []): string {
    return responses
      .filter(item => item.itemType == 'THINKING')
      .map(entry => entry.output)
      .join('');
  }

  function getRagDocs(responses: ChatStreamingResponseItem []): ChatStreamingResponseItem [] {
     return responses
       .filter(item => item.itemType == 'RAG_DOCUMENT');
  }

  function getMetadata(response: ChatStreamingResponseItem[]): FinalMetadata | undefined {
    const metadata = response.find((item) => item.itemType === 'META_DATA');

    if (metadata) {
      return {
        completionTokensUsed: metadata.completionTokensUsed,
        promptTokensUsed: metadata.promptTokensUsed,
        totalTokensUsed: metadata.totalTokensUsed,
        model: metadata.model
      }
    } else {
      return  undefined;
    }
  }

  function getModel(responses: ChatStreamingResponseItem []): string {
    if (responses.length > 0) {
      return responses[0].model
    } else {
      return 'TBD'
    }
  }

  function sendPromptClicked() {
    setStreamStartTime(new Date());

    streamChat({
      systemPrompt,
      userPrompt,
      numberOfRagDocumentsToInclude,
      documentSourceIds: selectedDocuments.map(d => d.id),
      conversationId: streamingConversationId
    }, (completedData) => {
      // Stream completed successfully - add to past responses
      console.info('streaming response complete');

      const conversationId = completedData.length > 0 && completedData[0].conversationId
        ? completedData[0].conversationId
        : undefined;

      setStreamingConversationId(conversationId);
      setMetaDataFromLastStream(getMetadata(completedData));

      const response: ChatResponse = {
        prompt: userPrompt,
        response: getAnswer(completedData),
        thinking: getThinking(completedData),
        vectorSearchResults: getRagDocs(completedData).map(entry => entry.vectorSearchResult),
        conversationId,
        model: getModel(completedData),
        requestTimeStartTime: streamStartTime || new Date(),
        requestEndTime: new Date(),
      };

      setPastResponses(prev => [...prev, response]);
    });
  }

  function resetConversationClicked() {
    setStreamingConversationId(undefined);
    setPastResponses([]);
  }

  function isStillThinking(): boolean {
    return !!streamedData
      .find(item => item.itemType == 'THINKING') && answer.length == 0;
  }
}

function StreamingConversationExchange(
  { prompt, response, isLoading, isThinking, error }:
  { prompt: string, response: string, isLoading: boolean, isThinking: boolean, error: any }
) {
  if (!isLoading) {
    return null;
  }

  if (error) {
    return <p className="error" role="alert">
      Error processing request: {error?.status} -- {JSON.stringify(error?.data)}
    </p>
  }

  return <>
    <div>
      <label className="exchange-user-label">User: </label>

      <div>
        <pre className="exchange-value">{prompt}</pre>
      </div>
    </div>
    <div>
      <label className="exchange-user-label">Bot: </label>
      <div>️‍️
        <pre className="exchange-value">
          { isThinking ? 'thinking...' : response}
        </pre>
      </div>
    </div>
  </>;
}

interface FinalMetadata {
    completionTokensUsed: number | undefined;
    promptTokensUsed: number | undefined;
    totalTokensUsed: number | undefined;
    model: string | undefined;
}
