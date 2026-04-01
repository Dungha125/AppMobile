package com.eldercare.service;

import com.eldercare.model.SystemConfig;
import com.eldercare.repository.SystemConfigRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.HttpStatusCodeException;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FoodAiService {

    private final SystemConfigRepository systemConfigRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ai.openai.apiKey:}")
    private String openAiApiKeyFromProps;

    @Value("${ai.openai.model:gpt-4o-mini}")
    private String openAiModel;

    @Data
    @Builder
    @AllArgsConstructor
    public static class FoodAiResult {
        private String foodItemsJson;
        private String note;
    }

    public Optional<FoodAiResult> analyzeMealImage(byte[] imageBytes, String mimeType) {
        String provider = getConfig("ai_provider").orElse("openai").trim().toLowerCase();
        if (!provider.equals("openai")) {
            return Optional.empty();
        }

        String apiKey = getConfig("ai_api_key").orElse(openAiApiKeyFromProps);
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }

        String prompt = getConfig("ai_food_prompt_template").orElse(
                "Bạn là chuyên gia dinh dưỡng. Hãy đọc ảnh bữa ăn và trả về JSON liệt kê các món ăn/đồ uống nhìn thấy.\n" +
                "Yêu cầu:\n" +
                "- Chỉ trả về JSON thuần, không markdown.\n" +
                "- Format: {\"items\":[{\"name\":\"...\",\"confidence\":0.0}],\"note\":\"...\"}\n" +
                "- note: 1 câu ngắn mô tả tổng quan.\n"
        );

        String b64 = Base64.getEncoder().encodeToString(imageBytes);
        String dataUrl = "data:" + (mimeType == null || mimeType.isBlank() ? "image/jpeg" : mimeType) + ";base64," + b64;

        try {
            Map<String, Object> payload = Map.of(
                    "model", openAiModel,
                    "messages", List.of(
                            Map.of(
                                    "role", "user",
                                    "content", List.of(
                                            Map.of("type", "text", "text", prompt),
                                            Map.of("type", "image_url", "image_url", Map.of("url", dataUrl))
                                    )
                            )
                    ),
                    "temperature", 0.2
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey.trim());
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<Map<String, Object>> res = restTemplate.exchange(
                    "https://api.openai.com/v1/chat/completions",
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<>() {}
            );
            if (!res.getStatusCode().is2xxSuccessful() || res.getBody() == null) {
                return Optional.empty();
            }

            Object choicesObj = res.getBody().get("choices");
            if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) return Optional.empty();
            Object first = choices.get(0);
            if (!(first instanceof Map<?, ?> firstMap)) return Optional.empty();
            Object messageObj = firstMap.get("message");
            if (!(messageObj instanceof Map<?, ?> msgMap)) return Optional.empty();
            Object contentObj = msgMap.get("content");
            if (!(contentObj instanceof String content)) return Optional.empty();

            String json = content.trim();
            String note = null;
            // MVP: lưu toàn bộ JSON vào foodItemsJson; note có thể để client parse hoặc giữ nguyên json
            return Optional.of(FoodAiResult.builder()
                    .foodItemsJson(json)
                    .note(note)
                    .build());
        } catch (HttpStatusCodeException e) {
            // log body để dễ debug key / quota / model / policy
            String body = null;
            try { body = e.getResponseBodyAsString(); } catch (Exception ignored) {}
            log.warn("FoodAiService http error: status={} body={}", e.getStatusCode(), body);
            return Optional.empty();
        } catch (Exception e) {
            log.warn("FoodAiService analyze failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<String> getConfig(String key) {
        try {
            return systemConfigRepository.findByConfigKey(key)
                    .map(SystemConfig::getConfigValue)
                    .filter(v -> v != null && !v.isBlank());
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}

