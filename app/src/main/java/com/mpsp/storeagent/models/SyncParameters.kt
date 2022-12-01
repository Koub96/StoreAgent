package com.mpsp.storeagent.models

data class SyncParameters(
    val masterCategoryParams: MasterCategoryParameters = MasterCategoryParameters(),
    val masterAndSubcategoryParams: MasterCategorySubcategoryParams = MasterCategorySubcategoryParams(),
    val productParams: ProductParameters = ProductParameters(),
    val deleteProductParams: DeleteProductParameters = DeleteProductParameters(),
    val increaseQuantityParams: IncreaseQuantityParams = IncreaseQuantityParams(),
    val decreaseQuantityParams: DecreaseQuantityParams = DecreaseQuantityParams()
)

data class MasterCategoryParameters(
    val trainingPhrases: ArrayList<String> = arrayListOf()
)

data class MasterCategorySubcategoryParams(
    val trainingPhrases: ArrayList<ArrayList<String>> = arrayListOf()
)

data class ProductParameters(
    val trainingPhrases: ArrayList<String> = arrayListOf(),
    val trainingPhrasesWithQuantity: ArrayList<String> = arrayListOf()
)

data class DeleteProductParameters(
    val trainingPhrases: ArrayList<String> = arrayListOf()
)

data class IncreaseQuantityParams(
    val trainingPhrases: ArrayList<ArrayList<String>> = arrayListOf()
)

data class DecreaseQuantityParams(
    val trainingPhrases: ArrayList<ArrayList<String>> = arrayListOf()
)
