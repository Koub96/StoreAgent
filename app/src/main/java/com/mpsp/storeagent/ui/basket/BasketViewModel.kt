package com.mpsp.storeagent.ui.basket

import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.google.cloud.dialogflow.v2.DetectIntentRequest
import com.google.cloud.dialogflow.v2.QueryInput
import com.google.cloud.dialogflow.v2.TextInput
import com.mpsp.storeagent.App
import com.mpsp.storeagent.agent.enums.AgentActionEnum
import com.mpsp.storeagent.agent.intenthandlers.AgentActionHandler
import com.mpsp.storeagent.agent.session.AgentSession
import com.mpsp.storeagent.models.Basket
import com.mpsp.storeagent.models.BasketFooter
import com.mpsp.storeagent.models.Product
import com.mpsp.storeagent.models.agent.ProductLine
import com.mpsp.storeagent.singletons.AppConstants
import com.mpsp.storeagent.ui.uievents.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

//App will have one global basket.
data class BasketState(
    val agentResponseEvent: AgentResponseEvent = AgentResponseEvent(),
    val actionNavigationEvent: ActionNavigationEvent = ActionNavigationEvent(),
    val addToBasketEvent: AddToBasketEvent = AddToBasketEvent(),
    val deleteFromBasketEvent: DeleteFromBasketEvent = DeleteFromBasketEvent(),
    val finalizedOrderEvent: FinalizedOrderEvent = FinalizedOrderEvent(),
    val showSpeechDialog: Boolean = false,
    val productLines: List<ProductLine> = emptyList(),
    val footerData: BasketFooter = BasketFooter()
) : MavericksState

class BasketViewModel(initialState: BasketState) : MavericksViewModel<BasketState>(initialState) {
    val database = App.getInstance().getDatabase()

    init {
        getBasket()
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

    fun finalizeOrder() {
        setState {
            copy(
                showSpeechDialog = true
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            AgentSession.terminateSessionClient()

            val finalizedOrderEvent = FinalizedOrderEvent(id = UUID.randomUUID().toString())
            setState {
                copy(
                    showSpeechDialog = false,
                    finalizedOrderEvent = finalizedOrderEvent
                )
            }
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
                } else if(action.navigationEvent.name == AgentActionEnum.DeleteProduct.name) {
                    val productId = action.entityMapping[Product::class.simpleName!!]
                    if(productId != null) {
                        val deleteFromBasketEvent = DeleteFromBasketEvent(productID = productId)
                        setState {
                            copy(
                                deleteFromBasketEvent = deleteFromBasketEvent
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

    private fun getBasket() = viewModelScope.launch(Dispatchers.IO) {
        database.BasketDao().getBasketFlow(AppConstants.currentBasketId)
            .distinctUntilChanged()
            .collect { basketLines ->
                val productLines = basketLines.map { basketLine ->
                    val product = database.ProductDao().getProductById(basketLine.productID)

                    var finalPrice = if(product.discount > 0) {
                        product.price * product.discount
                    } else {
                        product.price
                    }

                    finalPrice = BigDecimal(finalPrice).setScale(2, RoundingMode.HALF_EVEN).toDouble()
                    finalPrice = if(basketLine.quantity > 1) {
                        finalPrice * basketLine.quantity
                    } else {
                        finalPrice
                    }

                    ProductLine(
                        productName = product.name,
                        quantity = basketLine.quantity,
                        price = finalPrice
                    )
                }

                val totalSum = productLines.sumOf { productLine ->
                    productLine.price
                }
                val roundedTotalSum = BigDecimal(totalSum).setScale(2, RoundingMode.HALF_EVEN)
                val footerData = BasketFooter(totalSum = roundedTotalSum.toDouble())

                setState {
                    copy(
                        productLines = productLines
                    )
                }

                setState {
                    copy(
                        footerData = footerData
                    )
                }
            }
    }
}