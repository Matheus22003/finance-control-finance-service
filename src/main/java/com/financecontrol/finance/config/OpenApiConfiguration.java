package com.financecontrol.finance.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Finance Control - Finance Service",
                version = "v1",
                description = "Personal income, expense, category and monthly summary API."),
        servers = @Server(url = "/", description = "Current host"))
public class OpenApiConfiguration {
}
