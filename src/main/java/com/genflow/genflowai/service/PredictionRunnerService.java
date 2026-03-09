package com.genflow.genflowai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genflow.genflowai.dto.AgentQueryResponse;
import com.genflow.genflowai.entity.Prediction;
import com.genflow.genflowai.entity.PredictionJob;
import com.genflow.genflowai.entity.enums.Status;
import com.genflow.genflowai.exceptions.ResourceNotFoundException;
import com.genflow.genflowai.repository.PredictionJobRepository;
import com.genflow.genflowai.repository.PredictionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Async job runner that bridges a PredictionJob with the genflowai-agent
 * microservice. Runs in the "predictionTaskExecutor" thread pool so it
 * never blocks HTTP request threads.
 *
 * Lifecycle:
 *   PENDING → RUNNING → SUCCESS | FAILED
 */
@Service
public class PredictionRunnerService {

    private static final Logger log = LoggerFactory.getLogger(PredictionRunnerService.class);

    private final PredictionJobRepository predictionJobRepository;
    private final PredictionRepository predictionRepository;
    private final GeospatialAgentClient agentClient;
    private final ObjectMapper objectMapper;

    public PredictionRunnerService(
            PredictionJobRepository predictionJobRepository,
            PredictionRepository predictionRepository,
            GeospatialAgentClient agentClient,
            ObjectMapper objectMapper) {
        this.predictionJobRepository = predictionJobRepository;
        this.predictionRepository = predictionRepository;
        this.agentClient = agentClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Executes the full geospatial prediction pipeline asynchronously.
     *
     * @param jobId        ID of the PredictionJob to track progress
     * @param predictionId ID of the Prediction entity to update with results
     */
    @Async("predictionTaskExecutor")
    @Transactional
    public void runPredictionAsync(UUID jobId, UUID predictionId) {
        log.info("[Runner] Starting async prediction: jobId={}, predictionId={}", jobId, predictionId);

        // ── 1. Load entities ──────────────────────────────────────────────────
        PredictionJob job = predictionJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("PredictionJob not found: " + jobId));

        Prediction prediction = predictionRepository.findById(predictionId)
                .orElseThrow(() -> new ResourceNotFoundException("Prediction not found: " + predictionId));

        // ── 2. Mark as RUNNING ────────────────────────────────────────────────
        job.setStatus(Status.RUNNING);
        predictionJobRepository.save(job);
        prediction.setStatus(Status.RUNNING.name());
        predictionRepository.save(prediction);

        long startMs = System.currentTimeMillis();

        try {
            // ── 3. Extract prompt text and call the agent ─────────────────────
            String promptText = prediction.getPrompt().getPromptText();

            // Try to extract a region hint from the prompt for better geospatial scoping.
            // Simple heuristic: look for "in <Word>" pattern.
            String regionHint = extractRegionHint(promptText);

            log.info("[Runner] Calling agent: jobId={}, promptLength={}, regionHint='{}'",
                    jobId, promptText.length(), regionHint);

            AgentQueryResponse agentResponse = agentClient.query(promptText, regionHint);

            // ── 4. Build result JSON ──────────────────────────────────────────
            long elapsedMs = System.currentTimeMillis() - startMs;
            String resultJson = buildResultJson(agentResponse, elapsedMs);

            // ── 5. Persist success ────────────────────────────────────────────
            prediction.setResult(resultJson);
            prediction.setStatus(Status.SUCCESS.name());
            prediction.setExecutionTimeMs((int) elapsedMs);
            predictionRepository.save(prediction);

            job.setStatus(Status.SUCCESS);
            predictionJobRepository.save(job);

            log.info("[Runner] ✅ Prediction SUCCESS: jobId={}, elapsedMs={}, sql={} chars",
                    jobId, elapsedMs,
                    agentResponse.getResult() != null ? agentResponse.getResult().getSql().length() : 0);

        } catch (Exception ex) {
            // ── 6. Persist failure ────────────────────────────────────────────
            long elapsedMs = System.currentTimeMillis() - startMs;
            String errorJson = buildErrorJson(ex.getMessage(), elapsedMs);

            prediction.setResult(errorJson);
            prediction.setStatus(Status.FAILED.name());
            prediction.setExecutionTimeMs((int) elapsedMs);
            predictionRepository.save(prediction);

            job.setStatus(Status.FAILED);
            predictionJobRepository.save(job);

            log.error("[Runner] ❌ Prediction FAILED: jobId={}, error={}", jobId, ex.getMessage(), ex);
        }
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Helpers
    // ────────────────────────────────────────────────────────────────────────────

    /**
     * Simple regex to extract "in {Region}" from the prompt text.
     * Used as a region_hint for the agent's SQL scoping.
     */
    private String extractRegionHint(String promptText) {
        if (promptText == null || promptText.isEmpty()) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\\b(?:in|around|near)\\s+([A-Z][a-zA-Z\\s,]+?)(?:\\s*[,.]|$)", 0)
                .matcher(promptText);
        return m.find() ? m.group(1).trim() : null;
    }

    /**
     * Serialises the AgentQueryResponse into a JSON string for DB storage.
     * On any serialisation error falls back to a plain JSON object with the raw SQL.
     */
    private String buildResultJson(AgentQueryResponse agentResponse, long elapsedMs) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("agent_request_id", agentResponse.getRequestId());
        result.put("shapefile_path", agentResponse.getShapefilePath());
        result.put("elapsed_ms", elapsedMs);

        if (agentResponse.getResult() != null) {
            AgentQueryResponse.AgentSqlResult sqlResult = agentResponse.getResult();
            result.put("sql", sqlResult.getSql());
            result.put("is_safe", sqlResult.isSafe());
            result.put("reasoning", sqlResult.getReasoning());
            result.put("agent_logs", sqlResult.getAgentLogs());
        }

        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            return "{\"error\": \"Failed to serialize agent response\", \"raw\": \"" + agentResponse.getShapefilePath() + "\"}";
        }
    }

    private String buildErrorJson(String errorMessage, long elapsedMs) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("error", true);
        error.put("message", errorMessage != null ? errorMessage : "Unknown error");
        error.put("elapsed_ms", elapsedMs);
        try {
            return objectMapper.writeValueAsString(error);
        } catch (JsonProcessingException e) {
            return "{\"error\": true, \"message\": \"Serialization failure\"}";
        }
    }
}
