package com.example.ChatIntegrationZoya;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Flux;


import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@Component //For dependency injection

public class ChatController {
    private final Map<String, String> responseStore = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    // polling endpoint for frontend
    // called by KafkaProcessor when the answer is ready
    public void storeResponse(String requestId, String answer) {
        responseStore.put(requestId, answer);
    }
    private final KafkaTemplate<String, String> kafkaTemplate;

    public ChatController(KafkaTemplate<String,String> kafkaTemplate, ObjectMapper objectMapper){
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper= objectMapper;
    }


    @GetMapping("/ai/generate")
    public Map<String,Object> generate(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
        String requestId = UUID.randomUUID().toString();
        //jackson-safe serialisation attempt
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "message", message,
                    "requestId", requestId
            ));
            //No need to cast to producer record anymore because KafkaTemplate can handle it
            kafkaTemplate.send("user-questions", requestId, payload);
            return Map.of("status", "processing", "requestId", requestId, "message", "We are working on it...");
        }catch (JsonProcessingException e) {
            return Map.of("status", "error", "message", "Failed to serialize message: " + e.getMessage());
        }
    }

    @GetMapping("/ai/response/{requestId}")
    public Map<String, Object> getResponse(@PathVariable String requestId) {
        String answer = responseStore.get(requestId);
        if (answer != null) {
            return Map.of("status", "complete", "answer", answer);
        }
        return Map.of("status", "processing");
    }
}
