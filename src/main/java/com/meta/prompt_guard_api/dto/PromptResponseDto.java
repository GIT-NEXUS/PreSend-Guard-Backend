package com.meta.prompt_guard_api.dto;

import lombok.Getter;

@Getter
public class PromptResponseDto {

    private final int score;
    private final String action;
    private final String maskedText;

    // regex 검출 카운트
    private final int phoneCount;
    private final int emailCount;
    private final int rrnCount;
    private final int cardCount;
    private final int accountCount;
    private final int ipCount;
    private final int bizRegCount;
    private final int passportCount;

    public PromptResponseDto(
            int score,
            String action,
            String maskedText,
            int phoneCount,
            int emailCount,
            int rrnCount,
            int cardCount,
            int accountCount,
            int ipCount,
            int bizRegCount,
            int passportCount
    ) {
        this.score = score;
        this.action = action;
        this.maskedText = maskedText;
        this.phoneCount = phoneCount;
        this.emailCount = emailCount;
        this.rrnCount = rrnCount;
        this.cardCount = cardCount;
        this.accountCount = accountCount;
        this.ipCount = ipCount;
        this.bizRegCount = bizRegCount;
        this.passportCount = passportCount;
    }
}
