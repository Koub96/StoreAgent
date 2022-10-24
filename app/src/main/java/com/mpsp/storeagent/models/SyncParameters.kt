package com.mpsp.storeagent.models

data class SyncParameters(
    val masterCategoryParams: MasterCategoryParameters = MasterCategoryParameters(),
    val productParams: ProductParameters = ProductParameters()
)

data class MasterCategoryParameters(
    val trainingPhrases: ArrayList<String> = arrayListOf()
)

data class ProductParameters(
    val trainingPhrases: ArrayList<String> = arrayListOf(),
    val trainingPhrasesWithQuantity: ArrayList<String> = arrayListOf()
)
