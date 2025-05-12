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
    documentSelected: (state, action) => {
      state.selectedDocuments = [
        ...state.selectedDocuments,
        action.payload
      ]
    },
    documentUnSelected: (state, action) => {
      state.selectedDocuments = state.selectedDocuments
        .filter(d => d.id !== action.payload.id)
    }
  }
});

function getInitialState(): ChatPageState {
  return  {
    systemPrompt: getDefaultSystemPrompt(),
    userPrompt: "",
    conversationId: undefined,
    conversation: [],
    selectedDocuments: []
  }
}

export function getDefaultSystemPrompt() {
  return `You are a helpful assistant. You are confident in your answers. Your answers are short and to the point.
If you do not know something you simply say so. Please do not explain your thinking, just answer the
question.`;
}

export const {
  userPromptUpdated,
  systemPromptUpdated,
  conversationIdUpdated,
  conversationExchanged,
  documentUnSelected,
  documentSelected
} = chatPageSlice.actions;

export default chatPageSlice.reducer;

export type ChatPageState = {
  systemPrompt: string,
  userPrompt: string,
  conversationId?: number,
  conversation: ChatResponse []
  selectedDocuments: DocumentImport []
}
