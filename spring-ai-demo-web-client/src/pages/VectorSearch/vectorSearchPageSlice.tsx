import {createSlice} from "@reduxjs/toolkit";
import {DocumentImport} from "../../api/apiBase.tsx";

const vectorSearchPageSlice = createSlice({
  name: 'vectorSearchPage',
  initialState: getInitialState(),
  reducers: {
    queryUpdated: (state, action) => {
      state.query = action.payload;
    },
    numResultsUpdated: (state, action) => {
      state.numResults = action.payload;
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
})

export const { queryUpdated, numResultsUpdated, documentSelected, documentUnSelected } =
  vectorSearchPageSlice.actions;
export default vectorSearchPageSlice.reducer;

interface InitialState {
  selectedDocuments: DocumentImport [],
   query: string,
   numResults: number
}
function getInitialState(): InitialState {
  return {
    selectedDocuments: [],
    query: "",
    numResults: 10
  }
}
