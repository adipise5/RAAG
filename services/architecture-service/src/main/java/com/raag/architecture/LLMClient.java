package com.raag.architecture;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class LLMClient {

    private final WebClient webClient;

    public LLMClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public Map<String, Object> analyzeRequirements(List<String> requirements) {
        Map<String, Object> request = new HashMap<>();
        request.put("project_id", UUID.randomUUID().toString());
        request.put("requirements", requirements == null ? List.of() : requirements);
        return postForMap("/analyze", request);
    }

    public Map<String, Object> callCustomPrompt(String prompt) {
        Map<String, String> request = Map.of("prompt", prompt == null ? "" : prompt);
        return postForMap("/llm/generate", request);
    }

    public Map<String, Object> analyzeEnhancedQuality(List<String> requirements, String projectId) {
        Map<String, Object> request = new HashMap<>();
        request.put("project_id", projectId);
        request.put("requirements", requirements == null ? List.of() : requirements);
        return postForMap("/quality/enhanced", request);
    }

    public Map<String, Object> analyzeRequirementGaps(List<String> requirements) {
        Map<String, Object> request = new HashMap<>();
        request.put("requirements", requirements == null ? List.of() : requirements);
        return postForMap("/gap-analysis", request);
    }

    public Map<String, Object> analyzeTraceability(List<String> requirements, List<String> architectureComponents) {
        Map<String, Object> request = new HashMap<>();
        request.put("requirements", requirements == null ? List.of() : requirements);
        request.put("architecture_components", architectureComponents == null ? List.of() : architectureComponents);
        return postForMap("/traceability-matrix", request);
    }

    public Map<String, Object> analyzeRiskAssumptions(String projectDescription, List<String> requirements) {
        Map<String, Object> request = new HashMap<>();
        request.put("project_description", projectDescription == null ? "" : projectDescription);
        request.put("requirements", requirements == null ? List.of() : requirements);
        return postForMap("/risk-assumptions", request);
    }

    public Map<String, Object> estimateComplexity(List<String> requirements) {
        Map<String, Object> request = new HashMap<>();
        request.put("requirements", requirements == null ? List.of() : requirements);
        return postForMap("/complexity-estimation", request);
    }

    public Map<String, Object> assessNovelty(String projectDescription, String domain, List<String> requirements) {
        Map<String, Object> request = new HashMap<>();
        request.put("project_description", projectDescription == null ? "" : projectDescription);
        request.put("domain", domain == null ? "General" : domain);
        request.put("requirements", requirements == null ? List.of() : requirements);
        return postForMap("/novelty-assessment", request);
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> readListOfMaps(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> m) {
                out.add((Map<String, Object>) m);
            }
        }
        return out;
    }

    private Map<String, Object> postForMap(String uri, Object payload) {
        try {
            return webClient.post()
                    .uri(uri)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(Duration.ofSeconds(12))
                    .onErrorResume(err -> {
                        Map<String, Object> fallback = new HashMap<>();
                        fallback.put("error", "LLM request failed");
                        fallback.put("details", err.getMessage() == null ? "unknown error" : err.getMessage());
                        return Mono.just(fallback);
                    })
                    .blockOptional(Duration.ofSeconds(15))
                    .orElseGet(HashMap::new);
        } catch (Exception e) {
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("error", "LLM request exception");
            fallback.put("details", e.getMessage() == null ? "unknown error" : e.getMessage());
            return fallback;
        }
    }
}
