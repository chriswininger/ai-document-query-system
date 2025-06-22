import {configureStore} from "@reduxjs/toolkit";
import {documentsApi} from "../api/apiBase.tsx";
import {vectorApi} from "../api/vectorApi.tsx";
import vectorSearchPageReducer from "../pages/VectorSearch/vectorSearchPageSlice.tsx";
import chatPageReducer from "../pages/Chat/chatPageSlice.tsx";
import {chatApi} from "../api/chatApi.tsx";

export const store = configureStore({
  reducer: {
    [documentsApi.reducerPath]: documentsApi.reducer,
    [vectorApi.reducerPath]: vectorApi.reducer,
    [chatApi.reducerPath]: chatApi.reducer,
    vectorSearchPage: vectorSearchPageReducer,
    chatPage: chatPageReducer
  },
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware()
      .concat(documentsApi.middleware)
      .concat(vectorApi.middleware)
      .concat(chatApi.middleware)
})

// Infer the `RootState` and `AppDispatch` types from the store itself
export type RootState = ReturnType<typeof store.getState>
// Inferred type: {posts: PostsState, comments: CommentsState, users: UsersState}
export type AppDispatch = typeof store.dispatch
