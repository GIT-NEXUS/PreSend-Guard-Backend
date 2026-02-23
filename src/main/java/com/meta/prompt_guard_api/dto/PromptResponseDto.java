package com.meta.prompt_guard_api.dto;

import lombok.Getter;

@Getter
public class PromptResponseDto {

    private final int score;
    private final String action;
    private final String maskedText;

    private final int phoneCount;
    private final int emailCount;
    private final int rrnCount;

    public PromptResponseDto(
            int score,
            String action,
            String maskedText,
            int phoneCount,
            int emailCount,
            int rrnCount
    ){
        this.score=score;
        this.action=action;
        this.maskedText=maskedText;
        this.phoneCount=phoneCount;
        this.emailCount=emailCount;
        this.rrnCount=rrnCount;
    }
}
