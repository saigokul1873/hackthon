package com.ziprun.engine.ai;

import com.ziprun.engine.exception.ZycusErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class LLMGateway {

    private static final Logger log = LoggerFactory.getLogger(LLMGateway.class);

    @Value("${llm.provider}")
    private String provider;

    @Value("${llm.api-key:}")
    private String apiKey;

    @Value("${llm.model}")
    private String model;

    @Value("${llm.base-url}")
    private String baseUrl;

    @Value("${llm.timeout-seconds:15}")
    private int timeoutSeconds;

    private RestClient http;

    @jakarta.annotation.PostConstruct
    void init() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        this.http = RestClient.builder().requestFactory(factory).build();
    }

    public String callLLM(String prompt) {
        log.info("Calling LLM provider={} model={} promptLength={}", provider, model, prompt.length());
        String response = switch (provider.toLowerCase()) {
            case "gemini" -> callGemini(prompt);
            case "groq"   -> callOpenAICompatible(prompt, baseUrl + "/openai/v1/chat/completions");
            case "ollama" -> callOpenAICompatible(prompt, baseUrl + "/v1/chat/completions");
            default       -> throw ZycusErrorCode.LLM_PROVIDER_UNKNOWN.exception(provider);
        };
        log.info("LLM response received, length={}", response != null ? response.length() : 0);
        return response;
    }

    private String callGemini(String prompt) {
        var url = baseUrl + "/v1beta/models/" + model + ":generateContent?key=" + apiKey;
        var body = Map.of("contents", List.of(
                Map.of("parts", List.of(Map.of("text", prompt)))));

        var resp = http.post().uri(url)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(body).retrieve().body(Map.class);

        try {
            var candidates = (List<?>) resp.get("candidates");
            var content = (Map<?, ?>) ((Map<?, ?>) candidates.get(0)).get("content");
            var parts = (List<?>) content.get("parts");
            return (String) ((Map<?, ?>) parts.get(0)).get("text");
        } catch (Exception e) {
            throw ZycusErrorCode.LLM_PARSE_FAILED.exception(e);
        }
    }

    private String callOpenAICompatible(String prompt, String url) {
        var body = Map.of(
                "model", model,
                "messages", List.of(Map.of("role", "user", "content", prompt)));

        var request = http.post().uri(url)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(body);

        if (!"ollama".equalsIgnoreCase(provider) && apiKey != null && !apiKey.isBlank()) {
            request = request.header("Authorization", "Bearer " + apiKey);
        }

        var resp = request.retrieve().body(Map.class);

        try {
            var choices = (List<?>) resp.get("choices");
            var message = (Map<?, ?>) ((Map<?, ?>) choices.get(0)).get("message");
            return (String) message.get("content");
        } catch (Exception e) {
            throw ZycusErrorCode.LLM_PARSE_FAILED.exception(e);
        }
    }
}
