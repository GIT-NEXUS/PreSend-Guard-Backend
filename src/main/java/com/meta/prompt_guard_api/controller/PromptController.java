package com.meta.prompt_guard_api.controller;

import com.meta.prompt_guard_api.dto.PromptRequestDto;
import com.meta.prompt_guard_api.dto.PromptResponseDto;
import com.meta.prompt_guard_api.service.PromptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PromptController {

    private final PromptService promptService;

    @PostMapping("/analyze")
    public ResponseEntity<PromptResponseDto> analyze(@RequestBody PromptRequestDto dto){
        PromptResponseDto promptResponseDto = promptService.analyze(dto);
        return ResponseEntity.ok(promptResponseDto);
    }
}
