package com.training.cleanarchitecture.business.interactors.notelist

import com.training.cleanarchitecture.business.data.cache.FORCE_DELETES_NOTE_EXCEPTION
import com.training.cleanarchitecture.business.data.cache.abstraction.NoteCacheDataSource
import com.training.cleanarchitecture.business.data.network.abstraction.NoteNetworkDataSource
import com.training.cleanarchitecture.business.domain.model.Note
import com.training.cleanarchitecture.business.domain.model.NoteFactory
import com.training.cleanarchitecture.business.interactors.notelist.DeleteMultipleNotes.Companion.DELETE_NOTES_ERRORS
import com.training.cleanarchitecture.business.interactors.notelist.DeleteMultipleNotes.Companion.DELETE_NOTES_SUCCESS
import com.training.cleanarchitecture.di.DependencyContainer
import com.training.cleanarchitecture.framework.presentation.notelist.state.NoteListStateEvent
import junit.framework.Assert.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

/*
Test cases:
1. deleteNotes_success_confirmNetworkAndCacheUpdated()
    a) select a handful of random notes for deleting
    b) delete from cache and network
    c) confirm DELETE_NOTES_SUCCESS msg is emitted from flow
    d) confirm notes are delted from cache
    e) confirm notes are deleted from "notes" node in network
    f) confirm notes are added to "deletes" node in network
2. deleteNotes_fail_confirmCorrectDeletesMade()
    - This is a complex one:
        - The use-case will attempt to delete all notes passed as input. If there
        is an error with a particular delete, it continues with the others. But the
        resulting msg is DELETE_NOTES_ERRORS. So we need to do rigorous checks here
        to make sure the correct notes were deleted and the correct notes were not.
    a) select a handful of random notes for deleting
    b) change the ids of a few notes so they will cause errors when deleting
    c) confirm DELETE_NOTES_ERRORS msg is emitted from flow
    d) confirm ONLY the valid notes are deleted from network "notes" node
    e) confirm ONLY the valid notes are inserted into network "deletes" node
    f) confirm ONLY the valid notes are deleted from cache
3. throwException_checkGenericError_confirmNetworkAndCacheUnchanged()
    a) select a handful of random notes for deleting
    b) force an exception to be thrown on one of them
    c) confirm DELETE_NOTES_ERRORS msg is emitted from flow
    d) confirm ONLY the valid notes are deleted from network "notes" node
    e) confirm ONLY the valid notes are inserted into network "deletes" node
    f) confirm ONLY the valid notes are deleted from cache
 */
class DeleteMultipleNotesTest {
    // system in test
    private var deleteMultipleNotes: DeleteMultipleNotes? = null

    // dependencies
    private lateinit var dependencyContainer: DependencyContainer
    private lateinit var noteCacheDataSource: NoteCacheDataSource
    private lateinit var noteNetworkDataSource: NoteNetworkDataSource
    private lateinit var noteFactory: NoteFactory


    @AfterEach
    fun afterEach() {
        deleteMultipleNotes = null
    }

    @BeforeEach
    fun beforeEach() {
        dependencyContainer = DependencyContainer()
        dependencyContainer.build()
        noteCacheDataSource = dependencyContainer.noteCacheDataSource
        noteNetworkDataSource = dependencyContainer.noteNetworkDataSource
        noteFactory = dependencyContainer.noteFactory
        deleteMultipleNotes = DeleteMultipleNotes(
            noteCacheDataSource = noteCacheDataSource,
            noteNetworkDataSource = noteNetworkDataSource
        )
    }


    @Test
    fun deleteNotes_success_confirmNetworkAndCacheUpdated() = runBlocking {
        val randomNotes: ArrayList<Note> = ArrayList()
        val notesInCache = noteCacheDataSource.searchNotes("", "", 1)

        for (note in notesInCache) {
            randomNotes.add(note)
            if (randomNotes.size > 4) {
                break
            }
        }

        deleteMultipleNotes?.deleteNotes(
            notes = randomNotes,
            stateEvent = NoteListStateEvent.DeleteMultipleNotesEvent(randomNotes)
        )?.collect { value ->
            assertEquals(
                value?.stateMessage?.response?.message,
                DELETE_NOTES_SUCCESS
            )
        }

        //confirm notes were inserted into 'deletes' node
        val deleteNetworkNodes = noteNetworkDataSource.getDeletedNotes()
        assertTrue(deleteNetworkNodes.containsAll(randomNotes))

        // confirm notes were deleted from 'notes' node
        val doNotesExistInNetwork = noteNetworkDataSource.getAllNotes().containsAll(randomNotes)
        assertFalse(doNotesExistInNetwork)

        //confirm notes were deleted from cache
        for (note in randomNotes) {
            val noteInCache = noteCacheDataSource.searchNoteById(note.id)
            assertTrue(noteInCache == null)
        }
    }


    @Test
    fun deleteNotes_fail_confirmCorrectDeletesMade() = runBlocking {
        val validNotes: ArrayList<Note> = ArrayList()
        val invalidNotes: ArrayList<Note> = ArrayList()
        val notesInCache = noteCacheDataSource.searchNotes("", "", 1)

        for (index in 0..notesInCache.size) {
            var note: Note
            if (index % 2 == 0) {
                note = noteFactory.createSingleNote(
                    id = UUID.randomUUID().toString(),
                    title = notesInCache[index].title,
                    body = notesInCache[index].body
                )
                invalidNotes.add(note)
            } else {
                note = notesInCache[index]
                validNotes.add(note)
            }

            if ((invalidNotes.size + validNotes.size) > 4) {
                break
            }
        }

        val notesToDelete = ArrayList(validNotes + invalidNotes)

        deleteMultipleNotes?.deleteNotes(
            notes = notesToDelete,
            stateEvent = NoteListStateEvent.DeleteMultipleNotesEvent(notesToDelete)
        )?.collect { value ->
            assertEquals(
                value?.stateMessage?.response?.message,
                DELETE_NOTES_ERRORS
            )
        }

        //confirm only valid notes are deleted from the network 'notes' node
        val networkNotes = noteNetworkDataSource.getAllNotes()
        assertFalse(networkNotes.containsAll(validNotes))

        //confirm only valid notes are inserted into the network 'deletes' node
        val deletedNetworkNotes = noteNetworkDataSource.getDeletedNotes()
        assertTrue(deletedNetworkNotes.containsAll(validNotes))
        assertFalse(deletedNetworkNotes.containsAll(invalidNotes))

        //confirm only the valid notes are deleted from cache
        for (note in validNotes) {
            val noteInCache = noteCacheDataSource.searchNoteById(note.id)
            assertTrue(noteInCache == null)
        }
        val numNotesInCache = noteCacheDataSource.getNumNotes()
        assertTrue(numNotesInCache == (notesInCache.size - validNotes.size))
    }


    @Test
    fun throwException_checkGenericError_confirmNetworkAndCacheUnchanged() = runBlocking {
        val validNotes: ArrayList<Note> = ArrayList()
        val invalidNotes: ArrayList<Note> = ArrayList()
        val notesInCache = noteCacheDataSource.searchNotes("", "", 1)

        for (note in notesInCache) {
            validNotes.add(note)
            if (validNotes.size > 4) {
                break
            }
        }
        val errorNote = Note(
            id = FORCE_DELETES_NOTE_EXCEPTION,
            title = UUID.randomUUID().toString(),
            body = UUID.randomUUID().toString(),
            created_at = UUID.randomUUID().toString(),
            updated_at = UUID.randomUUID().toString()
        )
        invalidNotes.add(errorNote)

        val notesToDelete = ArrayList(validNotes + invalidNotes)

        deleteMultipleNotes?.deleteNotes(
            notes = notesToDelete,
            stateEvent = NoteListStateEvent.DeleteMultipleNotesEvent(notesToDelete)
        )?.collect { value ->
            assertEquals(
                value?.stateMessage?.response?.message,
                DELETE_NOTES_ERRORS
            )
        }

        //confirm only valid notes are deleted from the network 'notes' node
        val networkNotes = noteNetworkDataSource.getAllNotes()
        assertFalse(networkNotes.containsAll(validNotes))

        //confirm only valid notes are inserted into the network 'deletes' node
        val deletedNetworkNotes = noteNetworkDataSource.getDeletedNotes()
        assertTrue(deletedNetworkNotes.containsAll(validNotes))
        assertFalse(deletedNetworkNotes.containsAll(invalidNotes))

        //confirm only the valid notes are deleted from cache
        for (note in validNotes) {
            val noteInCache = noteCacheDataSource.searchNoteById(note.id)
            assertTrue(noteInCache == null)
        }
        val numNotesInCache = noteCacheDataSource.getNumNotes()
        assertTrue(numNotesInCache == (notesInCache.size - validNotes.size))
    }

}