package com.varenabled.livefeed;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LiveFeedProducerApplication {

    public static void main(String[] args) {
        SpringApplication.run(LiveFeedProducerApplication.class, args);
    }
}
