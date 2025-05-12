import './ChatPage.css';
import PromptResult from "./PromptResult.tsx";
import {useDispatch, useSelector} from "react-redux";
import {AppDispatch, RootState} from "../../store/store.tsx";
import {useRef} from "react";
import {
  conversationExchanged,
  conversationIdUpdated, documentSelected, documentUnSelected,
  systemPromptUpdated,
  userPromptUpdated
} from "./chatPageSlice.tsx";
import {usePerformPromptMutation} from "../../api/chatApi.tsx";
import {ConversationExchange} from "./ConversationExchange.tsx";
import DocumentList from "../../components/DocumentList/DocumentList.tsx";
import {DocumentImport} from "../../api/apiBase.tsx";

export default function ChatPage() {
  const conversation = useConversation();
  const conversationId = useConversationId();
  const userPrompt = useUserPrompt();
  const systemPrompt = useSystemPrompt();
  const selectedDocuments = useSelectedDocuments();

  const bottomRef = useRef<HTMLSpanElement>(null);

  const dispatch = useDispatch<AppDispatch>();

  const [performPrompt, result] = usePerformPromptMutation();

  return (
    <main className="chat-page">
      <DocumentList selectedDocuments={selectedDocuments} onDocumentSelected={onDocumentSelected} />

      <div className="conversation-exchange-area">
        <label htmlFor="system-prompt">System Prompt:</label>

        <textarea
          name="system-prompt"
          value={systemPrompt}
          className="post-input system-prompt-input"
          onChange={(e) => dispatch(systemPromptUpdated(e.target.value))}
        />

        <div className="conversation-area conversation-area-test">
          { conversation
            .map(exchange => <ConversationExchange key={`${exchange.requestEndTime}`} exchange={exchange} />)
          }

          <span ref={bottomRef}></span>
        </div>

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

        <div>
          <div>
            <PromptResult {...result} />
          </div>
        </div>
      </div>
    </main>
  )

  async function sendPromptClicked() {
    const result = await performPrompt({ userPrompt, systemPrompt, conversationId })

    if (result.data) {
      dispatch(conversationExchanged(result.data));
      dispatch(conversationIdUpdated(result.data.conversationId));
    }
  }

  function onDocumentSelected(document: DocumentImport, newValue: boolean) {
    if (newValue) {
      dispatch(documentSelected(document));
    } else {
      dispatch(documentUnSelected(document));
    }
  }
}

const useUserPrompt = () => useSelector((state: RootState) => state.chatPage.userPrompt);
const useSystemPrompt = () => useSelector((state: RootState) => state.chatPage.systemPrompt);
const useConversationId = () => useSelector((state: RootState) => state.chatPage.conversationId);
const useConversation = () => useSelector((state: RootState) => state.chatPage.conversation);
const useSelectedDocuments = () => useSelector((state: RootState) => state.chatPage.selectedDocuments);
