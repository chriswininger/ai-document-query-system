import {VectorSearchResult} from "../../api/vectorApi.tsx";
import './SearchResultItem.css';

export default
function SearchResultsItem({ searchResult } : { searchResult: VectorSearchResult}) {
  return  (
    <div className="search-results-item">
      <p>
        {searchResult.text}
      </p>

      <code>
        {JSON.stringify(searchResult.metadata, null, 4)}
      </code>

      <div className="search-result-score-area">
        <label>Score: </label><span title="search-result-score">{searchResult.score}</span>
      </div>
    </div>
  )
}
