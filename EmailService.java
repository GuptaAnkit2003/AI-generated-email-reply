package com.email.writer;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class EmailService {

   private final WebClient webClient;
   private final String apikey;

    public EmailService(WebClient.Builder webClientBuilder
                        ,@Value("${gemini.api.url}")String baseUrl,
                        @Value("${gemini.api.key}") String geminiApikey) {
        this.apikey = geminiApikey;
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();

    }

    public String generateEmailReply(EmailRequest emailRequest) {

        // Build prompt
        String prompt = buildprompt(emailRequest);
        // Prepare raw JSON body
        String requestBody = String.format("""
                {
                    "contents": [
                      {
                        "parts": [
                          {
                            "text": "%s"
                          }
                        ]
                      }
                    ]
                  }""" , prompt);
        //send request
        String response = webClient.post().uri(uriBuilder -> uriBuilder
                        .path("/v1beta/models/gemini-2.5-flash:generateContent").build())
                .header("x-goog-api-key", apikey)
                .header("Content-Type" , "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        // extract response
        return extractResponseContent(response);
        }


    private String extractResponseContent(String response) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);

            return root.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();

        } catch (Exception e) {
            e.printStackTrace();
            return "Error parsing response";
        }
    }

private String buildprompt(EmailRequest emailRequest) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a professional reply for this email :");
        if (emailRequest.getTone() != null && !emailRequest.getTone().isEmpty()) {
            prompt.append("use a").append(emailRequest.getTone()).append("tone.");
        }
        prompt.append("Original Email: \n").append(emailRequest.getEmailContent());
        return prompt.toString();
    }
}

