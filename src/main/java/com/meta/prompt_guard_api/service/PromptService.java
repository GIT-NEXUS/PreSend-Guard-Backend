package com.meta.prompt_guard_api.service;

import com.meta.prompt_guard_api.dto.PromptRequestDto;
import com.meta.prompt_guard_api.dto.PromptResponseDto;
import com.meta.prompt_guard_api.ner.NerEntityDto;
import com.meta.prompt_guard_api.ner.NerResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.meta.prompt_guard_api.domain.Prompt;
import com.meta.prompt_guard_api.domain.Verdict;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



@Service
@RequiredArgsConstructor
public class PromptService {

    private final NerService nerService;

    // ── Regex 패턴 ───────────────────────────────────────────────────────────
    private static final String PHONE    = "(01[016789])[\\-\\s]?(\\d{3,4})[\\-\\s]?(\\d{4})";
    private static final String EMAIL    = "[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}";
    private static final String RRN      = "\\d{6}-?\\d{7}";
    private static final String CARD     = "\\b\\d{4}[ \\-]\\d{4}[ \\-]\\d{4}[ \\-]\\d{4}\\b";
    // 사업자등록번호(3-2-5)를 먼저 확인해 계좌번호와 중복 방지
    private static final String BIZ_REG  = "\\b\\d{3}-\\d{2}-\\d{5}\\b";
    private static final String ACCOUNT  = "\\b\\d{3,6}-\\d{2,6}-\\d{2,6}(?:-\\d{2,3})?\\b";
    private static final String IP       = "\\b(?:(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\b";
    private static final String PASSPORT = "\\b[A-Z]{1,2}\\d{7,8}\\b";

    @Transactional
    public PromptResponseDto analyze(PromptRequestDto dto) {
        String text = dto.getText();

        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text는 필수입니다.");
        }

        // ── 1차: Regex 검출 ──────────────────────────────────────────────────
        int phone    = count(text, PHONE);
        int email    = count(text, EMAIL);
        int rrn      = count(text, RRN);
        int card     = count(text, CARD);
        int bizReg   = count(text, BIZ_REG);
        // 사업자등록번호 매칭분은 계좌번호에서 제외
        int account  = Math.max(0, count(text, ACCOUNT) - bizReg);
        int ip       = count(text, IP);
        int passport = count(text, PASSPORT);

        // ── 2차: NER 검출 ────────────────────────────────────────────────────
        int nerName = 0;
        int nerOrg  = 0;
        int nerLoc  = 0;

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

        // ── 위험도 점수 계산 ─────────────────────────────────────────────────
        boolean hasPii = phone > 0 || email > 0 || rrn > 0 ||
                card > 0 || bizReg > 0 || account > 0 || ip > 0 || passport > 0 ||
                nerName > 0 || nerOrg > 0 || nerLoc > 0;

        int score = 0;
        if (hasPii) {
            score = Math.min(
                    phone    * 30 +
                    email    * 20 +
                    rrn      * 80 +
                    card     * 70 +
                    account  * 60 +
                    bizReg   * 40 +
                    ip       * 15 +
                    passport * 70 +
                    nerName  * 15 +
                    nerOrg   * 10 +
                    nerLoc   * 10,
                    100
            );
        }

        String action = !hasPii ? "ALLOW" : (score >= 70 ? "WARN" : "MASK");

        // ── 마스킹 ───────────────────────────────────────────────────────────
        String masked = text;
        if (!action.equals("ALLOW")) {
            masked = masked.replaceAll(PHONE,    "$1-****-****");
            masked = masked.replaceAll(EMAIL,    "****@****");
            masked = masked.replaceAll(RRN,      "******-*******");
            masked = masked.replaceAll(CARD,     "****-****-****-****");
            // 사업자등록번호 먼저 마스킹 후 계좌번호 처리 (중복 방지)
            masked = masked.replaceAll(BIZ_REG,  "***-**-*****");
            masked = masked.replaceAll(ACCOUNT,  "****-****-******");
            masked = masked.replaceAll(IP,       "*.*.*.*");
            masked = masked.replaceAll(PASSPORT, "**-*******");
        }

        if(!action .equals("ALLOW")) {
            Prompt prompt = new Prompt(
                masked,
                Verdict.valueOf(action),
                score,
                "phone: " + phone + ", email: " + email + ", rrn: " + rrn
            );
            promptRepository.save(prompt);
        }

        return new PromptResponseDto(
                score, action, masked,
                phone, email, rrn,
                card, account, ip, bizReg, passport
        );
    }

    private int count(String text, String regex) {
        Matcher m = Pattern.compile(regex).matcher(text);
        int c = 0;
        while (m.find()) c++;
        return c;
    }
}
