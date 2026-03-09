package com.genflow.genflowai.controller;

import com.genflow.genflowai.dto.PageResponse;
import com.genflow.genflowai.dto.PromptRequest;
import com.genflow.genflowai.dto.PromptResponse;
import com.genflow.genflowai.service.PromptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/prompts")
@Tag(name = "Prompts", description = "Prompt management APIs")
@RequiredArgsConstructor
public class PromptController {

    private final PromptService promptService;

    @PostMapping
    @Operation(summary = "Create prompt", description = "Submit a new prompt for a dataset")
    public ResponseEntity<PromptResponse> createPrompt(
            @RequestBody PromptRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(promptService.createPrompt(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get prompt by ID", description = "Get prompt details by ID")
    public ResponseEntity<PromptResponse> getPrompt(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(promptService.getPromptById(id));
    }

    @GetMapping
    @Operation(summary = "Get all prompts", description = "Get paginated list of prompts")
    public ResponseEntity<PageResponse<PromptResponse>> getAllPrompts(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(promptService.getAllPrompts(page, size));
    }

    @GetMapping("/dataset/{datasetId}")
    @Operation(summary = "Get prompts by dataset", description = "Get paginated list of prompts for a dataset")
    public ResponseEntity<PageResponse<PromptResponse>> getPromptsByDataset(
            @PathVariable("datasetId") UUID datasetId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(promptService.getPromptsByDataset(datasetId, page, size));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete prompt", description = "Delete prompt by ID")
    public ResponseEntity<Void> deletePrompt(@PathVariable("id") UUID id) {
        promptService.deletePrompt(id);
        return ResponseEntity.noContent().build();
    }
}
