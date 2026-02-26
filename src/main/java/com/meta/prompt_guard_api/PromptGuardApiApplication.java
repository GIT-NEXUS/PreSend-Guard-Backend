package com.meta.prompt_guard_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;


@SpringBootApplication
public class PromptGuardApiApplication {

  public static void main(String[] args) {
    SpringApplication.run(PromptGuardApiApplication.class, args);
  }

}
