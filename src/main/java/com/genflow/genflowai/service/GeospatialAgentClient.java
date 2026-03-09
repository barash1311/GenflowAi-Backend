package com.genflow.genflowai.service;

import com.genflow.genflowai.config.AgentProperties;
import com.genflow.genflowai.dto.AgentQueryRequest;
import com.genflow.genflowai.dto.AgentQueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Thin HTTP client for the genflowai-agent FastAPI microservice.
 *
 * Endpoint: POST {baseUrl}/api/v1/queries/nl-to-sql
 */
@Service
public class GeospatialAgentClient {

    private static final Logger log = LoggerFactory.getLogger(GeospatialAgentClient.class);
    private static final String NL_TO_SQL_PATH = "/api/v1/queries/nl-to-sql";

    private final RestTemplate restTemplate;
    private final AgentProperties agentProperties;

    public GeospatialAgentClient(
            @Qualifier("agentRestTemplate") RestTemplate restTemplate,
            AgentProperties agentProperties) {
        this.restTemplate = restTemplate;
        this.agentProperties = agentProperties;
    }

    /**
     * Send a natural language query to the agent and receive the generated SQL
     * and optional shapefile path.
     *
     * @param promptText  The user's plain-text query (e.g. "Find rivers in Kerala")
     * @param regionHint  Optional region name extracted from the prompt or dataset metadata
     * @return The full agent response including SQL, safety check, and shapefile path
     * @throws AgentUnavailableException if the agent is unreachable or returns an error
     */
    public AgentQueryResponse query(String promptText, String regionHint) {
        String url = agentProperties.getBaseUrl() + NL_TO_SQL_PATH;
        AgentQueryRequest body = new AgentQueryRequest(promptText, regionHint);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AgentQueryRequest> entity = new HttpEntity<>(body, headers);

        log.info("[AgentClient] Sending NL query to agent: url={}, query='{}'",
                url, promptText.length() > 80 ? promptText.substring(0, 80) + "..." : promptText);

        try {
            ResponseEntity<AgentQueryResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    AgentQueryResponse.class
            );

            AgentQueryResponse result = response.getBody();
            if (result == null) {
                throw new AgentUnavailableException("Agent returned empty response body");
            }

            log.info("[AgentClient] Agent responded: requestId={}, shapefilePath={}",
                    result.getRequestId(), result.getShapefilePath());
            return result;

        } catch (RestClientException ex) {
            log.error("[AgentClient] Failed to reach agent at {}: {}", url, ex.getMessage());
            throw new AgentUnavailableException("Agent microservice unreachable: " + ex.getMessage(), ex);
        }
    }

    // ---- Checked exception for agent connectivity issues ----

    public static class AgentUnavailableException extends RuntimeException {
        public AgentUnavailableException(String message) { super(message); }
        public AgentUnavailableException(String message, Throwable cause) { super(message, cause); }
    }
}
