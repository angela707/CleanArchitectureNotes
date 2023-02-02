package com.training.cleanarchitecture.di

import com.training.cleanarchitecture.business.NoteDataFactory
import com.training.cleanarchitecture.business.data.cache.FakeNoteCacheDataSourceImpl
import com.training.cleanarchitecture.business.data.cache.abstraction.NoteCacheDataSource
import com.training.cleanarchitecture.business.data.network.FakeNoteNetworkDataSourceImpl
import com.training.cleanarchitecture.business.data.network.abstraction.NoteNetworkDataSource
import com.training.cleanarchitecture.business.domain.model.Note
import com.training.cleanarchitecture.business.domain.model.NoteFactory
import com.training.cleanarchitecture.business.domain.util.DateUtil
import com.training.cleanarchitecture.util.isUnitTest
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class DependencyContainer {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd hh:mm:ss a", Locale.ENGLISH)
    val dateUtil = DateUtil(dateFormat)
    lateinit var noteNetworkDataSource: NoteNetworkDataSource
    lateinit var noteCacheDataSource: NoteCacheDataSource
    lateinit var noteFactory: NoteFactory
    lateinit var noteDataFactory: NoteDataFactory
    private var notesData: HashMap<String, Note> = HashMap()

    init {
        isUnitTest = true // for Logger.kt
    }

    fun build() {
        this.javaClass.classLoader?.let { classLoader ->
            noteDataFactory = NoteDataFactory(classLoader)

            //fake data set
            notesData = noteDataFactory.produceHashMapOfNotes(
                noteDataFactory.produceListOfNotes()
            )
        }
        noteFactory = NoteFactory(dateUtil)
        noteNetworkDataSource = FakeNoteNetworkDataSourceImpl(
            notesData = notesData,
            deletedNotesData = HashMap(),
            dateUtil = dateUtil
        )
        noteCacheDataSource = FakeNoteCacheDataSourceImpl(
            notesData = notesData,
            dateUtil = dateUtil
        )
    }

}

















