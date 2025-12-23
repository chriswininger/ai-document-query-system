import {useSelector} from "react-redux";
import {RootState} from "../store/store.tsx";

export const useSelectedDocuments = () => useSelector((state: RootState) => state.chatPage.selectedDocuments);
export const useSystemPrompt = () => useSelector((state: RootState) => state.chatPage.systemPrompt);
export const useNumberOfRagDocumentsToInclude = () => useSelector((state: RootState) => state.chatPage.numberOfRagDocumentsToInclude);
export const useUserPrompt = () => useSelector((state: RootState) => state.chatPage.userPrompt);
export const useConversationId = () => useSelector((state: RootState) => state.chatPage.conversationId);
