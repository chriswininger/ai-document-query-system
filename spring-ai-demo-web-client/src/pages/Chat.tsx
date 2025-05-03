import {useRef, useState} from 'react'
import './Chat.css'
import {
  doPostRequest, getErrorRequestState,
  getInitialRequestState,
  getLoadingRequestState,
  getSuccessRequestState,
} from "../requests/useMakeRequest.tsx";
import PromptResult from "../components/PromptResult.tsx";
import {ChatRequestResult} from "../types/ChatRequestResult.tsx";


export default function Chat() {
  const defaultSystemPrompt = `You are a helpful assistant. You are confident in your answers. Your answers are short and to the point.
If you do not know something you simply say so. Please do not explain your thinking, just answer the
question.
  `;

// variant: Вы научный ассистент важного русского профессора. Вы всегда отвечаете на очень академическом русском языке.
//
  const [inProgressPrompt, setInProgressPrompt] = useState("");
  const [systemPrompt, setSystemPrompt] = useState(defaultSystemPrompt);

  const [promptRequest, setPromptRequest] = useState(getInitialRequestState<ChatRequestResult>());

  const [conversation, setConversation] = useState<ChatRequestResult []>([])
  const [conversationId, setConversationId] = useState<number | null>(null)

  const bottomRef = useRef<HTMLSpanElement>(null);

  return (
    <main className="chat-page">
      <label htmlFor="system-prompt">System Prompt:</label>

      <textarea
        name="system-prompt"
        value={systemPrompt}
        className="post-input system-prompt-input"
        onChange={(e) => setSystemPrompt(e.target.value)}
      />


      <div className="conversation-area conversation-area-test">
        { conversation
          .map(exchange => <ConversationExchange exchange={exchange} />)
        }

        <span ref={bottomRef}></span>
      </div>

      <div className="post-area">
        <textarea
          name="prompt"
          value={inProgressPrompt}
          className="post-input prompt"
          onChange={(e) => setInProgressPrompt(e.target.value)}
        />

        <button
          className="send-prompt-btn"
          name="send-prompt"
          onClick={sendPromptClicked}
        >
          Post
        </button>
      </div>

      <div>
        <div>
          <PromptResult {...promptRequest} />
        </div>
      </div>
    </main>
  )

  async function sendPromptClicked() {
    setPromptRequest(getLoadingRequestState<ChatRequestResult>())

    try {
      const url = '/api/v1/chat/with-jack'
      const result: ChatRequestResult = await doPostRequest(
        url,
        { userPrompt: inProgressPrompt, conversationId }
      )

      setPromptRequest(getSuccessRequestState<ChatRequestResult>(result))
      setConversation([...conversation, result])
      setConversationId(result.conversationId)

      setInProgressPrompt("");

      setTimeout(() => {
        bottomRef.current?.scrollIntoView({behavior: 'smooth'});
      }, 0);
    } catch (error) {
      // @ts-expect-error error will likely have a message
      const message = error.message ? error.message : 'unknown error';
      setPromptRequest(getErrorRequestState(message))
    }
  }
}

function ConversationExchange({ exchange }: { exchange: ChatRequestResult}) {
  return <>
    <div>
      <label className="exchange-user-label">User: </label>

      <div>
        <pre className="exchange-value">{exchange.prompt}</pre>
      </div>
    </div>
    <div>
      <label className="exchange-user-label">Bot: </label>
      <div>
          <pre className="exchange-value">
            {exchange.response}
          </pre>
      </div>
    </div>
  </>;
}
