package com.finance.finance.service;

import com.finance.finance.exception.AIServiceException;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.IOException;

@Service
public class AIService {

    @Value("${gemini.project.id}")
    private String projectId;

    @Value("${gemini.location}")
    private String location;

    @Value("${gemini.model.name}")
    private String modelName;
    private static final Logger logger = LoggerFactory.getLogger(AIService.class);

    private GenerativeModel model;
    private VertexAI vertexAI; // VertexAI nesnesini bir alan olarak tutun

    @PostConstruct
    public void init() {
        logger.info("Initializing GenerativeModel for Vertex AI...");
        try {
            this.vertexAI = new VertexAI(projectId, location); // Kapatma, bean yaşadığı sürece yaşasın
            this.model = new GenerativeModel.Builder()
                    .setModelName(modelName)
                    .setVertexAi(vertexAI)
                    .build();
            logger.info("GenerativeModel initialized successfully.");
        } catch (Exception e) {
            logger.error("Error initializing Vertex AI", e);
            throw new AIServiceException("Vertex AI başlatılamadı", e);
        }
    }

    public String getAIResponse(String prompt) {
        try {
            GenerateContentResponse response = model.generateContent(prompt);
            return ResponseHandler.getText(response);
        } catch (Exception e) {
            logger.error("Error getting AI response for prompt: '{}'", prompt, e);
            throw new AIServiceException("Yapay zeka modelinden yanıt alınamadı.", e);
        }
    }

    // Bean destroy olduğunda vertexAI'yı kapatmak için
    @PreDestroy
    public void shutdown() {
        if (vertexAI != null) {
            try {
                vertexAI.close();
            } catch (Exception e) {
                logger.warn("Error closing VertexAI", e);
            }
        }
    }
}