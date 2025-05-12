import {ChatResponse} from "../../api/chatApi.tsx";

export function ConversationExchange({ exchange }: { exchange: ChatResponse}) {
  return <>
    <div>
      <label className="exchange-user-label">User: </label>

      <div>
        <pre className="exchange-value">{exchange.prompt}</pre>
      </div>
    </div>
    <div>
      <label className="exchange-user-label">Bot: </label>
      <div>
          <pre className="exchange-value">
            {exchange.response}
          </pre>
      </div>
    </div>
  </>;
}
