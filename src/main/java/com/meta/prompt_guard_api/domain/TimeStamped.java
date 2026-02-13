
package com.meta.prompt_guard_api.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass  // JPA Entity 클래스들이 TimeStamped 를 상속할 경우 필드들도 칼럼으로 인식하게 함
@EntityListeners(AuditingEntityListener.class)

// 모든 테이블에 공통으로 들어가는 시간 컬럼을 중복 없이 재상하려고 사용
public class TimeStamped {

  @CreatedDate
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;  // 언제생성되는지

  @LastModifiedDate
  @Column(nullable = false)
  private LocalDateTime updatedAt; // 언제 수정되는지
}
