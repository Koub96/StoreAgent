package com.mpsp.storeagent.ui.uievents

import com.mpsp.storeagent.agent.enums.AgentActionEnum

data class ActionNavigationEvent(
    val action: AgentActionEnum = AgentActionEnum.Unspecified,
    val entityID: String = ""
)
