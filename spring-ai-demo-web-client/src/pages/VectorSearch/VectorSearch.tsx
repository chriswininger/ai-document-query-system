import './VectorSearch.css'
import {useState} from "react";
import DocumentList from "../../components/DocumentList/DocumentList.tsx";
import {usePerformSearchMutation, VectorSearchResult} from "../../api/vectorApi.tsx";
import {useDispatch, useSelector} from "react-redux";
import {AppDispatch, RootState} from "../../store/store.tsx";
import {documentSelected, documentUnSelected, queryUpdated} from "./vectorSearchPageSlice.tsx";
import {DocumentImport} from "../../api/apiBase.tsx";

export default function VectorSearch() {
  const dispatch = useDispatch<AppDispatch>();

  const [searchValue, setSearchValue] = useState("")
  const query = useQuery();
  const numResults = useNumResults();
  const selectedDocuments = useSelectedDocuments();

  const [performSearch, { data, isLoading, error }] = usePerformSearchMutation();

  console.log('!!! query: ' + query)
  console.log('!!! numResults: ' + numResults)
  console.log('!!! selectedDocuments: ' + selectedDocuments)

  return <main className="vector-search-page">
    <DocumentList selectedDocuments={selectedDocuments} onDocumentSelected={onDocumentSelected} />

    <div className="vector-search-area">
      <section className="search-controls">
        <input
          className="post-input vector-search-input"
          placeholder="Search"
          value={searchValue}
          onChange={(e) => updateSearchValue(e.target.value)}
        />

        <button className="search-button" onClick={onSearchClicked}>Go</button>
      </section>

      <section className="search-results">
        { isLoading
          ? <p role="status">Loading...</p>
          : error
            ? <p className="error" role="alert">Error</p>
            : data?.map(searchResult => <SearchResultsItem searchResult={searchResult} />)
        }
      </section>
    </div>
  </main>

  function onSearchClicked() {
    const documentIds = selectedDocuments.map(d => d.id)
    performSearch({ query: searchValue, numMatches: 10, documentSourceIds: documentIds })
  }

  function updateSearchValue(newValue: string) {
    setSearchValue(newValue);
    dispatch(queryUpdated(newValue));
  }

  function onDocumentSelected(document: DocumentImport, newValue: boolean) {
    if (newValue) {
      dispatch(documentSelected(document))
    } else {
      dispatch(documentUnSelected(document))
    }
  }
}

function SearchResultsItem({ searchResult } : { searchResult: VectorSearchResult}) {
  return  (
    <div className="search-results-item">
      {searchResult.text}
    </div>
  )
}

const useQuery = () => useSelector((state: RootState) => state.vectorSearchPage.query);
const useNumResults = () => useSelector((state: RootState) => state.vectorSearchPage.numResults);
const useSelectedDocuments = () => useSelector((state: RootState) => state.vectorSearchPage.selectedDocuments);
