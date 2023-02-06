package com.training.cleanarchitecture.framework.presentation.notedetail.state

import android.os.Parcelable
import com.training.cleanarchitecture.business.domain.model.Note
import com.training.cleanarchitecture.business.domain.state.ViewState
import kotlinx.android.parcel.Parcelize

@Parcelize
data class NoteDetailViewState(
    var note: Note? = null,
    var isUpdatePending: Boolean? = null
) : Parcelable, ViewState