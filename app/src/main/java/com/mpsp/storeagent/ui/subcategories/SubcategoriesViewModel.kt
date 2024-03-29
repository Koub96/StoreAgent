package com.mpsp.storeagent.ui.subcategories

import android.os.Parcelable
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.PersistState
import com.google.cloud.dialogflow.v2.DetectIntentRequest
import com.google.cloud.dialogflow.v2.QueryInput
import com.google.cloud.dialogflow.v2.TextInput
import com.mpsp.storeagent.App
import com.mpsp.storeagent.agent.enums.AgentActionEnum
import com.mpsp.storeagent.singletons.AppConstants
import com.mpsp.storeagent.agent.intenthandlers.AgentActionHandler
import com.mpsp.storeagent.agent.session.AgentSession
import com.mpsp.storeagent.models.Product
import com.mpsp.storeagent.models.Subcategory
import com.mpsp.storeagent.ui.uievents.ActionNavigationEvent
import com.mpsp.storeagent.ui.uievents.AddToBasketEvent
import com.mpsp.storeagent.ui.uievents.AgentResponseEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

@Parcelize
data class SubcategoriesFragmentArgs(
    val masterCategoryId: String
) : Parcelable

data class SubcategoriesState(
    @PersistState val masterCategoryId: String = "",
    val subcategories: List<Subcategory> = emptyList(),
    val actionNavigationEvent: ActionNavigationEvent = ActionNavigationEvent(),
    val agentResponseEvent: AgentResponseEvent = AgentResponseEvent(),
    val addToBasketEvent: AddToBasketEvent = AddToBasketEvent(),
    val showSpeechDialog: Boolean = false
) : MavericksState {
    constructor(args: SubcategoriesFragmentArgs) : this(args.masterCategoryId)
}

class SubcategoriesViewModel(initialState: SubcategoriesState) : MavericksViewModel<SubcategoriesState>(initialState) {
    val appDatabase = App.getInstance().getDatabase()

    init {
        fetchSubcategories()
    }

    private fun fetchSubcategories()  = withState { state ->
        val id = state.masterCategoryId
        viewModelScope.launch(Dispatchers.IO) {
            val masterSubcategories = appDatabase.MasterSubcategoryDao().getMasterSubcategories(id)
            val subcategories = arrayListOf<Subcategory>()
            masterSubcategories.forEach {
                val subcategory = appDatabase.SubcategoryDao().getSubcategory(it.subcategoryID)
                subcategories.add(subcategory)
            }

            setState {
                copy(
                    subcategories = subcategories.toList()
                )
            }
        }
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

                if(action.navigationEvent.name == AgentActionEnum.GetProduct.name) {
                    val productId = action.entityMapping[Product::class.simpleName!!]
                    if(productId != null) {
                        val basketEvent = AddToBasketEvent(productId)
                        setState {
                            copy(
                                addToBasketEvent = basketEvent
                            )
                        }
                    }
                } else if(action.navigationEvent.name == AgentActionEnum.GetProductWithQuantity.name) {
                    val productId = action.entityMapping[Product::class.simpleName!!]
                    val quantity = action.entityMapping[actionHandler.quantityKey]

                    if(productId != null) {
                        val basketEvent = AddToBasketEvent(productId = productId)
                        if(!quantity.isNullOrEmpty())
                            basketEvent.quantity = quantity

                        setState {
                            copy(
                                addToBasketEvent = basketEvent
                            )
                        }
                    }
                }

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