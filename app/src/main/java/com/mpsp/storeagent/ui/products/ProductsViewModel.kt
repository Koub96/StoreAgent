package com.mpsp.storeagent.ui.products

import android.os.Parcelable
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.PersistState
import com.google.cloud.dialogflow.v2.DetectIntentRequest
import com.google.cloud.dialogflow.v2.QueryInput
import com.google.cloud.dialogflow.v2.TextInput
import com.mpsp.storeagent.App
import com.mpsp.storeagent.AppConstants
import com.mpsp.storeagent.agent.enums.AgentActionEnum
import com.mpsp.storeagent.agent.intenthandlers.AgentActionHandler
import com.mpsp.storeagent.agent.session.AgentSession
import com.mpsp.storeagent.models.Product
import com.mpsp.storeagent.ui.subcategories.SubcategoriesState
import com.mpsp.storeagent.ui.uievents.ActionNavigationEvent
import com.mpsp.storeagent.ui.uievents.AgentResponseEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

@Parcelize
data class ProductsFragmentArgs(
    val subcategoryId: String
) : Parcelable

data class ProductsState(
    @PersistState val subcategoryId: String = "",
    val products: List<Product> = emptyList(),
    val actionNavigationEvent: ActionNavigationEvent = ActionNavigationEvent(),
    val agentResponseEvent: AgentResponseEvent = AgentResponseEvent(),
    val showSpeechDialog: Boolean = false
) : MavericksState {
    constructor(args: ProductsFragmentArgs) : this(args.subcategoryId)
}

class ProductsViewModel(initialState: ProductsState) : MavericksViewModel<ProductsState>(initialState) {
    val database = App.getInstance().getDatabase()

    init {
        fetchProducts()
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

                val action = AgentActionHandler().determineAction(
                    result.queryResult.action,
                    result.queryResult.parameters.fieldsMap
                )

                if(result.queryResult.action == AgentActionEnum.GetProduct.name) {
                    val productId = action.entityMapping[Product::class.simpleName!!]
                    if(productId != null) {
                        val product = database.ProductDao().getProductById(productId)
                        //TODO Add to basket
                        product.toString()
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

    private fun fetchProducts() = withState { state ->
        val id = state.subcategoryId

        viewModelScope.launch(Dispatchers.IO) {
            val products = App.getInstance().getDatabase().ProductDao().getProductsBySubcategory(id)
            setState {
                copy(
                    products = products
                )
            }
        }
    }
}