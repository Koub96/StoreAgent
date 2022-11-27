package com.mpsp.storeagent.ui.basket

import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.mpsp.storeagent.App
import com.mpsp.storeagent.models.Basket
import com.mpsp.storeagent.models.agent.ProductLine
import com.mpsp.storeagent.singletons.AppConstants
import com.mpsp.storeagent.ui.uievents.ActionNavigationEvent
import com.mpsp.storeagent.ui.uievents.AgentResponseEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

//App will have one global basket.
data class BasketState(
    val agentResponseEvent: AgentResponseEvent = AgentResponseEvent(),
    val actionNavigationEvent: ActionNavigationEvent = ActionNavigationEvent(),
    val showSpeechDialog: Boolean = false,
    val productLines: List<ProductLine> = emptyList()
) : MavericksState

class BasketViewModel(initialState: BasketState) : MavericksViewModel<BasketState>(initialState) {
    val database = App.getInstance().getDatabase()

    init {
        getBasket()
    }

    private fun getBasket() = viewModelScope.launch(Dispatchers.IO) {
        database.BasketDao().getBasket(AppConstants.currentBasketId)
            .distinctUntilChanged()
            .collect { basketLines ->
                val productLines = basketLines.map { basketLine ->
                    val product = database.ProductDao().getProductById(basketLine.productID)
                    ProductLine(productName = product.name, quantity = basketLine.quantity, price = product.price)
                }

                setState {
                    copy(
                        productLines = productLines
                    )
                }
            }
    }
}