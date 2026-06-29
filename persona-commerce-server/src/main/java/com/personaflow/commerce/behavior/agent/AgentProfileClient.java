package com.personaflow.commerce.behavior.agent;

import com.personaflow.commerce.behavior.agent.dto.AgentProfileBuildResponse;
import com.personaflow.commerce.behavior.vo.AgentProfileContext;

public interface AgentProfileClient {

    AgentProfileBuildResponse buildProfile(AgentProfileContext context);
}
