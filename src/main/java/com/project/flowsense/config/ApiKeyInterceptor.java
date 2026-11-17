package com.project.flowsense.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.project.flowsense.dto.response.ErroResponse;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;


@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyInterceptor implements HandlerInterceptor {

    public static final String CABECALHO = "X-API-Key";
    private static final Set<String> METODOS_PROTEGIDOS = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final FlowsenseProperties propriedades;
    private final ObjectMapper objectMapper;

    @PostConstruct
    void avisarSeSemChave() {
        if (!propriedades.apiKeyConfigurada()) {
            log.warn("FLOWSENSE_API_KEY não configurada: todos os comandos (POST/PUT) serão recusados.");
        }
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!METODOS_PROTEGIDOS.contains(request.getMethod())) {
            return true;
        }
        if (chaveValida(request.getHeader(CABECALHO))) {
            return true;
        }

        log.warn("Comando recusado: chave de API ausente ou inválida ({} {} de {})",
                request.getMethod(), request.getRequestURI(), request.getRemoteAddr());
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(
                ErroResponse.de(HttpStatus.UNAUTHORIZED, "Chave de API ausente ou inválida (cabeçalho X-API-Key).")));
        return false;
    }

    private boolean chaveValida(String recebida) {
        if (!propriedades.apiKeyConfigurada() || recebida == null) {
            return false;
        }
       
        return MessageDigest.isEqual(
                recebida.getBytes(StandardCharsets.UTF_8),
                propriedades.apiKey().getBytes(StandardCharsets.UTF_8));
    }
}
