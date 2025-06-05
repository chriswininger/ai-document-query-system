import './ChatPage.css';
import PromptResult from "./PromptResult.tsx";
import {useDispatch, useSelector} from "react-redux";
import {AppDispatch, RootState} from "../../store/store.tsx";
import {useRef} from "react";
import {
  conversationCleared,
  conversationExchanged,
  conversationIdUpdated,
  documentSelected,
  documentUnSelected, numberOfRagDocumentsToIncludeUpdated,
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
  const numberOfRagDocumentsToInclude = useNumberOfRagDocumentsToInclude();

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

        <label htmlFor="system-prnumberOfRagDocumentsToIncludeompt">Num RAG Documents to Include:</label>

        <input
          name="numberOfRagDocumentsToInclude"
          value={numberOfRagDocumentsToInclude}
          className="post-input"
          onChange={(e) => dispatch(numberOfRagDocumentsToIncludeUpdated(e.target.value))}
        />

        <div className="conversation-area">
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

        {
          conversationId !== undefined &&
          (
            <div className="conversation-exchange-area--conversation-instance-info">
              <span className="conversation-exchange-area--conversation-instance-info-id">
                Existing Conversation ID: {conversationId}
              </span>

              <button onClick={resetConversationClicked}>New Conversation</button>
            </div>
          )
        }
      </div>
    </main>
  )

  async function sendPromptClicked() {
    const result = await performPrompt({
      userPrompt,
      systemPrompt,
      conversationId,
      numberOfRagDocumentsToInclude,
      documentSourceIds: selectedDocuments.map(d => d.id)
    })

    if (result.data) {
      dispatch(conversationExchanged(result.data));
      dispatch(conversationIdUpdated(result.data.conversationId));
      dispatch(userPromptUpdated(''));

      setTimeout(() => {
        bottomRef.current?.scrollIntoView({behavior: 'smooth'});
      }, 0);
    }
  }

  function onDocumentSelected(document: DocumentImport, newValue: boolean) {
    if (newValue) {
      dispatch(documentSelected(document));
    } else {
      dispatch(documentUnSelected(document));
    }
  }

  function resetConversationClicked() {
    dispatch(conversationCleared());
  }
}

const useUserPrompt = () => useSelector((state: RootState) => state.chatPage.userPrompt);
const useSystemPrompt = () => useSelector((state: RootState) => state.chatPage.systemPrompt);
const useConversationId = () => useSelector((state: RootState) => state.chatPage.conversationId);
const useConversation = () => useSelector((state: RootState) => state.chatPage.conversation);
const useSelectedDocuments = () => useSelector((state: RootState) => state.chatPage.selectedDocuments);
const useNumberOfRagDocumentsToInclude = () => useSelector((state: RootState) => state.chatPage.numberOfRagDocumentsToInclude);
