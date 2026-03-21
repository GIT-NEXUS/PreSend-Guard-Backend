package com.meta.prompt_guard_api.service;

import com.meta.prompt_guard_api.dto.PromptRequestDto;
import com.meta.prompt_guard_api.dto.PromptResponseDto;
import com.meta.prompt_guard_api.ner.NerEntityDto;
import com.meta.prompt_guard_api.ner.NerResponseDto;
import com.meta.prompt_guard_api.repository.PromptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PromptService {

    // private final PromptRepository promptRepository;
    private final NerService nerService;

    @Transactional
    public PromptResponseDto analyze(PromptRequestDto dto) {
        String text = dto.getText();

        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text는 필수입니다.");
        }

        // 1차: regex 검사
        int phone = count(text, "(01[016789])[-\\s]?(\\d{3,4})[-\\s]?(\\d{4})");
        int email = count(text, "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
        int rrn = count(text, "\\d{6}-?\\d{7}");

        // 2차: NER 호출
        int nerName = 0;
        int nerOrg = 0;
        int nerLoc = 0;

        try {
            NerResponseDto nerResult = nerService.analyze(text);

            if (nerResult != null && nerResult.getEntities() != null) {
                for (NerEntityDto entity : nerResult.getEntities()) {
                    String label = entity.getLabel();

                    if ("PER".equals(label)) nerName++;
                    if ("ORG".equals(label)) nerOrg++;
                    if ("LOC".equals(label)) nerLoc++;
                }
            }
        } catch (Exception e) {
            System.out.println("NER 서버 호출 실패: " + e.getMessage());
        }

        // regex + NER 결과 합치기
        boolean hasPii =
                phone > 0 || email > 0 || rrn > 0 ||
                        nerName > 0 || nerOrg > 0 || nerLoc > 0;

        int score = 0;
        if (hasPii) {
            score = Math.min(
                    phone * 30 +
                            email * 20 +
                            rrn * 80 +
                            nerName * 15 +
                            nerOrg * 10 +
                            nerLoc * 10,
                    100
            );
        }

        String action = !hasPii ? "ALLOW" : (score >= 70 ? "WARN" : "MASK");

        String masked = text;

        if (!action.equals("ALLOW")) {
            masked = masked.replaceAll("(01[016789])[-\\s]?(\\d{3,4})[-\\s]?(\\d{4})", "$1-****-****");
            masked = masked.replaceAll("([A-Za-z0-9])([A-Za-z0-9._%+-]*)(@[A-Za-z0-9.-]+\\.[A-Za-z]{2,})", "$1***$3");
            masked = masked.replaceAll("(\\d{6})-?(\\d{7})", "$1-*******");
        }

        // TODO: DB 저장

        return new PromptResponseDto(
                score,
                action,
                masked,
                phone,
                email,
                rrn
        );
    }

    private int count(String text, String regex) {
        Matcher m = Pattern.compile(regex).matcher(text);
        int c = 0;
        while (m.find()) c++;
        return c;
    }
}

