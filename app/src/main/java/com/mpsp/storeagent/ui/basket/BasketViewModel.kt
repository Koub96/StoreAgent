package com.mpsp.storeagent.ui.basket

import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.mpsp.storeagent.App
import com.mpsp.storeagent.models.Basket
import com.mpsp.storeagent.models.BasketFooter
import com.mpsp.storeagent.models.agent.ProductLine
import com.mpsp.storeagent.singletons.AppConstants
import com.mpsp.storeagent.ui.uievents.ActionNavigationEvent
import com.mpsp.storeagent.ui.uievents.AgentResponseEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

//App will have one global basket.
data class BasketState(
    val agentResponseEvent: AgentResponseEvent = AgentResponseEvent(),
    val actionNavigationEvent: ActionNavigationEvent = ActionNavigationEvent(),
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

    private fun getBasket() = viewModelScope.launch(Dispatchers.IO) {
        database.BasketDao().getBasket(AppConstants.currentBasketId)
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