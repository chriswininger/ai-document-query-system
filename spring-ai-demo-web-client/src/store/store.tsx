import {configureStore} from "@reduxjs/toolkit";
import {documentsApi} from "../api/apiBase.tsx";

export const store = configureStore({
  reducer: {
    [documentsApi.reducerPath]: documentsApi.reducer,
  },
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware().concat(documentsApi.middleware),
})


// Infer the `RootState` and `AppDispatch` types from the store itself
export type RootState = ReturnType<typeof store.getState>
// Inferred type: {posts: PostsState, comments: CommentsState, users: UsersState}
export type AppDispatch = typeof store.dispatch
