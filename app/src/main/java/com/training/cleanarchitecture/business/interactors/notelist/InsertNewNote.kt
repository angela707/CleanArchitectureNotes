package com.training.cleanarchitecture.business.interactors.notelist

import com.training.cleanarchitecture.business.data.cache.abstraction.NoteCacheDataSource
import com.training.cleanarchitecture.business.data.network.abstraction.NoteNetworkDataSource
import com.training.cleanarchitecture.business.domain.model.Note
import com.training.cleanarchitecture.business.domain.model.NoteFactory
import com.training.cleanarchitecture.business.domain.state.*
import com.training.cleanarchitecture.framework.presentation.notelist.state.NoteListViewState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.*

class InsertNewNote(
    private val noteCacheDataSource: NoteCacheDataSource,
    private val noteNetworkDataSource: NoteNetworkDataSource,
    private val noteFactory: NoteFactory
) {
    companion object{
        const val INSERT_NOTE_SUCCESS = "Successfully inserted new note."
        const val INSERT_NOTE_FAILURE = "Failed to insert new note."
    }

    fun insertNewNote(
        id: String? = null,
        title: String,
        stateEvent: StateEvent
    ): Flow<DataState<NoteListViewState>> = flow {
        val newNote = noteFactory.createSingleNote(
            id = id ?: UUID.randomUUID().toString(),
            title = title,
            body = ""
        )
        val cacheResult = noteCacheDataSource.insertNote(newNote)
        var cacheResponse: DataState<NoteListViewState>? = null
        if(cacheResult > 0) {
            val viewState = NoteListViewState(
                newNote = newNote
            )
            cacheResponse = DataState.data(
                response = Response(
                    message = INSERT_NOTE_SUCCESS,
                    uiComponentType = UIComponentType.Toast(),
                    messageType = MessageType.Success()
                ),
                data = viewState,
                stateEvent = stateEvent
            )
        }
        else{
            cacheResponse = DataState.data(
                response = Response(
                    message = INSERT_NOTE_FAILURE,
                    uiComponentType = UIComponentType.Toast(),
                    messageType = MessageType.Error()
                ),
                data = null,
                stateEvent = stateEvent
            )
        }
        emit(cacheResponse)
        updateNetwork(cacheResponse.stateMessage?.response?.message, newNote)
    }

    private suspend fun updateNetwork(cacheResponse: String?, newNote: Note) {
        if(cacheResponse.equals(INSERT_NOTE_SUCCESS)){
            noteNetworkDataSource.insertOrUpdateNote(newNote)
        }
    }
}