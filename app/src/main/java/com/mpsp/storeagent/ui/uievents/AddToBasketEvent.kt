package com.mpsp.storeagent.ui.uievents

data class AddToBasketEvent(
    val productId: String = "",
    var quantity: String = ""
)
