package com.meta.prompt_guard_api.ner;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class NerResponseDto {
    private String message;
    private String input_text;
    private List<NerEntityDto> entities;
    private String masked_text;
}
