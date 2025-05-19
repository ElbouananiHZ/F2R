package Find.read.Read.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private static final Logger logger = LoggerFactory.getLogger(AiController.class);
    private static final String[] OFF_TOPICS = {"code", "java", "python", "math", "algorithm", "programming"};
    private static final String AI_MODEL = "phi";
    private static final String AI_BASE_URL = "http://localhost:11434";

    private final WebClient webClient;
    private final ObjectMapper mapper;

    public AiController(ObjectMapper mapper) {
        this.mapper = mapper;
        this.webClient = WebClient.builder()
                .baseUrl(AI_BASE_URL)
                .build();
    }

    @PostMapping("/generate")
    public Mono<ResponseEntity<Map<String, Object>>> generate(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {

        String userInput = Optional.ofNullable(body.get("prompt")).orElse("").trim();

        if (userInput.isEmpty()) {
            return createErrorResponse("Prompt is required", HttpStatus.BAD_REQUEST);
        }

        if (containsOffTopic(userInput)) {
            return createSuccessResponse("Sorry, I specialize in books and reading-related topics.");
        }

        if (isUserRequestingRecommendation(userInput)) {
            return redirectToRecommendationPage();
        }

        if (isUserBored(userInput)) {
            Map<String, Object> response = new HashMap<>();
            response.put("response", "Je vois que tu t'ennuies. Clique sur le bouton ci-dessous pour recevoir des recommandations personnalisées.");
            response.put("redirectTo", "/recommendation");
            return Mono.just(ResponseEntity.ok(response));
        }

        return processGeneralChat(userInput);
    }

    private boolean isUserBored(String input) {
        String lower = input.toLowerCase();
        return lower.contains("bored") || lower.contains("ennuie") || lower.contains("je m'ennuie");
    }

    private boolean isUserRequestingRecommendation(String input) {
        String lower = input.toLowerCase();
        return lower.contains("recommend") || lower.contains("recommendation") ||
                lower.contains("i want to read") || lower.contains("suggest") || lower.contains("suggestion");
    }

    private Mono<ResponseEntity<Map<String, Object>>> redirectToRecommendationPage() {
        Map<String, Object> response = new HashMap<>();
        response.put("response", "Accède à la page de recommandations pour découvrir des livres qui te plairont.");
        response.put("redirectTo", "/recommendation");
        return Mono.just(ResponseEntity.ok(response));
    }

    private boolean containsOffTopic(String input) {
        String lower = input.toLowerCase();
        return Arrays.stream(OFF_TOPICS).anyMatch(lower::contains);
    }

    private Mono<ResponseEntity<Map<String, Object>>> processGeneralChat(String userInput) {
        String prompt = buildPrompt(userInput);

        return callPhiAI(prompt)
                .flatMap(json -> {
                    JsonNode responseNode = json.get("response");
                    String reply = (responseNode != null) ? responseNode.asText() : "Je n'ai pas bien compris, peux-tu reformuler ?";
                    return createSuccessResponse(reply);
                })
                .onErrorResume(this::handleAiError);
    }

    private String buildPrompt(String userInput) {
        return "You are a friendly book expert assistant. Focus on literature, authors, and reading.\n" +
                "User: " + userInput + "\n" +
                "Assistant:";
    }

    private Mono<JsonNode> callPhiAI(String prompt) {
        Map<String, Object> request = Map.of(
                "model", AI_MODEL,
                "prompt", prompt,
                "stream", false
        );

        return webClient.post()
                .uri("/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> Mono.error(new RuntimeException("AI error: " + errorBody)))
                )
                .bodyToMono(String.class)
                .flatMap(json -> {
                    try {
                        return Mono.just(mapper.readTree(json));
                    } catch (Exception e) {
                        return Mono.error(e);
                    }
                });
    }

    private Mono<ResponseEntity<Map<String, Object>>> createSuccessResponse(String message) {
        return Mono.just(ResponseEntity.ok(Map.of("response", message)));
    }

    private Mono<ResponseEntity<Map<String, Object>>> createErrorResponse(String message, HttpStatus status) {
        return Mono.just(ResponseEntity.status(status).body(Map.of("error", message)));
    }

    private Mono<ResponseEntity<Map<String, Object>>> handleAiError(Throwable error) {
        logger.error("AI processing error", error);
        return createErrorResponse("AI service is temporarily unavailable", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
