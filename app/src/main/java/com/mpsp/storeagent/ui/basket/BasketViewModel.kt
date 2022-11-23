package com.mpsp.storeagent.ui.basket

import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.mpsp.storeagent.ui.uievents.ActionNavigationEvent
import com.mpsp.storeagent.ui.uievents.AgentResponseEvent

//App will have one global basket.
data class BasketState(
    val agentResponseEvent: AgentResponseEvent = AgentResponseEvent(),
    val actionNavigationEvent: ActionNavigationEvent = ActionNavigationEvent(),
    val showSpeechDialog: Boolean = false
) : MavericksState

class BasketViewModel(initialState: BasketState) : MavericksViewModel<BasketState>(initialState) {
    init {

    }

    private fun getBasket() = withState { state ->

    }
}