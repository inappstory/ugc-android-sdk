package com.inappstory.sdk.ugc.cache

interface UseCaseCallback<T> {
    fun onError(message: String?)
    fun onSuccess(result: T)
}