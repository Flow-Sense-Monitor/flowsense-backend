package com.project.flowsense.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.RequiredArgsConstructor;

/**
 * Configuração web: CORS restrito às origens do frontend e a checagem da chave de API.
 * Em produção o nginx serve o frontend e a API na mesma origem, então o CORS só é usado
 * quando o frontend roda em outro endereço (ex.: npm run dev na porta 5173).
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final FlowsenseProperties propriedades;
    private final ApiKeyInterceptor apiKeyInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(propriedades.corsOrigens().toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "OPTIONS")
                .allowedHeaders("Content-Type", ApiKeyInterceptor.CABECALHO);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiKeyInterceptor).addPathPatterns("/api/**");
    }
}
