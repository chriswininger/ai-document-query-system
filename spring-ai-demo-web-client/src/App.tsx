import {useRef, useState} from 'react'
import './App.css'
import {
    doPostRequest, getErrorRequestState,
    getInitialRequestState,
    getLoadingRequestState,
    getSuccessRequestState,
} from "./requests/useMakeRequest.tsx";
import PromptResult from "./components/PromptResult.tsx";
import {ChatRequestResult} from "./types/ChatRequestResult.tsx";

function App() {
  const [inProgressPrompt, setInProgressPrompt] = useState("");
  const [promptRequest, setPromptRequest] = useState(getInitialRequestState<ChatRequestResult>());
  const [conversation, setConversation] = useState<ChatRequestResult []>([])
  const [conversationId, setConversationId] = useState<number | null>(null)

  const bottomRef = useRef<HTMLSpanElement>(null);

  return (
    <>
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
          className="post-input"
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
    </>
  )

  async function sendPromptClicked() {
      setPromptRequest(getLoadingRequestState<ChatRequestResult>())

      try {
          const url = '/api/v1/chat'
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

export default App


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
