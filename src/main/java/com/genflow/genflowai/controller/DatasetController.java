package com.genflow.genflowai.controller;

import com.genflow.genflowai.dto.DatasetRequest;
import com.genflow.genflowai.dto.DatasetResponse;
import com.genflow.genflowai.dto.PageResponse;
import com.genflow.genflowai.service.DatasetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/datasets")
@Tag(name = "Datasets", description = "Dataset management APIs")
@RequiredArgsConstructor
public class DatasetController {

    private final DatasetService datasetService;

    @PostMapping
    @Operation(summary = "Create dataset", description = "Upload dataset metadata")
    public ResponseEntity<DatasetResponse> createDataset(
            @RequestBody DatasetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(datasetService.createDataset(request));
    }

    @GetMapping
    @Operation(summary = "Get all datasets", description = "Get paginated list of datasets")
    public ResponseEntity<PageResponse<DatasetResponse>> getAllDatasets(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(datasetService.getAllDatasets(page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get dataset by ID", description = "Get dataset details by ID")
    public ResponseEntity<DatasetResponse> getDataset(@PathVariable UUID id) {
        return ResponseEntity.ok(datasetService.getDatasetById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update dataset", description = "Update dataset metadata")
    public ResponseEntity<DatasetResponse> updateDataset(
            @PathVariable UUID id,
            @RequestBody DatasetRequest request) {
        return ResponseEntity.ok(datasetService.updateDataset(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete dataset", description = "Delete dataset by ID")
    public ResponseEntity<Void> deleteDataset(@PathVariable UUID id) {
        datasetService.deleteDataset(id);
        return ResponseEntity.noContent().build();
    }
}