package com.training.cleanarchitecture.framework.presentation.notelist

import android.os.Bundle
import android.view.*
import com.training.cleanarchitecture.framework.presentation.common.BaseNoteFragment
import com.training.cleanarchitecture.R

const val NOTE_LIST_STATE_BUNDLE_KEY =
    "com.codingwithmitch.cleannotes.notes.framework.presentation.notelist.state"

class NoteListFragment : BaseNoteFragment(R.layout.fragment_note_list) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun inject() {
        TODO("prepare dagger")
    }

}










































