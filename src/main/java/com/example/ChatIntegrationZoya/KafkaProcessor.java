package com.example.ChatIntegrationZoya;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class KafkaProcessor {

    private static final Logger logger = LoggerFactory.getLogger(KafkaProcessor.class);
    private final ChatController chatController;
    private final OpenAiChatModel chatModel;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Constructor injection for all three dependencies
    public KafkaProcessor(ChatController chatController, OpenAiChatModel chatModel, KafkaTemplate<String, String> kafkaTemplate) {
        this.chatController = chatController;
        this.chatModel = chatModel;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "user-questions", groupId = "chat-group")
    public void handleUserQuestion(ConsumerRecord<String, String> record) {
        logger.info("Received message: {}", record.value());

        try {
            // Parse JSON payload safely using Jackson
            Map<?, ?> payload = objectMapper.readValue(record.value(), Map.class);
            String messageText = (String) payload.get("message");
            String requestId = (String) payload.get("requestId");

            logger.info("Processing request for ID: {}", requestId);

            // Calling OpenAI
            Prompt prompt = new Prompt(new UserMessage(messageText));
            ChatResponse response = chatModel.call(prompt);

            // getText() not getContent()
            String aiAnswer = response.getResult().getOutput().getText();

            // response JSON payload
            assert aiAnswer != null;
            String responsePayload = objectMapper.writeValueAsString(Map.of(
                    "requestId", requestId,
                    "answer", aiAnswer,
                    "status", "complete"
            ));
            // Calling storeResponse from chatController
            chatController.storeResponse(requestId, aiAnswer);
            logger.info("Published answer for ID: {}", requestId);

        } catch (Exception e) {
            logger.error("Failed to process message: {}", record.value(), e);
            throw new RuntimeException("Failed to generate AI response", e);
        }
    }
}