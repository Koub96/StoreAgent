package com.mpsp.storeagent.ui.basket

import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class SharedBasketState(
    val id: String = ""
) : MavericksState

class SharedBasketViewModel(initialState: SharedBasketState) : MavericksViewModel<SharedBasketState>(initialState) {

    fun addProductToBasket(productId: String, quantity: String = "") = viewModelScope.launch(Dispatchers.IO) {
        productId.toString()
        quantity.toString()
    }
}