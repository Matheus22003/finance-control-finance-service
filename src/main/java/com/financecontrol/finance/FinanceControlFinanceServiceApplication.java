package com.financecontrol.finance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FinanceControlFinanceServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinanceControlFinanceServiceApplication.class, args);
    }
}
