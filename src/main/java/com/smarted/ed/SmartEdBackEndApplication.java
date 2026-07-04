package com.smarted.ed;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SmartEdBackEndApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartEdBackEndApplication.class, args);
    }

}
