package com.genflow.genflowai.controller;

import com.genflow.genflowai.dto.ModelRequest;
import com.genflow.genflowai.dto.ModelResponse;
import com.genflow.genflowai.dto.PageResponse;
import com.genflow.genflowai.service.ModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/models")
@Tag(name = "Models", description = "ML model APIs")
@RequiredArgsConstructor
public class ModelController {

    private final ModelService modelService;

    @PostMapping
    @Operation(summary = "Create model", description = "Register new ML model (ADMIN only)")
    public ResponseEntity<ModelResponse> createModel(@RequestBody ModelRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(modelService.createModel(request));
    }

    @GetMapping
    @Operation(summary = "Get all models", description = "Get paginated list of models")
    public ResponseEntity<PageResponse<ModelResponse>> getAllModels(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(modelService.getAllModels(page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get model by ID", description = "Get model details by ID")
    public ResponseEntity<ModelResponse> getModel(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(modelService.getModelById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update model", description = "Update model information (ADMIN only)")
    public ResponseEntity<ModelResponse> updateModel(
            @PathVariable("id") UUID id,
            @RequestBody ModelRequest request) {
        return ResponseEntity.ok(modelService.updateModel(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete model", description = "Delete model by ID (ADMIN only)")
    public ResponseEntity<Void> deleteModel(@PathVariable("id") UUID id) {
        modelService.deleteModel(id);
        return ResponseEntity.noContent().build();
    }
}