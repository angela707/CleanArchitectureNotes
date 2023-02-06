package com.training.cleanarchitecture.business.interactors.splash

import com.training.cleanarchitecture.business.data.cache.CacheResponseHandler
import com.training.cleanarchitecture.business.data.cache.abstraction.NoteCacheDataSource
import com.training.cleanarchitecture.business.data.network.ApiResponseHandler
import com.training.cleanarchitecture.business.data.network.abstraction.NoteNetworkDataSource
import com.training.cleanarchitecture.business.data.util.safeApiCall
import com.training.cleanarchitecture.business.data.util.safeCacheCall
import com.training.cleanarchitecture.business.domain.model.Note
import com.training.cleanarchitecture.business.domain.state.DataState
import com.training.cleanarchitecture.util.printLogD
import kotlinx.coroutines.Dispatchers.IO

/*
    Search firestore for all notes in the "deleted" node.
    It will then search the cache for notes matching those deleted notes.
    If a match is found, it is deleted from the cache.
 */
class SyncDeletedNotes(
    private val noteCacheDataSource: NoteCacheDataSource,
    private val noteNetworkDataSource: NoteNetworkDataSource
) {
    suspend fun syncDeletedNotes() {
        val apiResult = safeApiCall(IO) {
            noteNetworkDataSource.getDeletedNotes()
        }

        val response = object : ApiResponseHandler<List<Note>, List<Note>>(
            response = apiResult,
            stateEvent = null
        ) {
            override suspend fun handleSuccess(resultObj: List<Note>): DataState<List<Note>> {
                return DataState.data(
                    response = null,
                    data = resultObj,
                    stateEvent = null
                )
            }
        }

        val notes = response.getResult().data ?: ArrayList()
        val cacheResult = safeCacheCall(IO) {
            noteCacheDataSource.deleteNotes(notes)
        }

        //debugging purposes
        object : CacheResponseHandler<Int, Int>(
            response = cacheResult,
            stateEvent = null
        ) {
            override suspend fun handleSuccess(resultObj: Int): DataState<Int>? {
                printLogD(
                    "SyncNotes",
                    "num deleted notes: ${resultObj}"
                )
                return DataState.data(
                    response = null,
                    data = resultObj,
                    stateEvent = null
                )
            }
        }.getResult()
    }
}
