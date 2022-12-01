package com.mpsp.storeagent.ui.basket

import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.mpsp.storeagent.App
import com.mpsp.storeagent.models.Basket
import com.mpsp.storeagent.singletons.AppConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class SharedBasketState(
    val id: String = ""
) : MavericksState

class SharedBasketViewModel(initialState: SharedBasketState) : MavericksViewModel<SharedBasketState>(initialState) {
    val database = App.getInstance().getDatabase()

    fun addProductToBasket(productId: String, quantity: String = "") = viewModelScope.launch(Dispatchers.IO) {
        val newBasketLine = if(!quantity.isNullOrEmpty())
            Basket(basketID = AppConstants.currentBasketId, productID = productId, quantity = quantity.toFloat().toInt())
        else
            Basket(basketID = AppConstants.currentBasketId, productID = productId)

        database.BasketDao().insertBasketLine(newBasketLine)
    }

    fun deleteProductFromBasket(productId: String) = viewModelScope.launch(Dispatchers.IO) {
        if(productId.isNullOrEmpty())
            return@launch

        database.BasketDao().deleteProductFromBasket(productId, AppConstants.currentBasketId)
    }
}