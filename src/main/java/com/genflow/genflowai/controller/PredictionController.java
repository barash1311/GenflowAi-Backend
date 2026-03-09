package com.genflow.genflowai.controller;

import com.genflow.genflowai.dto.PageResponse;

import com.genflow.genflowai.dto.PredictionRequest;
import com.genflow.genflowai.dto.PredictionResponse;
import com.genflow.genflowai.service.PredictionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/predictions")
@Tag(name = "Predictions", description = "Prediction result APIs")
@RequiredArgsConstructor
public class PredictionController {

    private final PredictionService predictionService;

    @PostMapping
    @Operation(summary = "Submit prediction", description = "Submit new prediction request")
    public ResponseEntity<?> submitPrediction(
            @RequestBody PredictionRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(predictionService.submitPrediction(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get prediction by ID", description = "Get prediction result by ID")
    public ResponseEntity<PredictionResponse> getPrediction(
            @PathVariable("id") UUID id) {
        return ResponseEntity.ok(predictionService.getPredictionById(id));
    }

    @GetMapping
    @Operation(summary = "Get all predictions", description = "Get paginated list of predictions")
    public ResponseEntity<PageResponse<PredictionResponse>> getAllPredictions(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(predictionService.getAllPredictions(page, size));
    }

    @GetMapping("/prompt/{promptId}")
    @Operation(summary = "Get predictions by prompt", description = "Get paginated list of predictions for a prompt")
    public ResponseEntity<PageResponse<PredictionResponse>> getPredictionsByPrompt(
            @PathVariable("promptId") UUID promptId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(predictionService.getPredictionsByPrompt(promptId, page, size));
    }
}
