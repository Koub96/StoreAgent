package com.mpsp.storeagent.agent.intenthandlers

import com.mpsp.storeagent.App
import com.mpsp.storeagent.agent.enums.AgentActionEnum

class AgentActionHandler {
    private val productTypeKey: String = "product-type"

    fun determineAction(action: String): AgentActionEnum {
        if(action == AgentActionEnum.GetProductType.name) {
            return AgentActionEnum.GetProductType
        }

        return AgentActionEnum.Unspecified
    }

    suspend fun determineEntityId(action: String, parameters: Map<String, com.google.protobuf.Value>): String {
        if(action == AgentActionEnum.GetProductType.name) {
            return handleGetProductTypeID(parameters)
        }

        return ""
    }

    private suspend fun handleGetProductTypeID(parameters: Map<String, com.google.protobuf.Value>): String {
        val masterCategoryTitle = parameters[productTypeKey]?.stringValue //Could be an alias too.
        val masterCategoryId = App.getInstance().getDatabase()
            .MasterCategoryDao()
            .getMasterCategoryIdByTitle(masterCategoryTitle.toString())

        if(masterCategoryId == null) {
            val masterCategoryIdFromAlias = App.getInstance().getDatabase()
                .MasterCategoryAliasDao()
                .getMasterCategoryIdByAlias(masterCategoryTitle.toString())

            return if(masterCategoryIdFromAlias.isNullOrEmpty())
                ""
            else
                masterCategoryIdFromAlias
        }

        return masterCategoryId
    }
}