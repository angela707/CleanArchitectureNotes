package com.training.cleanarchitecture.business.interactors.notelist

import com.training.cleanarchitecture.business.data.cache.CacheErrors.CACHE_ERROR_UNKNOWN
import com.training.cleanarchitecture.business.data.cache.FORCE_SEARCH_NOTES_EXCEPTION
import com.training.cleanarchitecture.business.data.cache.abstraction.NoteCacheDataSource
import com.training.cleanarchitecture.business.data.network.abstraction.NoteNetworkDataSource
import com.training.cleanarchitecture.business.domain.model.Note
import com.training.cleanarchitecture.business.domain.model.NoteFactory
import com.training.cleanarchitecture.business.interactors.notelist.SearchNotes.Companion.SEARCH_NOTES_NO_MATCHING_RESULTS
import com.training.cleanarchitecture.business.interactors.notelist.SearchNotes.Companion.SEARCH_NOTES_SUCCESS
import com.training.cleanarchitecture.di.DependencyContainer
import com.training.cleanarchitecture.framework.datasource.cache.database.ORDER_BY_ASC_DATE_UPDATED
import com.training.cleanarchitecture.framework.presentation.notelist.state.NoteListStateEvent
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/*
Test cases:
1. blankQuery_success_confirmNotesRetrieved()
    a) query with some default search options
    b) listen for SEARCH_NOTES_SUCCESS emitted from flow
    c) confirm notes were retrieved
    d) confirm notes in cache match with notes that were retrieved
2. randomQuery_success_confirmNoResults()
    a) query with something that will yield no results
    b) listen for SEARCH_NOTES_NO_MATCHING_RESULTS emitted from flow
    c) confirm nothing was retrieved
    d) confirm there is notes in the cache
3. searchNotes_fail_confirmNoResults()
    a) force an exception to be thrown
    b) listen for CACHE_ERROR_UNKNOWN emitted from flow
    c) confirm nothing was retrieved
    d) confirm there is notes in the cache
 */

class SearchNotesTest {
    //system in test
    private val searchNotes: SearchNotes

    // dependencies
    private val dependencyContainer: DependencyContainer
    private val noteCacheDataSource: NoteCacheDataSource
    private val noteNetworkDataSource: NoteNetworkDataSource
    private val noteFactory: NoteFactory

    init {
        dependencyContainer = DependencyContainer()
        dependencyContainer.build()
        noteCacheDataSource = dependencyContainer.noteCacheDataSource
        noteNetworkDataSource = dependencyContainer.noteNetworkDataSource
        noteFactory = dependencyContainer.noteFactory
        searchNotes = SearchNotes(
            noteCacheDataSource = noteCacheDataSource
        )
    }

    @Test
    fun blankQuery_success_confirmNotesRetrieved() = runBlocking {
        val query = ""
        var results: ArrayList<Note>? = null

        searchNotes.searchNotes(
            query = query,
            filterAndOrder = ORDER_BY_ASC_DATE_UPDATED,
            page = 1,
            stateEvent = NoteListStateEvent.SearchNotesEvent()
        ).collect { value ->
            assertEquals(
                value?.stateMessage?.response?.message,
                SEARCH_NOTES_SUCCESS
            )
            value?.data?.noteList?.let { list ->
                results = ArrayList(list)
            }
        }

        //confirm notes were retrieved
        assertTrue(results != null)

        //confirm notes in cache match with retrieved notes
        val notesInCache = noteCacheDataSource.searchNotes(
            query = query,
            filterAndOrder = ORDER_BY_ASC_DATE_UPDATED,
            page = 1
        )

        assertTrue(results?.containsAll(notesInCache) ?: false)
    }

    @Test
    fun randomQuery_success_confirmNoResults() = runBlocking {

        val query = "hthrthrgrkgenrogn843nn4u34n934v53454hrth"
        var results: ArrayList<Note>? = null
        searchNotes.searchNotes(
            query = query,
            filterAndOrder = ORDER_BY_ASC_DATE_UPDATED,
            page = 1,
            stateEvent = NoteListStateEvent.SearchNotesEvent()
        ).collect { value ->
            assertEquals(
                value?.stateMessage?.response?.message,
                SEARCH_NOTES_NO_MATCHING_RESULTS
            )
            value?.data?.noteList?.let { list ->
                results = ArrayList(list)
            }
        }

        // confirm nothing was retrieved
        assertTrue(results?.run { size == 0 } ?: true)

        // confirm there is notes in the cache
        val notesInCache = noteCacheDataSource.searchNotes(
            query = "",
            filterAndOrder = ORDER_BY_ASC_DATE_UPDATED,
            page = 1
        )
        assertTrue(notesInCache.isNotEmpty())
    }

    @Test
    fun searchNotes_fail_confirmNoResults() = runBlocking {

        val query = FORCE_SEARCH_NOTES_EXCEPTION
        var results: ArrayList<Note>? = null
        searchNotes.searchNotes(
            query = query,
            filterAndOrder = ORDER_BY_ASC_DATE_UPDATED,
            page = 1,
            stateEvent = NoteListStateEvent.SearchNotesEvent()
        ).collect { value ->
            assert(
                value?.stateMessage?.response?.message
                    ?.contains(CACHE_ERROR_UNKNOWN) ?: false
            )
            value?.data?.noteList?.let { list ->
                results = ArrayList(list)
            }
        }

        // confirm nothing was retrieved
        assertTrue(results?.run { size == 0 } ?: true)

        // confirm there is notes in the cache
        val notesInCache = noteCacheDataSource.searchNotes(
            query = "",
            filterAndOrder = ORDER_BY_ASC_DATE_UPDATED,
            page = 1
        )
        assertTrue(notesInCache.isNotEmpty())
    }
}