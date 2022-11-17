package com.mpsp.storeagent.ui.products

import android.os.Parcelable
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.PersistState
import com.mpsp.storeagent.App
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
    init {
        fetchProducts()
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