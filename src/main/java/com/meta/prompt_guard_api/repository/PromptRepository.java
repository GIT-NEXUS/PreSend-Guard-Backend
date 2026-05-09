package com.meta.prompt_guard_api.repository;

import com.meta.prompt_guard_api.domain.Prompt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromptRepository extends JpaRepository<Prompt, Long> {
}
