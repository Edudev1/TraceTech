package com.tracetech.eah.tracetecheah.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.tracetech.eah.tracetecheah")
@EnableJpaRepositories(basePackages = "com.tracetech.eah.tracetecheah.common.repository")
@EntityScan(basePackages = "com.tracetech.eah.tracetecheah.common.entity")
public class TraceTechWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(TraceTechWebApplication.class, args);
    }
}
