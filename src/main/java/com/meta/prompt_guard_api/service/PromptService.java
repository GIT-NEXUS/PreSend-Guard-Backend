package com.meta.prompt_guard_api.service;

import com.meta.prompt_guard_api.dto.PromptRequestDto;
import com.meta.prompt_guard_api.dto.PromptResponseDto;
import com.meta.prompt_guard_api.repository.PromptRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
@RequiredArgsConstructor
public class PromptService {

    private final PromptRepository promptRepository; // DB랑 연결할거임

    @Transactional
    public PromptResponseDto analyze(PromptRequestDto dto){
        String text = dto.getText(); // dto에 text 필드가 있다고 가정하고 작성함
        if(text == null || text.isBlank()){
            throw new IllegalArgumentException("text는 필수입니다.");
        }

        int phone = count(text,"(01[016789])[-\\s]?(\\d{3,4})[-\\s]?(\\d{4})");
        int email = count(text,"[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
        int rrn = count(text,"\\d{6}-?\\d{7}");

        boolean hasPii = phone > 0 || email > 0 || rrn > 0;

        int score = hasPii ? Math.min(phone * 30 + email * 20 + rrn * 80, 100) : 0;

        String action = !hasPii ? "ALLOW" : (score >= 70 ? "WARN" : "MASK");

        String masked =text;
        if(!action.equals("ALLOW")){
            masked = masked.replaceAll("(01[016789])[-\\s]?(\\d{3,4})[-\\s]?(\\d{4})", "$1-****-****");
            masked = masked.replaceAll("([A-Za-z0-9])([A-Za-z0-9._%+-]*)(@[A-Za-z0-9.-]+\\.[A-Za-z]{2,})", "$1***$3");
            masked = masked.replaceAll("(\\d{6})-?(\\d{7})", "$1-*******");
        }

        // DB 저장 - Prompt 엔티티 구조에 맞춰서 저장 할 예정

        return new PromptResponseDto(score, action, masked, phone, email, rrn); // 이 순서/필드가 실제 DTO랑 비교해야함

    }

    private int count(String text, String regex) {
        Matcher m = Pattern.compile(regex).matcher(text);
        int c = 0;
        while (m.find()) c++;
        return c;
    }

}
