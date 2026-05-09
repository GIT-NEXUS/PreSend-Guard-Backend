package com.meta.prompt_guard_api.ner;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NerEntityDto {
    private String text;
    private String label;
    private double score;
    private int start;
    private int end;
}
