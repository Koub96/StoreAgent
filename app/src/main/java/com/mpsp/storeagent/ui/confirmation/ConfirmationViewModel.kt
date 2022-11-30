package com.mpsp.storeagent.ui.confirmation

import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.google.cloud.dialogflow.v2.DetectIntentRequest
import com.google.cloud.dialogflow.v2.QueryInput
import com.google.cloud.dialogflow.v2.TextInput
import com.mpsp.storeagent.agent.intenthandlers.AgentActionHandler
import com.mpsp.storeagent.agent.session.AgentSession
import com.mpsp.storeagent.singletons.AppConstants
import com.mpsp.storeagent.ui.uievents.ActionNavigationEvent
import com.mpsp.storeagent.ui.uievents.AgentResponseEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class ConfirmationState(
    val agentResponseEvent: AgentResponseEvent = AgentResponseEvent(),
    val actionNavigationEvent: ActionNavigationEvent = ActionNavigationEvent(),
    //val addToBasketEvent: AddToBasketEvent = AddToBasketEvent(),
    val showSpeechDialog: Boolean = false
) : MavericksState

class ConfirmationViewModel(initialState: ConfirmationState) : MavericksViewModel<ConfirmationState>(initialState) {
    init {
        //TODO Fetch basket summary
    }

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

                val actionHandler = AgentActionHandler()
                val action = actionHandler.determineAction(
                    result.queryResult.action,
                    result.queryResult.parameters.fieldsMap
                )

//                if(action.navigationEvent.name == AgentActionEnum.GetProduct.name) {
//                    val productId = action.entityMapping[Product::class.simpleName!!]
//                    if(productId != null) {
//                        val basketEvent = AddToBasketEvent(productId)
//                        setState {
//                            copy(
//                                addToBasketEvent = basketEvent
//                            )
//                        }
//                    }
//                } else if(action.navigationEvent.name == AgentActionEnum.GetProductWithQuantity.name) {
//                    val productId = action.entityMapping[Product::class.simpleName!!]
//                    val quantity = action.entityMapping[actionHandler.quantityKey]
//
//                    if(productId != null) {
//                        val basketEvent = AddToBasketEvent(productId = productId)
//                        if(!quantity.isNullOrEmpty())
//                            basketEvent.quantity = quantity
//
//                        setState {
//                            copy(
//                                addToBasketEvent = basketEvent
//                            )
//                        }
//                    }
//                }

                val agentResponseEvent = AgentResponseEvent(response = result.queryResult.fulfillmentText)
                setState {
                    copy(
                        agentResponseEvent = agentResponseEvent
                    )
                }

                val navigateByActionEvent = ActionNavigationEvent(action)
                setState {
                    copy(
                        actionNavigationEvent = navigateByActionEvent
                    )
                }
            } catch(ex: Exception) {
                //TODO Inform the UI
            }
        }
    }
}