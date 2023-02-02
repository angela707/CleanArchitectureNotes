package com.training.cleanarchitecture.business.interactors.notelist

import com.training.cleanarchitecture.business.data.cache.CacheErrors.CACHE_ERROR_UNKNOWN
import com.training.cleanarchitecture.business.data.cache.FORCE_GENERAL_FAILURE
import com.training.cleanarchitecture.business.data.cache.FORCE_NEW_NOTE_EXCEPTION
import com.training.cleanarchitecture.business.data.cache.abstraction.NoteCacheDataSource
import com.training.cleanarchitecture.business.data.network.abstraction.NoteNetworkDataSource
import com.training.cleanarchitecture.business.domain.model.NoteFactory
import com.training.cleanarchitecture.business.interactors.notelist.RestoreDeletedNote.Companion.RESTORE_NOTE_FAILED
import com.training.cleanarchitecture.business.interactors.notelist.RestoreDeletedNote.Companion.RESTORE_NOTE_SUCCESS
import com.training.cleanarchitecture.di.DependencyContainer
import com.training.cleanarchitecture.framework.presentation.notelist.state.NoteListStateEvent
import junit.framework.Assert.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.util.*

/*
Test cases:
1. restoreNote_success_confirmCacheAndNetworkUpdated()
    a) create a new note and insert it into the "deleted" node of network
    b) restore that note
    c) Listen for success msg RESTORE_NOTE_SUCCESS from flow
    d) confirm note is in the cache
    e) confirm note is in the network "notes" node
    f) confirm note is not in the network "deletes" node
2. restoreNote_fail_confirmCacheAndNetworkUnchanged()
    a) create a new note and insert it into the "deleted" node of network
    b) restore that note (force a failure)
    c) Listen for success msg RESTORE_NOTE_FAILED from flow
    d) confirm note is not in the cache
    e) confirm note is not in the network "notes" node
    f) confirm note is in the network "deletes" node
3. throwException_checkGenericError_confirmNetworkAndCacheUnchanged()
    a) create a new note and insert it into the "deleted" node of network
    b) restore that note (force an exception)
    c) Listen for success msg CACHE_ERROR_UNKNOWN from flow
    d) confirm note is not in the cache
    e) confirm note is not in the network "notes" node
    f) confirm note is in the network "deletes" node
 */

class RestoreDeletedNoteTest {
    // system in test
    private val restoreDeletedNote: RestoreDeletedNote

    // dependencies
    private val dependencyContainer: DependencyContainer = DependencyContainer()
    private val noteCacheDataSource: NoteCacheDataSource
    private val noteNetworkDataSource: NoteNetworkDataSource
    private val noteFactory: NoteFactory

    init {
        dependencyContainer.build()
        noteCacheDataSource = dependencyContainer.noteCacheDataSource
        noteNetworkDataSource = dependencyContainer.noteNetworkDataSource
        noteFactory = dependencyContainer.noteFactory
        restoreDeletedNote = RestoreDeletedNote(
            noteCacheDataSource = noteCacheDataSource,
            noteNetworkDataSource = noteNetworkDataSource,
        )
    }

    @Test
    fun restoreNote_success_confirmCacheAndNetworkUpdated() = runBlocking {
        // create a new note and insert into network "deletes" node
        val restoredNote = noteFactory.createSingleNote(
            id = UUID.randomUUID().toString(),
            title = UUID.randomUUID().toString(),
            body = UUID.randomUUID().toString()
        )

        noteNetworkDataSource.insertDeletedNote(restoredNote)

        // confirm that note is in the "deletes" node before restoration
        var deletedNotes = noteNetworkDataSource.getDeletedNotes()
        assertTrue(deletedNotes.contains(restoredNote))

        restoreDeletedNote.restoreDeletedNote(
            note = restoredNote,
            stateEvent = NoteListStateEvent.RestoreDeletedNoteEvent(restoredNote)
        ).collect { value ->
            assertEquals(
                value?.stateMessage?.response?.message,
                RESTORE_NOTE_SUCCESS
            )
        }

        // confirm note is in the cache
        val noteInCache = noteCacheDataSource.searchNoteById(restoredNote.id)
        assertTrue(noteInCache == restoredNote)

        // confirm note is in the network "notes" node
        val noteInNetwork = noteNetworkDataSource.searchNote(restoredNote)
        assertTrue(noteInNetwork == restoredNote)

        // confirm note is not in the network "deletes" node
        deletedNotes = noteNetworkDataSource.getDeletedNotes()
        assertFalse(deletedNotes.contains(restoredNote))
    }

    @Test
    fun restoreNote_fail_confirmCacheAndNetworkUnchanged() = runBlocking {
        // create a new note and insert into network "deletes" node
        val restoredNote = noteFactory.createSingleNote(
            id = FORCE_GENERAL_FAILURE,
            title = UUID.randomUUID().toString(),
            body = UUID.randomUUID().toString()
        )

        noteNetworkDataSource.insertDeletedNote(restoredNote)

        // confirm that note is in the "deletes" node before restoration
        var deletedNotes = noteNetworkDataSource.getDeletedNotes()
        assertTrue(deletedNotes.contains(restoredNote))

        restoreDeletedNote.restoreDeletedNote(
            note = restoredNote,
            stateEvent = NoteListStateEvent.RestoreDeletedNoteEvent(restoredNote)
        ).collect { value ->
            assertEquals(
                value?.stateMessage?.response?.message,
                RESTORE_NOTE_FAILED
            )
        }

        // confirm note is not in the cache
        val noteInCache = noteCacheDataSource.searchNoteById(restoredNote.id)
        assertFalse(noteInCache == restoredNote)

        // confirm note is not in the network "notes" node
        val noteInNetwork = noteNetworkDataSource.searchNote(restoredNote)
        assertFalse(noteInNetwork == restoredNote)

        // confirm note is in the network "deletes" node
        deletedNotes = noteNetworkDataSource.getDeletedNotes()
        assertTrue(deletedNotes.contains(restoredNote))
    }

    @Test
    fun throwException_checkGenericError_confirmNetworkAndCacheUnchanged() = runBlocking {
        // create a new note and insert into network "deletes" node
        val restoredNote = noteFactory.createSingleNote(
            id = FORCE_NEW_NOTE_EXCEPTION,
            title = UUID.randomUUID().toString(),
            body = UUID.randomUUID().toString()
        )

        noteNetworkDataSource.insertDeletedNote(restoredNote)

        // confirm that note is in the "deletes" node before restoration
        var deletedNotes = noteNetworkDataSource.getDeletedNotes()
        assertTrue(deletedNotes.contains(restoredNote))

        restoreDeletedNote.restoreDeletedNote(
            note = restoredNote,
            stateEvent = NoteListStateEvent.RestoreDeletedNoteEvent(restoredNote)
        ).collect { value ->
            assert(
                value?.stateMessage?.response?.message?.contains(CACHE_ERROR_UNKNOWN) ?: false
            )
        }

        // confirm note is not in the cache
        val noteInCache = noteCacheDataSource.searchNoteById(restoredNote.id)
        assertFalse(noteInCache == restoredNote)

        // confirm note is not in the network "notes" node
        val noteInNetwork = noteNetworkDataSource.searchNote(restoredNote)
        assertFalse(noteInNetwork == restoredNote)

        // confirm note is in the network "deletes" node
        deletedNotes = noteNetworkDataSource.getDeletedNotes()
        assertTrue(deletedNotes.contains(restoredNote))
    }

}