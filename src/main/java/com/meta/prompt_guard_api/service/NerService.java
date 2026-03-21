package com.meta.prompt_guard_api.service;

import com.meta.prompt_guard_api.ner.NerRequestDto;
import com.meta.prompt_guard_api.ner.NerResponseDto;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class NerService {

    private final RestTemplate restTemplate = new RestTemplate();

    public NerResponseDto analyze(String text) {

        String url = "http://localhost:8000/analyze";

        NerRequestDto request = new NerRequestDto();
        request.setText(text);

        return restTemplate.postForObject(
                url,
                request,
                NerResponseDto.class
        );
    }
}
