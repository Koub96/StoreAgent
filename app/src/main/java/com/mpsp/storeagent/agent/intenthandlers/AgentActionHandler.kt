package com.mpsp.storeagent.agent.intenthandlers

import com.mpsp.storeagent.App
import com.mpsp.storeagent.agent.enums.AgentActionEnum
import com.mpsp.storeagent.models.MasterCategory
import com.mpsp.storeagent.models.Product
import com.mpsp.storeagent.models.Subcategory
import com.mpsp.storeagent.models.agent.AgentAction

class AgentActionHandler {
    private val productTypeKey: String = "product-type"
    private val productSubtypeKey: String = "product-subtype"
    private val productKey: String = "product"
    val quantityKey: String = "quantity"

    suspend fun determineAction(
        action: String,
        parameters: Map<String, com.google.protobuf.Value>
    ): AgentAction {
        if (action == AgentActionEnum.GetProductType.name) {
            val masterCategoryId = handleGetProductTypeID(parameters)
            return AgentAction(
                AgentActionEnum.GetProductType,
                mapOf(MasterCategory::class.simpleName!! to masterCategoryId)
            )
        } else if (action == AgentActionEnum.GetProductTypeAndSubtype.name) {
            val masterToSubcategoryIds = handleGetProductTypeAndSubtypeIds(parameters)
            return AgentAction(
                AgentActionEnum.GetProductTypeAndSubtype,
                mapOf(
                    MasterCategory::class.simpleName!! to masterToSubcategoryIds.first,
                    Subcategory::class.simpleName!! to masterToSubcategoryIds.second
                )
            )
        } else if (action == AgentActionEnum.GetProduct.name) {
            val productId = handleGetProductId(parameters)

            return AgentAction(
                AgentActionEnum.GetProduct,
                mapOf(Product::class.simpleName!! to productId)
            )
        } else if (action == AgentActionEnum.GetProductWithQuantity.name) {
            val productId = handleGetProductId(parameters)
            val quantity = if(parameters[quantityKey] == null)
                ""
            else
                parameters[quantityKey]!!.numberValue.toString()

            return AgentAction(
                AgentActionEnum.GetProductWithQuantity,
                mapOf(
                    Product::class.simpleName!! to productId,
                    quantityKey to quantity
                )
            )
        } else if (action == AgentActionEnum.GetBasket.name) {
            return AgentAction(
                AgentActionEnum.GetBasket,
                emptyMap()
            )
        } else if(action == AgentActionEnum.FinalizeOrder.name) {
            return AgentAction(
                AgentActionEnum.FinalizeOrder,
                emptyMap()
            )
        }

        return AgentAction()
    }

    private suspend fun handleGetProductTypeID(parameters: Map<String, com.google.protobuf.Value>): String {
        val masterCategoryTitle = parameters[productTypeKey]?.stringValue //Could be an alias too.
        return getMasterCategoryId(masterCategoryTitle.toString())
    }

    private suspend fun handleGetProductTypeAndSubtypeIds(parameters: Map<String, com.google.protobuf.Value>): Pair<String, String> {
        val masterCategoryTitle = parameters[productTypeKey]?.stringValue //Could be an alias too.
        val subcategoryTitle = parameters[productSubtypeKey]?.stringValue //Could be an alias too.

        val masterCategoryId = getMasterCategoryId(masterCategoryTitle.toString())
        val subcategoryId = getSubcategoryId(subcategoryTitle.toString())

        return masterCategoryId to subcategoryId
    }

    private suspend fun getMasterCategoryId(masterCategoryTitle: String): String {
        val masterCategoryId = App.getInstance().getDatabase()
            .MasterCategoryDao()
            .getMasterCategoryIdByTitle(masterCategoryTitle)

        if (masterCategoryId == null) {
            val masterCategoryIdFromAlias = App.getInstance().getDatabase()
                .MasterCategoryAliasDao()
                .getMasterCategoryIdByAlias(masterCategoryTitle)

            return if (masterCategoryIdFromAlias.isNullOrEmpty())
                ""
            else
                masterCategoryIdFromAlias
        }

        return masterCategoryId
    }

    private suspend fun getSubcategoryId(subcategoryTitle: String): String {
        val subcategoryId = App.getInstance().getDatabase()
            .SubcategoryDao()
            .getSubcategoryIdByTitle(subcategoryTitle)

        if (subcategoryId == null) {
            val subcategoryIdFromAlias = App.getInstance().getDatabase()
                .SubcategoryAliasDao()
                .getSubcategoryIdByAlias(subcategoryTitle)

            return if (subcategoryIdFromAlias.isNullOrEmpty())
                ""
            else
                subcategoryIdFromAlias
        }

        return subcategoryId
    }

    private suspend fun handleGetProductId(parameters: Map<String, com.google.protobuf.Value>): String {
        val productTitle = parameters[productKey]?.stringValue ?: return "" //Could be an alias too.

        val productId = App.getInstance().getDatabase().ProductDao().getProductIdByName(productTitle)
        if(productId == null) {
            val productIdByAlias = App.getInstance().getDatabase()
                .ProductAliasDao()
                .getProductIdByAlias(productTitle)

            return if(productIdByAlias.isNullOrEmpty())
                ""
            else
                productIdByAlias
        }

        return productId
    }
}