import './VectorSearch.css'
import DocumentList from "../../components/DocumentList/DocumentList.tsx";
import {usePerformSearchMutation } from "../../api/vectorApi.tsx";
import {useDispatch, useSelector} from "react-redux";
import {AppDispatch, RootState} from "../../store/store.tsx";
import {documentSelected, documentUnSelected, numResultsUpdated, queryUpdated, useRAGRewriteUpdated} from "./vectorSearchPageSlice.tsx";
import {DocumentImport} from "../../api/apiBase.tsx";
import SearchResultsItem from "../../components/SearchResultItem/SearchResultItem.tsx";

export default function VectorSearch() {
  const dispatch = useDispatch<AppDispatch>();

  const query = useQuery();
  const numResults = useNumResults();
  const useRAGRewrite = useUseRAGRewrite();
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

        <label className="rag-rewrite-checkbox-label">
          <input
            type="checkbox"
            checked={useRAGRewrite}
            onChange={(e) => dispatch(useRAGRewriteUpdated(e.target.checked))}
          />
          Use RAG Rewrite
        </label>

        <button className="search-button" onClick={onSearchClicked}>Go</button>
      </section>

      {data?.rewrittenQuery && (
        <section className="rewritten-query">
          <p className="rewritten-query-label">Rewritten Query:</p>
          <p className="rewritten-query-text">{data.rewrittenQuery}</p>
        </section>
      )}

      <section className="search-results">
        { isLoading
          ? <p role="status">Loading...</p>
          : error
            ? <p className="error" role="alert">Error</p>
            : data?.searchResults?.map((searchResult, index) => <SearchResultsItem key={index} searchResult={searchResult} />)
        }
      </section>
    </div>
  </main>

  function onSearchClicked() {
    const documentIds = selectedDocuments.map(d => d.id)
    performSearch({ 
      query, 
      numMatches: numResults, 
      documentSourceIds: documentIds,
      useRAGRewrite: useRAGRewrite || undefined
    })
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
const useUseRAGRewrite = () => useSelector((state: RootState) => state.vectorSearchPage.useRAGRewrite);
const useSelectedDocuments = () => useSelector((state: RootState) => state.vectorSearchPage.selectedDocuments);
