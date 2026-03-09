package com.genflow.genflowai.service.implementation;

import com.genflow.genflowai.dto.*;
import com.genflow.genflowai.entity.*;
import com.genflow.genflowai.entity.enums.Status;
import com.genflow.genflowai.exceptions.ResourceNotFoundException;
import com.genflow.genflowai.repository.*;
import com.genflow.genflowai.service.PredictionRunnerService;
import com.genflow.genflowai.service.PredictionService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Handles prediction submission and querying.
 *
 * On submitPrediction():
 *   1. Creates a PredictionJob (PENDING)
 *   2. Creates a Prediction record (status = PENDING)
 *   3. Fires PredictionRunnerService.runPredictionAsync() on a background thread
 *   4. Returns the PredictionJob response immediately (non-blocking)
 *
 * The PredictionRunnerService then drives:
 *   PENDING → RUNNING → SUCCESS | FAILED
 * and writes the agent's SQL result into Prediction.result as JSON.
 */
@Service
public class PredictionServiceImpl implements PredictionService {

    private static final Logger log = LoggerFactory.getLogger(PredictionServiceImpl.class);

    private final PredictionRepository predictionRepository;
    private final PredictionJobRepository predictionJobRepository;
    private final PromptRepository promptRepository;
    private final ModelRepository modelRepository;
    private final PredictionRunnerService predictionRunnerService;

    public PredictionServiceImpl(
            PredictionRepository predictionRepository,
            PredictionJobRepository predictionJobRepository,
            PromptRepository promptRepository,
            ModelRepository modelRepository,
            PredictionRunnerService predictionRunnerService) {

        this.predictionRepository = predictionRepository;
        this.predictionJobRepository = predictionJobRepository;
        this.promptRepository = promptRepository;
        this.modelRepository = modelRepository;
        this.predictionRunnerService = predictionRunnerService;
    }

    // =========================================================
    //  SUBMIT — creates Job + Prediction, fires async runner
    // =========================================================

    @Override
    @Transactional
    public PredictionJobResponse submitPrediction(PredictionRequest request) {
        // Load required entities
        Prompt prompt = promptRepository.findById(request.getPromptId())
                .orElseThrow(() -> new ResourceNotFoundException("Prompt not found: " + request.getPromptId()));

        Model model = modelRepository.findById(request.getModelId())
                .orElseThrow(() -> new ResourceNotFoundException("Model not found: " + request.getModelId()));

        // ── Create PredictionJob (tracks async execution) ──────────────────
        PredictionJob job = new PredictionJob();
        job.setPrompt(prompt);
        job.setStatus(Status.PENDING);
        PredictionJob savedJob = predictionJobRepository.save(job);

        // ── Create Prediction (stores the result) ──────────────────────────
        Prediction prediction = new Prediction();
        prediction.setPrompt(prompt);
        prediction.setModel(model);
        prediction.setStatus(Status.PENDING.name());
        Prediction savedPrediction = predictionRepository.save(prediction);

        log.info("[PredictionService] Submitted: jobId={}, predictionId={}, promptId={}, modelId={}",
                savedJob.getId(), savedPrediction.getId(), prompt.getId(), model.getId());

        // ── Fire async agent pipeline (returns immediately) ────────────────
        predictionRunnerService.runPredictionAsync(savedJob.getId(), savedPrediction.getId());

        return mapJobResponse(savedJob);
    }

    // =========================================================
    //  QUERIES
    // =========================================================

    @Override
    public PredictionResponse getPredictionById(UUID predictionId) {
        Prediction prediction = predictionRepository.findById(predictionId)
                .orElseThrow(() -> new ResourceNotFoundException("Prediction not found: " + predictionId));
        return mapPredictionResponse(prediction);
    }

    @Override
    public PageResponse<PredictionResponse> getAllPredictions(int page, int size) {
        Page<Prediction> predictions = predictionRepository.findAll(PageRequest.of(page, size));
        return toPageResponse(predictions);
    }

    @Override
    public PageResponse<PredictionResponse> getPredictionsByPrompt(UUID promptId, int page, int size) {
        Page<Prediction> predictions = predictionRepository
                .findByPrompt_Id(promptId, PageRequest.of(page, size));
        return toPageResponse(predictions);
    }

    @Override
    public PredictionJobResponse getJobStatus(UUID jobId) {
        PredictionJob job = predictionJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));
        return mapJobResponse(job);
    }

    // =========================================================
    //  MAPPERS
    // =========================================================

    private PageResponse<PredictionResponse> toPageResponse(Page<Prediction> page) {
        return new PageResponse<>(
                page.getContent().stream().map(this::mapPredictionResponse).collect(Collectors.toList()),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    private PredictionResponse mapPredictionResponse(Prediction prediction) {
        return new PredictionResponse(
                prediction.getId(),
                prediction.getPrompt().getId(),
                prediction.getModel().getId(),
                prediction.getResult(),
                prediction.getStatus(),
                prediction.getExecutionTimeMs(),
                prediction.getCreatedAt()
        );
    }

    private PredictionJobResponse mapJobResponse(PredictionJob job) {
        return new PredictionJobResponse(
                job.getId(),
                job.getPrompt().getId(),
                job.getStatus().name(),
                job.getStartedAt(),
                job.getFinishedAt()
        );
    }
}