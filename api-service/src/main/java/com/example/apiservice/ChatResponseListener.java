package com.example.apiservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ChatResponseListener {

    private static final Logger logger = LoggerFactory.getLogger(ChatResponseListener.class);
    private final ChatController chatController;
    private final ObjectMapper objectMapper;

    public ChatResponseListener(ChatController chatController, ObjectMapper objectMapper) {
        this.chatController = chatController;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "chat-responses", groupId = "api-service")
    public void handleChatResponse(ConsumerRecord<String, String> record) {
        try {
            Map<?, ?> payload = objectMapper.readValue(record.value(), Map.class);
            String requestId = (String) payload.get("requestId");
            String answer = (String) payload.get("answer");

            chatController.storeResponse(requestId, answer);
            logger.info("Stored answer for requestId: {}", requestId);
        } catch (Exception e) {
            logger.error("Failed to process chat response: {}", record.value(), e);
        }
    }
}
