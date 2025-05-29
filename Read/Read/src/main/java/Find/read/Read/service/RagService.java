package Find.read.Read.service;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class RagService {
    private final RestTemplate restTemplate;
    private final String ragApiUrl = "http://localhost:8000/api";

    public RagService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String queryRag(String question) {
        try {
            // Create request body
            Map<String, String> request = new HashMap<>();
            request.put("text", question);

            // Make POST request to RAG API
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    ragApiUrl + "/query",
                    request,
                    Map.class
            );

            // Check if response is successful
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return (String) response.getBody().get("response");
            }
            throw new RuntimeException("Failed to get response from RAG API");
        } catch (Exception e) {
            throw new RuntimeException("Error querying RAG API: " + e.getMessage());
        }
    }
}