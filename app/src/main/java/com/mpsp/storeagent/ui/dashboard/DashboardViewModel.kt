package com.mpsp.storeagent.ui.dashboard

import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.cloud.dialogflow.v2.*
import com.mpsp.storeagent.AppConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

data class DashboardState(
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
            val uuid = UUID.randomUUID().toString()

            val settingsBuilder: SessionsSettings.Builder = SessionsSettings.newBuilder()
            val sessionsSettings: SessionsSettings = settingsBuilder.setCredentialsProvider(
                FixedCredentialsProvider.create(AppConstants.agentCredentials)
            ).build()
            val sessionsClient = SessionsClient.create(sessionsSettings)
            val sessionName = SessionName.of(AppConstants.projectId, uuid)

            val input = QueryInput.newBuilder()
                .setText(
                    TextInput.newBuilder().setText(speech).setLanguageCode(AppConstants.agentLanguageCode)
                ).build()

            val detectIntentRequest = DetectIntentRequest.newBuilder()
                .setSession(sessionName.toString())
                .setQueryInput(input)
                .build()

            try {
                val result = sessionsClient?.detectIntent(detectIntentRequest)
                result?.toString()
            } catch(ex: Exception) {
                //TODO Inform the UI
            }
        }
    }
}