import {useState} from 'react'
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

  return (
    <>
      <div className="conversation-area">
        <textarea className="chat-window" value={getThread()} readOnly />
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
      } catch (error) {
          // @ts-expect-error error will likely have a message
          const message = error.message ? error.message : 'unknown error';
          setPromptRequest(getErrorRequestState(message))
      }
  }

  function getThread() {
    return conversation.map((result) => {
      return `
user: ${result.prompt}

bot: ${result.response}
      `
    }).join("\n\n")
  }
}

export default App
