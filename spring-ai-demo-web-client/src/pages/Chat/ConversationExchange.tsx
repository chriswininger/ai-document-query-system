import {ChatResponse} from "../../api/chatApi.tsx";
import {useState} from "react";
import SearchResultsItem from "../../components/SearchResultItem/SearchResultItem.tsx";

export function ConversationExchange({ exchange }: { exchange: ChatResponse}) {
  const [showMore, setShowMore] = useState(false);

  const up = '🔼';
  const down = '🔽';

  return <>
    <div>
      <label className="exchange-user-label">User: </label>

      <div>
        <pre className="exchange-value">{exchange.prompt}</pre>
      </div>
    </div>
    <div>
      <label className="exchange-user-label">Bot: </label>
      <div>️‍️
          <pre className="exchange-value" title={exchange.thinking}>
            {exchange.response}
          </pre>

           <div className="exchange-more">
              <button className="exchange-see-more" onClick={showMoreClicked}>See Documents Used {showMore ? down : up}</button>

             {showMore && (exchange.vectorSearchResults.map(vr => (
               <SearchResultsItem searchResult={vr} />
             )))}
           </div>
      </div>
    </div>
  </>;

  function showMoreClicked() {
    setShowMore(!showMore);
  }
}
