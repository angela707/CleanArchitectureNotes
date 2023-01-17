package com.training.cleanarchitecture.business.data.cache

sealed class CacheResult<out T> {
    data class Success<out T>(val value: T) : CacheResult<T>()
    data class GenericError<out T>(val errorMessage: String?) : CacheResult<Nothing>()
}