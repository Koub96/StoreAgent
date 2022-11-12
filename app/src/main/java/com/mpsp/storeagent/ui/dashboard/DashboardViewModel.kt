package com.mpsp.storeagent.ui.dashboard

import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.cloud.dialogflow.v2.*
import com.mpsp.storeagent.AppConstants
import com.mpsp.storeagent.agent.session.AgentSession
import com.mpsp.storeagent.ui.uievents.AgentResponseEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

data class DashboardState(
    val agentResponseEvent: AgentResponseEvent = AgentResponseEvent(),
    val showSpeechDialog: Boolean = false
) : MavericksState

class DashboardViewModel(initialState: DashboardState) : MavericksViewModel<DashboardState>(initialState)  {
    init {}

    fun showSpeechDialog() {
        setState {
            copy(showSpeechDialog = true)
        }
    }

    fun hideSpeechDialog() {
        setState {
            copy(showSpeechDialog = false)
        }
    }

    fun processSpeech(speech: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val input = QueryInput.newBuilder()
                .setText(
                    TextInput.newBuilder().setText(speech).setLanguageCode(AppConstants.agentLanguageCode)
                ).build()

            val detectIntentRequest = DetectIntentRequest.newBuilder()
                .setSession(AgentSession.getSessionName()?.toString())
                .setQueryInput(input)
                .build()

            try {
                val result = AgentSession.getSessionClient()?.detectIntent(detectIntentRequest)
                if(result == null)
                    return@launch

                val agentResponseEvent = AgentResponseEvent(response = result.queryResult.fulfillmentText)
                setState {
                    copy(
                        agentResponseEvent = agentResponseEvent
                    )
                }
            } catch(ex: Exception) {
                //TODO Inform the UI
            }
        }
    }
}