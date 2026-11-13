package com.booth.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BoothAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(BoothAgentApplication.class, args);
    }
}
