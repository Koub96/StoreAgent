package com.mpsp.storeagent.models.agent

import com.mpsp.storeagent.agent.enums.AgentActionEnum

class AgentAction(
    val navigationEvent: AgentActionEnum = AgentActionEnum.Unspecified,
    val entityMapping: Map<String, String> = mapOf(),
) {
}