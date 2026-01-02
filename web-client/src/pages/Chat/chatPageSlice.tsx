import {createSlice } from "@reduxjs/toolkit";
import {ChatResponse} from "../../api/chatApi.tsx";
import {DocumentImport} from "../../api/apiBase.tsx";

const chatPageSlice = createSlice({
  name: 'chatPageSlice',
  initialState: getInitialState(),
  reducers: {
    systemPromptUpdated: (state, action) => {
      state.systemPrompt = action.payload;
    },
    userPromptUpdated: (state, action) => {
      state.userPrompt = action.payload;
    },
    conversationIdUpdated: (state, action) => {
      state.conversationId = action.payload
    },
    conversationExchanged: (state, action) => {
      state.conversation = [...state.conversation, action.payload]
    },
    conversationCleared: (state) => {
      state.conversation = []
      state.conversationId = undefined
    },
    documentSelected: (state, action) => {
      state.selectedDocuments = [
        ...state.selectedDocuments,
        action.payload
      ]
    },
    documentUnSelected: (state, action) => {
      state.selectedDocuments = state.selectedDocuments
        .filter(d => d.id !== action.payload.id)
    },
    numberOfRagDocumentsToIncludeUpdated: (state, action) => {
      state.numberOfRagDocumentsToInclude = action.payload;
    }
  }
});

function getInitialState(): ChatPageState {
  return  {
    systemPrompt: getDefaultSystemPrompt(),
    userPrompt: "",
    conversationId: undefined,
    conversation: [],
    selectedDocuments: [],
    numberOfRagDocumentsToInclude: 5
  }
}

export function getDefaultSystemPrompt() {
  return `You are a helpful assistant. If you do not know something you simply say so.

Answer the question that follows:
`;
}

export const {
  userPromptUpdated,
  systemPromptUpdated,
  conversationIdUpdated,
  conversationExchanged,
  documentUnSelected,
  documentSelected,
  conversationCleared,
  numberOfRagDocumentsToIncludeUpdated
} = chatPageSlice.actions;

export default chatPageSlice.reducer;

export type ChatPageState = {
  systemPrompt: string,
  userPrompt: string,
  conversationId?: number,
  conversation: ChatResponse []
  selectedDocuments: DocumentImport [],
  numberOfRagDocumentsToInclude: number
}
