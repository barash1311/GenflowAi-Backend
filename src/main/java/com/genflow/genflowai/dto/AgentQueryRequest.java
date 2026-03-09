package com.genflow.genflowai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request body sent to the genflowai-agent:
 * POST /api/v1/queries/nl-to-sql
 */
public class AgentQueryRequest {

    /** The natural language prompt text from the user */
    @JsonProperty("query")
    private final String query;

    /**
     * Optional geographic scope hint (e.g. "Kerala", "Kochi district").
     * The agent uses this to scope its PostGIS SQL to a region.
     */
    @JsonProperty("region_hint")
    private final String regionHint;

    public AgentQueryRequest(String query, String regionHint) {
        this.query = query;
        this.regionHint = regionHint;
    }

    public String getQuery() { return query; }
    public String getRegionHint() { return regionHint; }
}
