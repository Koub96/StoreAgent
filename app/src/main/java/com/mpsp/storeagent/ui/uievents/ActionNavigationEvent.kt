package com.mpsp.storeagent.ui.uievents

import com.mpsp.storeagent.models.agent.AgentAction

data class ActionNavigationEvent(
    val action: AgentAction = AgentAction()
)
