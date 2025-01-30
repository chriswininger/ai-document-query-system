import { RequestResult } from "../requests/useMakeRequest.tsx";
import {ChatRequestResult} from "../types/ChatRequestResult.tsx";

export default function PromptResult({ initialized, loading, error } : PromptResultsProps ) {
    if (!initialized) {
        return null;
    } else if (loading) {
        return <p role="status">Thinking...</p>;
    } else if (error !== null) {
        return <p className="error" role="alert">Error processing request: {error}</p>
    } else {
        return <p className="success" role="status">success</p>
    }
}

export type PromptResultsProps = RequestResult<ChatRequestResult>
