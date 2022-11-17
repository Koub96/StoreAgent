package com.mpsp.storeagent.agent.intenthandlers

import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.airbnb.mvrx.Mavericks
import com.mpsp.storeagent.R
import com.mpsp.storeagent.agent.enums.AgentActionEnum
import com.mpsp.storeagent.models.MasterCategory
import com.mpsp.storeagent.models.Subcategory
import com.mpsp.storeagent.ui.subcategories.SubcategoriesFragmentArgs

fun Fragment.agentNavigation(
    agentNavigationAction: AgentActionEnum,
    entityMapping: Map<String, String>
) {
    if (agentNavigationAction == AgentActionEnum.Unspecified)
        return

    if (agentNavigationAction == AgentActionEnum.GetProductType) {
        if (entityMapping[MasterCategory::class.simpleName].isNullOrEmpty())
            return

        findNavController().navigate(
            R.id.dashboard_to_subcategories,
            bundleOf(
                Mavericks.KEY_ARG to SubcategoriesFragmentArgs(
                    entityMapping[MasterCategory::class.simpleName]!!
                )
            )
        )
    } else if (agentNavigationAction == AgentActionEnum.GetProductTypeAndSubtype) {
        if (!entityMapping[Subcategory::class.simpleName].isNullOrEmpty()) {
            //Navigation to subcategory with all the corresponding products.
            return
        }

        if (!entityMapping[MasterCategory::class.simpleName].isNullOrEmpty()) {
            if (entityMapping[Subcategory::class.simpleName].isNullOrEmpty()) {
                findNavController().navigate(
                    R.id.dashboard_to_subcategories,
                    bundleOf(
                        Mavericks.KEY_ARG to SubcategoriesFragmentArgs(
                            entityMapping[MasterCategory::class.simpleName]!!
                        )
                    )
                )
            } else {
                //Navigation to subcategory with all the corresponding products.
            }
        }
    }
}
