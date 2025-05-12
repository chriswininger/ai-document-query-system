import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import {BrowserRouter, Route, Routes} from 'react-router'
import Chat from "./pages/Chat.tsx";
import TopNav from "./components/TopNav/TopNav.tsx";
import VectorSearch from "./pages/VectorSearch/VectorSearch.tsx";
import {Provider} from "react-redux";
import {store} from "./store/store.tsx";
import ChatPage from "./pages/Chat/ChatPage.tsx";

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <Provider store={store}>
      <BrowserRouter>
        <TopNav />
        <Routes>
          <Route path="/" element={<ChatPage />} />
          <Route path="/backup" element={<Chat />} />
          <Route path="vector-search" element={<VectorSearch />} />
        </Routes>
      </BrowserRouter>
    </Provider>
  </StrictMode>,
)
