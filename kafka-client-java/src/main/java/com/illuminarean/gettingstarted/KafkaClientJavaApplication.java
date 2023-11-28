package com.illuminarean.gettingstarted;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class KafkaClientJavaApplication {
    public static void main(String[] args) {
        SpringApplication.run(KafkaClientJavaApplication.class, args);
    }
}
