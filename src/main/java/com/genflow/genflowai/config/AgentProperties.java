package com.genflow.genflowai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed configuration for the genflowai-agent microservice.
 * Reads from application.yaml under: genflow.agent.*
 */
@ConfigurationProperties(prefix = "genflow.agent")
public class AgentProperties {

    /** Base URL of the Python FastAPI agent (e.g. http://localhost:8000) */
    private String baseUrl = "http://localhost:8000";

    /** HTTP timeout in seconds for agent calls (LLM can be slow!) */
    private int timeoutSeconds = 180;

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
}
