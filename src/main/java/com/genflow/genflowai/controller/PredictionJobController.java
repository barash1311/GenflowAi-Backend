package com.genflow.genflowai.controller;

import com.genflow.genflowai.dto.PredictionJobResponse;
import com.genflow.genflowai.service.PredictionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/prediction-jobs")
@Tag(name = "Prediction Jobs", description = "Prediction job tracking APIs")
@RequiredArgsConstructor
public class PredictionJobController {

    private final PredictionService predictionService;

    @GetMapping("/{id}")
    @Operation(summary = "Get job status", description = "Get prediction job status by ID")
    public ResponseEntity<PredictionJobResponse> getJobStatus(
            @PathVariable("id") UUID id) {
        return ResponseEntity.ok(predictionService.getJobStatus(id));
    }
}
