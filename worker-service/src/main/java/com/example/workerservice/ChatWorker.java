package com.example.workerservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ChatWorker {

    private static final Logger logger = LoggerFactory.getLogger(ChatWorker.class);
    private final OpenAiChatModel chatModel;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public ChatWorker(OpenAiChatModel chatModel, KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "user-questions", groupId = "worker-service")
    public void handleUserQuestion(ConsumerRecord<String, String> record) {
        logger.info("Received message: {}", record.value());

        try {
            Map<?, ?> payload = objectMapper.readValue(record.value(), Map.class);
            String messageText = (String) payload.get("message");
            String requestId = (String) payload.get("requestId");

            logger.info("Processing request for ID: {}", requestId);

            Prompt prompt = new Prompt(new UserMessage(messageText));
            ChatResponse response = chatModel.call(prompt);
            String aiAnswer = response.getResult().getOutput().getText();

            String responsePayload = objectMapper.writeValueAsString(Map.of(
                    "requestId", requestId,
                    "answer", aiAnswer,
                    "status", "complete"
            ));
            kafkaTemplate.send("chat-responses", requestId, responsePayload);
            logger.info("Published answer for ID: {}", requestId);

        } catch (Exception e) {
            logger.error("Failed to process message: {}", record.value(), e);
            throw new RuntimeException("Failed to generate AI response", e);
        }
    }
}
