import {PerformPromptMutationResponse} from "../../api/chatApi.tsx";

export default function PromptResult({ isLoading, isUninitialized, isError, error, data } : PerformPromptMutationResponse ) {
    if (isUninitialized) {
        return null;
    }

    if (isLoading) {
        return <p role="status">Thinking...</p>;
    }

    if (isError) {
        return <p className="error" role="alert">Error processing request: {error}</p>
    }

    if (data) {
        const { requestTimeStartTime, requestEndTime } = data

        const { minutes, seconds } = getTimeTaken(requestTimeStartTime, requestEndTime)

        return <p className="success" role="status">success -- took {minutes}:{seconds}</p>
    }

    return <p className="success" role="status">success - missing data?</p>

    function getTimeTaken(startTime: string, endTime: string) {
        const diff = Math.abs(new Date(endTime).getTime() - new Date(startTime).getTime())

        const minutes = Math.floor(diff / (1000 * 60))
        const seconds = Math.floor(diff % (1000 * 60))

        return { minutes, seconds}
    }
}
