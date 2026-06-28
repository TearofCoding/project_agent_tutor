package com.aitutor.client;

import com.aitutor.dto.FastApiSessionResponse;
import com.aitutor.dto.SessionAskPayload;
import com.aitutor.dto.SessionEndPayload;
import com.aitutor.dto.SessionStartPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.function.Supplier;

@Slf4j
@Component
public class AIAgentClient {

    private final RestClient restClient;

    private static final FastApiSessionResponse FALLBACK_START = new FastApiSessionResponse(
            null, null, null, null,
            new FastApiSessionResponse.ProblemInfo(
                    "클래스와 인터페이스의 차이점을 설명하세요.", "클래스"),
            null, "fallback", 0);

    private static final FastApiSessionResponse FALLBACK_ASK = new FastApiSessionResponse(
            null, null, false,
            "답변 평가 중 오류가 발생했습니다. 다음 문제로 넘어갑니다.",
            new FastApiSessionResponse.ProblemInfo(
                    "클래스와 인터페이스의 차이점을 설명하세요.", "클래스"),
            null, "fallback", 0);

    public AIAgentClient(@Value("${FASTAPI_BASE_URL}") String fastApiBaseUrl,
                         @Value("${INTERNAL_API_KEY}") String internalApiKey) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(10000);

        this.restClient = RestClient.builder()
                .baseUrl(fastApiBaseUrl)
                .requestFactory(factory)
                .defaultHeader("X-Internal-Api-Key", internalApiKey)
                .build();
    }

    public FastApiSessionResponse startSession(SessionStartPayload payload) {
        return executeWithRetry(
                () -> restClient.post()
                        .uri("/internal/sessions/start")
                        .body(payload)
                        .retrieve()
                        .body(FastApiSessionResponse.class),
                FALLBACK_START);
    }

    public FastApiSessionResponse submitAnswer(Long sessionId, SessionAskPayload payload) {
        return executeWithRetry(
                () -> restClient.post()
                        .uri("/internal/sessions/{id}/ask", sessionId)
                        .body(payload)
                        .retrieve()
                        .body(FastApiSessionResponse.class),
                FALLBACK_ASK);
    }

    public void triggerPipeline() {
        restClient.post()
                .uri("/internal/skills/pipeline/trigger")
                .retrieve()
                .toBodilessEntity();
    }

    @Async
    public void endSession(Long sessionId, SessionEndPayload payload) {
        try {
            restClient.post()
                    .uri("/internal/sessions/{id}/end", sessionId)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("endSession fire-and-forget failed (sessionId={}): {}", sessionId, e.getMessage());
        }
    }

    private <T> T executeWithRetry(Supplier<T> operation, T fallback) {
        try {
            return operation.get();
        } catch (ResourceAccessException | HttpServerErrorException e) {
            log.warn("FastAPI call failed ({}), retrying once...", e.getMessage());
            try {
                return operation.get();
            } catch (Exception retryEx) {
                log.error("FastAPI retry failed: {}", retryEx.getMessage());
                return fallback;
            }
        } catch (Exception e) {
            log.error("FastAPI call failed (non-retryable): {}", e.getMessage());
            return fallback;
        }
    }
}
