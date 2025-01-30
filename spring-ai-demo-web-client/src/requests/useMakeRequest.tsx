import {useEffect, useState} from "react";

export type Nullable<T> = T | null;
export const BODY_NOT_READY_VALUE = "BODY_NOT_READ";
export type BODY_NOT_READY = typeof BODY_NOT_READY_VALUE;

export interface RequestResult <T> {
    initialized: boolean,
    loading: boolean,
    error: Nullable<string>,
    data: Nullable<T>
}

export function doPostRequest <T, R>(url: string, body: T): Promise<R> {
    return fetch(
        url,
        {
            method: 'POST',
            body: JSON.stringify(body),
            headers: {
                "Content-Type": "application/json",
            },
        })
        .then(resp => resp.json())
}

export function useMakePostRequest <T, R> (
    { url, postBody }: { url: string, postBody: T | BODY_NOT_READY }): RequestResult<R>
{
    const [reqResult, setRequestResult] = useState(getInitialRequestState<R>())

    useEffect(() => {
        if (postBody === BODY_NOT_READY_VALUE) {
            return;
        }

        setRequestResult({
            ...getInitialRequestState(),
            initialized: true,
            loading: true
        })

        doPostRequest<T, R>(url, postBody)
            .then(result => {
                setRequestResult({
                    ...getInitialRequestState(),
                    initialized: true,
                    loading: false,
                    data: result
                })
            })
            .catch(err => () => {
                setRequestResult({
                    ...getInitialRequestState(),
                    initialized: true,
                    loading: false,
                    error: err.message
                })
            })
    },[url, postBody])

    return reqResult
}

export function getInitialRequestState <T> (): RequestResult<T> {
    return {
        initialized: false,
        loading: false,
        error: null,
        data: null
    }
}

export function getLoadingRequestState <T> (): RequestResult<T> {
    return {
        ...getInitialRequestState<T>(),
        initialized: true,
        loading: true,
        error: null,
        data: null
    }
}

export function getErrorRequestState <T> (error: string): RequestResult<T> {
    return {
        ...getInitialRequestState<T>(),
        initialized: true,
        loading: false,
        error,
        data: null
    }
}

export function getSuccessRequestState <T> (data: T): RequestResult<T> {
    return {
        ...getInitialRequestState(),
        initialized: true,
        loading: false,
        error: null,
        data
    }
}
