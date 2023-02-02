package com.training.cleanarchitecture.business.interactors.notelist

import com.training.cleanarchitecture.business.data.cache.abstraction.NoteCacheDataSource
import com.training.cleanarchitecture.business.domain.model.NoteFactory
import com.training.cleanarchitecture.business.interactors.notelist.GetNumNotes.Companion.GET_NUM_NOTES_SUCCESS
import com.training.cleanarchitecture.di.DependencyContainer
import com.training.cleanarchitecture.framework.presentation.notelist.state.NoteListStateEvent
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/*
Test cases:
1. getNumNotes_success_confirmCorrect()
    a) get the number of notes in cache
    b) listen for GET_NUM_NOTES_SUCCESS from flow emission
    c) compare with the number of notes in the fake data set
*/
class GetNumNotesTest {
    //system in test
    private val getNumNotes: GetNumNotes

    // dependencies
    private val dependencyContainer: DependencyContainer
    private val noteCacheDataSource: NoteCacheDataSource
    private val noteFactory: NoteFactory

    init {
        dependencyContainer = DependencyContainer()
        dependencyContainer.build()
        noteCacheDataSource = dependencyContainer.noteCacheDataSource
        noteFactory = dependencyContainer.noteFactory
        getNumNotes = GetNumNotes(
            noteCacheDataSource = noteCacheDataSource,
        )
    }

    @Test
    fun getNumNotes_success_confirmCorrect() = runBlocking {
        var numNotes = 0
        getNumNotes.getNumNotes(
            stateEvent = NoteListStateEvent.GetNumNotesInCacheEvent()
        ).collect { value ->
            assertEquals(
                value?.stateMessage?.response?.message,
                GET_NUM_NOTES_SUCCESS
            )
            numNotes = value?.data?.numNotesInCache ?: 0
        }

        val actualNumNotesInCache = noteCacheDataSource.getNumNotes()
        assertTrue(actualNumNotesInCache == numNotes)
    }
}