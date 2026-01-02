import {ChatResponse} from "../../api/chatApi.tsx";
import {useState} from "react";
import SearchResultsItem from "../../components/SearchResultItem/SearchResultItem.tsx";

export interface FinalResponse extends ChatResponse {
  queryRewrite: string | undefined;
  completionTokensUsed: number | undefined;
  promptTokensUsed: number | undefined;
  totalTokensUsed: number | undefined;
}

export function StreamingPastConversationExchange({ exchange }: { exchange: FinalResponse}) {
  const [showMoreRAG, setShowMoreRAG] = useState(false);
  const [showMoreThinking, setShowMoreThinking] = useState(false);

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

          <div className="exchange-metadata">
            <label>total tokens used: </label> {exchange.totalTokensUsed}
          </div>

          <div className="thinking-wrapper">
            <button className="thinking-see-more" onClick={showMoreThinkingClicked}>
              <span className="thinking-see-more-text">See Thinking</span>
              <span className="thinking-see-more-icon">{showMoreThinking ? down : up}</span>
            </button>

            {showMoreThinking && <div className="thinking-result-content">
              <p>
                {exchange.thinking}
              </p>
            </div>
            }
          </div>

           <div className="exchange-wrapper">
           <button className="exchange-see-more" onClick={showMoreRAGClicked}>
              <span className="exchange-see-more-text">See RAG Documents</span>
              <span className="echange-see-more-icon">{showMoreRAG ? down : up}</span>
            </button>


             {showMoreRAG && exchange.queryRewrite && (
               <div className="query-rewrite-wrapper">
                 <label className="query-rewrite-label">Query Rewrite: </label>
                 <div className="query-rewrite-content">
                   <pre className="exchange-value">{exchange.queryRewrite}</pre>
                 </div>
               </div>
             )}

             {showMoreRAG && (exchange.vectorSearchResults.map(vr => (
               <SearchResultsItem searchResult={vr} />
             )))}
           </div>
      </div>
    </div>
  </>;

  function showMoreRAGClicked() {
    setShowMoreRAG(!showMoreRAG);
  }

  function showMoreThinkingClicked() {
    setShowMoreThinking(!showMoreThinking);
  }
}
