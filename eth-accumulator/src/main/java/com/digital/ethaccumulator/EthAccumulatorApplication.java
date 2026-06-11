package com.digital.ethaccumulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EthAccumulatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(EthAccumulatorApplication.class, args);
    }
}
