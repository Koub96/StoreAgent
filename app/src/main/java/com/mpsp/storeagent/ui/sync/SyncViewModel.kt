package com.mpsp.storeagent.ui.sync

import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel

data class SyncState(val title: String = "") : MavericksState

class SyncViewModel(initialState: SyncState) : MavericksViewModel<SyncState>(initialState) {
    init {

    }

    fun getProductParameters() {}
}