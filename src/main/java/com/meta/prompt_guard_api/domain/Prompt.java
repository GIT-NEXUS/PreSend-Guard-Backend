package com.meta.prompt_guard_api.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 사용자가 입력한 프롬프트 하나 DB에 하나 저장
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "prompt")
public class Prompt extends TimeStamped {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)       // DB가 알아서 id값 생성 각프롬프트의 고유 번호 1,2,3...
  private Long id;

  @Column(name = "prompt", nullable = false, length = 5000)
  private String prompt;

  @Enumerated(EnumType.STRING)
  @Column(name = "verdict", nullable = false, length = 20) // 이 프롬프트를 시스템이 어떻게 판단했는지
  private Verdict verdict;

  @Column(name = "risk_score", nullable = false)  // 이 프롬프트가 위험한지를 숫자로 표현
  private Integer riskScore;

  @Column(name = "reasons", columnDefinition = "TEXT")
  private String reasons;

  public Prompt(String prompt, Verdict verdict, Integer riskScore, String reasons) {
    this.prompt = prompt;
    this.verdict = verdict;
    this.riskScore = riskScore;
    this.reasons = reasons;
  }
}
