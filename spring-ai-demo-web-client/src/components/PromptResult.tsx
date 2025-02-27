import { RequestResult } from "../requests/useMakeRequest.tsx";
import {ChatRequestResult} from "../types/ChatRequestResult.tsx";

export default function PromptResult({ initialized, loading, error, data } : PromptResultsProps ) {
    if (!initialized) {
        return null;
    } else if (loading) {
        return <p role="status">Thinking...</p>;
    } else if (error !== null) {
        return <p className="error" role="alert">Error processing request: {error}</p>
    } else {
        console.log('!!! here: ', data)
        if (data) {
            const { requestTimeStartTime, requestEndTime } = data

            const { minutes, seconds } = getTimeTaken(requestTimeStartTime, requestEndTime)

            return <p className="success" role="status">success -- took {minutes}:{seconds}</p>
        }
        return <p className="success" role="status">success - missing data?</p>
    }

    function getTimeTaken(startTime: string, endTime: string) {
        const diff = Math.abs(new Date(endTime).getTime() - new Date(startTime).getTime())

        const minutes = Math.floor(diff / (1000 * 60))
        const seconds = Math.floor(diff % (1000 * 60))

        return { minutes, seconds}
    }
}

export type PromptResultsProps = RequestResult<ChatRequestResult>
