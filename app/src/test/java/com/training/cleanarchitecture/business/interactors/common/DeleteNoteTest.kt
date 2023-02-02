package com.training.cleanarchitecture.business.interactors.common

import com.training.cleanarchitecture.business.data.cache.CacheErrors.CACHE_ERROR_UNKNOWN
import com.training.cleanarchitecture.business.data.cache.FORCE_DELETES_NOTE_EXCEPTION
import com.training.cleanarchitecture.business.data.cache.abstraction.NoteCacheDataSource
import com.training.cleanarchitecture.business.data.network.abstraction.NoteNetworkDataSource
import com.training.cleanarchitecture.business.domain.model.Note
import com.training.cleanarchitecture.business.domain.model.NoteFactory
import com.training.cleanarchitecture.business.interactors.common.DeleteNote.Companion.DELETE_NOTE_FAILURE
import com.training.cleanarchitecture.business.interactors.common.DeleteNote.Companion.DELETE_NOTE_SUCCESS
import com.training.cleanarchitecture.di.DependencyContainer
import com.training.cleanarchitecture.framework.presentation.notelist.state.NoteListStateEvent
import com.training.cleanarchitecture.framework.presentation.notelist.state.NoteListViewState
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.util.*

/*
Test cases:
1. deleteNote_success_confirmNetworkUpdated()
    a) delete a note
    b) check for success message from flow emission
    c) confirm note was deleted from "notes" node in network
    d) confirm note was added to "deletes" node in network
2. deleteNote_fail_confirmNetworkUnchanged()
    a) attempt to delete a note, fail since does not exist
    b) check for failure message from flow emission
    c) confirm network was not changed
3. throwException_checkGenericError_confirmNetworkUnchanged()
    a) attempt to delete a note, force an exception to throw
    b) check for failure message from flow emission
    c) confirm network was not changed
 */

class DeleteNoteTest {
    //system in test
    private val deleteNote: DeleteNote<NoteListViewState>

    // dependencies
    private val dependencyContainer: DependencyContainer = DependencyContainer()
    private val noteCacheDataSource: NoteCacheDataSource
    private val noteNetworkDataSource: NoteNetworkDataSource
    private val noteFactory: NoteFactory

    init {
        dependencyContainer.build()
        noteCacheDataSource = dependencyContainer.noteCacheDataSource
        noteFactory = dependencyContainer.noteFactory
        noteNetworkDataSource = dependencyContainer.noteNetworkDataSource
        deleteNote = DeleteNote(
            noteCacheDataSource = noteCacheDataSource,
            noteNetworkDataSource = noteNetworkDataSource
        )
    }

    @Test
    fun deleteNote_success_confirmNetworkUpdated() = runBlocking {
        val noteToDelete = noteCacheDataSource.searchNotes("", "", 1)[0]

        deleteNote.deleteNote(
            note = noteToDelete,
            stateEvent = NoteListStateEvent.DeleteNoteEvent(noteToDelete)
        ).collect { value ->
            assertEquals(
                value?.stateMessage?.response?.message,
                DELETE_NOTE_SUCCESS
            )
        }

        //confirm was deleted from 'notes' node
        val wasNoteDeleted = !noteNetworkDataSource.getAllNotes().contains(noteToDelete)
        assertTrue(wasNoteDeleted)

        //confirm was inserted into 'deletes' node
        val wasDeletedNodeInserted = noteNetworkDataSource.getDeletedNotes().contains(noteToDelete)
        assertTrue(wasDeletedNodeInserted)
    }

    @Test
    fun deleteNote_fail_confirmNetworkUnchanged() = runBlocking {
        val noteToDelete = Note(
            id = UUID.randomUUID().toString(),
            title = UUID.randomUUID().toString(),
            body = UUID.randomUUID().toString(),
            created_at = UUID.randomUUID().toString(),
            updated_at = UUID.randomUUID().toString()
        )

        deleteNote.deleteNote(
            note = noteToDelete,
            stateEvent = NoteListStateEvent.DeleteNoteEvent(noteToDelete)
        ).collect { value ->
            assertEquals(
                value?.stateMessage?.response?.message,
                DELETE_NOTE_FAILURE
            )
        }

        //confirm was not deleted from 'notes' node
        val notes = noteNetworkDataSource.getAllNotes()
        val numNotesInCache = noteCacheDataSource.getNumNotes()
        assertTrue(numNotesInCache == notes.size)

        //confirm was not inserted into 'deletes' node
        val wasDeletedNodeInserted = !noteNetworkDataSource.getDeletedNotes().contains(noteToDelete)
        assertTrue(wasDeletedNodeInserted)
    }

    @Test
    fun throwException_checkGenericError_confirmNetworkUnchanged() = runBlocking {
        val noteToDelete = Note(
            id = FORCE_DELETES_NOTE_EXCEPTION,
            title = UUID.randomUUID().toString(),
            body = UUID.randomUUID().toString(),
            created_at = UUID.randomUUID().toString(),
            updated_at = UUID.randomUUID().toString()
        )

        deleteNote.deleteNote(
            note = noteToDelete,
            stateEvent = NoteListStateEvent.DeleteNoteEvent(noteToDelete)
        ).collect { value ->
            assert(
                value?.stateMessage?.response?.message?.contains(CACHE_ERROR_UNKNOWN) ?: false
            )
        }

        //confirm was not deleted from 'notes' node
        val notes = noteNetworkDataSource.getAllNotes()
        val numNotesInCache = noteCacheDataSource.getNumNotes()
        assertTrue(numNotesInCache == notes.size)

        //confirm was not inserted into 'deletes' node
        val wasDeletedNodeInserted = !noteNetworkDataSource.getDeletedNotes().contains(noteToDelete)
        assertTrue(wasDeletedNodeInserted)
    }
}