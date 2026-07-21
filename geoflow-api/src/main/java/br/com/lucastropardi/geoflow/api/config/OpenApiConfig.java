package br.com.lucastropardi.geoflow.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI geoflowOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("GeoFlow API")
                        .version("v1")
                        .description("Initial HTTP API for GeoFlow job orchestration"));
    }
}
