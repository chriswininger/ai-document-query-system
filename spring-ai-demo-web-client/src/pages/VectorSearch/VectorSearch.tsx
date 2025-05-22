import './VectorSearch.css'
import DocumentList from "../../components/DocumentList/DocumentList.tsx";
import {usePerformSearchMutation } from "../../api/vectorApi.tsx";
import {useDispatch, useSelector} from "react-redux";
import {AppDispatch, RootState} from "../../store/store.tsx";
import {documentSelected, documentUnSelected, numResultsUpdated, queryUpdated} from "./vectorSearchPageSlice.tsx";
import {DocumentImport} from "../../api/apiBase.tsx";
import SearchResultsItem from "../../components/SearchResultItem/SearchResultItem.tsx";

export default function VectorSearch() {
  const dispatch = useDispatch<AppDispatch>();

  const query = useQuery();
  const numResults = useNumResults();
  const selectedDocuments = useSelectedDocuments();

  const [performSearch, { data, isLoading, error }] = usePerformSearchMutation();

  return <main className="vector-search-page">
    <DocumentList selectedDocuments={selectedDocuments} onDocumentSelected={onDocumentSelected} />

    <div className="vector-search-area">
      <section className="search-controls">
        <input
          className="post-input vector-search-input"
          placeholder="Search"
          value={query}
          onChange={(e) => dispatch(queryUpdated(e.target.value))}
        />

        <input
          className="post-input vector-search-num-results-input"
          placeholder="number of results"
          value={numResults}
          onChange={(e) => dispatch(numResultsUpdated(parseInt(e.target.value)))}
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
    performSearch({ query, numMatches: numResults, documentSourceIds: documentIds })
  }

  function onDocumentSelected(document: DocumentImport, newValue: boolean) {
    if (newValue) {
      dispatch(documentSelected(document))
    } else {
      dispatch(documentUnSelected(document))
    }
  }
}


const useQuery = () => useSelector((state: RootState) => state.vectorSearchPage.query);
const useNumResults = () => useSelector((state: RootState) => state.vectorSearchPage.numResults);
const useSelectedDocuments = () => useSelector((state: RootState) => state.vectorSearchPage.selectedDocuments);
