package com.eldercare.service;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FoodAiService {

    private final RestTemplate restTemplate = new RestTemplate();

    // Hard-coded AI config for meal analysis (edit here directly when needed).
    private static final String HARD_CODED_PROVIDER = "google"; // openai | google
    private static final String HARD_CODED_OPENAI_API_KEY = "";
    private static final String HARD_CODED_OPENAI_MODEL = "gpt-4o-mini";
    private static final String HARD_CODED_GOOGLE_API_KEY = "AIzaSyCSvlCzs_ga7rckCwT3jor1fmdMNZz72ZY";
    private static final String HARD_CODED_GOOGLE_MODEL = "gemini-3-flash-preview";
    private static final String HARD_CODED_MEAL_PROMPT =
            "Bạn là chuyên gia dinh dưỡng. Hãy đọc ảnh bữa ăn và trả về JSON liệt kê các món ăn/đồ uống nhìn thấy.\n" +
            "Yêu cầu:\n" +
            "- Chỉ trả về JSON thuần, không markdown.\n" +
            "- Format: {\"items\":[{\"name\":\"...\",\"confidence\":0.0}],\"note\":\"...\"}\n" +
            "- note: 1 câu ngắn mô tả tổng quan.\n";

    @Data
    @Builder
    @AllArgsConstructor
    public static class FoodAiResult {
        private String foodItemsJson;
        private String note;
    }

    public Optional<FoodAiResult> analyzeMealImage(byte[] imageBytes, String mimeType) {
        String provider = HARD_CODED_PROVIDER.trim().toLowerCase();
        if (provider.equals("google") || provider.equals("gemini") || provider.equals("google_ai_studio")) {
            return analyzeWithGemini(imageBytes, mimeType);
        }
        if (!provider.equals("openai")) return Optional.empty();

        String apiKey = HARD_CODED_OPENAI_API_KEY;
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }

        String prompt = HARD_CODED_MEAL_PROMPT;

        String b64 = Base64.getEncoder().encodeToString(imageBytes);
        String dataUrl = "data:" + (mimeType == null || mimeType.isBlank() ? "image/jpeg" : mimeType) + ";base64," + b64;

        try {
            Map<String, Object> payload = Map.of(
                    "model", HARD_CODED_OPENAI_MODEL,
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
            return Optional.of(FoodAiResult.builder()
                    .foodItemsJson(json)
                    .note(null)
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

    private Optional<FoodAiResult> analyzeWithGemini(byte[] imageBytes, String mimeType) {
        String apiKey = HARD_CODED_GOOGLE_API_KEY;
        if (apiKey == null || apiKey.isBlank()) return Optional.empty();

        String prompt = HARD_CODED_MEAL_PROMPT;

        String model = HARD_CODED_GOOGLE_MODEL.trim();
        if (model.isBlank()) model = "gemini-2.5-flash";

        String mt = (mimeType == null || mimeType.isBlank()) ? "image/jpeg" : mimeType;

        try {
            Client client = Client.builder().apiKey(apiKey.trim()).build();
            Content content = Content.fromParts(
                    Part.fromText(prompt),
                    Part.fromBytes(imageBytes, mt)
            );

            GenerateContentResponse response = client.models.generateContent(model, content, null);
            String text = response != null ? response.text() : null;
            if (text == null || text.isBlank()) {
                try {
                    String finishReason = response != null ? String.valueOf(response.finishReason()) : "null";
                    Object promptFeedback = response != null ? response.promptFeedback().orElse(null) : null;
                    String raw = response != null ? response.toJson() : null;
                    if (raw != null && raw.length() > 4000) raw = raw.substring(0, 4000) + "...(truncated)";
                    log.warn("FoodAiService(GeminiSDK) empty text. model={} finishReason={} promptFeedback={} raw={}",
                            model, finishReason, promptFeedback, raw);
                } catch (Exception ignored) {}
                return Optional.empty();
            }

            return Optional.of(FoodAiResult.builder()
                    .foodItemsJson(text.trim())
                    .note(null)
                    .build());
        } catch (Exception e) {
            log.warn("FoodAiService(GeminiSDK) analyze failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

}

