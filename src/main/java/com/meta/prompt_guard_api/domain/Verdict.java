package com.meta.prompt_guard_api.domain;

import org.springframework.cglib.core.Block;

public enum Verdict {
  ALLOW,
  REVIEW,
  BLOCK
}
