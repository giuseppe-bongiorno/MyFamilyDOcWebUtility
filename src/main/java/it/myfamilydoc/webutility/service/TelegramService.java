package it.myfamilydoc.webutility.service;

import it.myfamilydoc.webutility.dto.TelegramDto;
import it.myfamilydoc.webutility.entity.TelegramMessage;
import it.myfamilydoc.webutility.entity.TelegramMessage.TelegramMessageStatus;
import it.myfamilydoc.webutility.repository.TelegramMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for sending messages via Telegram Bot API
 */
@Service
public class TelegramService {

    private static final Logger log = LoggerFactory.getLogger(TelegramService.class);
    private static final String TELEGRAM_API_URL = "https://api.telegram.org/bot%s/sendMessage";

    private final TelegramMessageRepository telegramMessageRepository;
    private final RestTemplate restTemplate;

    public TelegramService(TelegramMessageRepository telegramMessageRepository) {
        this.telegramMessageRepository = telegramMessageRepository;
        this.restTemplate = new RestTemplate();
    }

    @Transactional
    public TelegramDto.TelegramMessageResponse sendMessage(
            TelegramDto.SendMessageRequest request, 
            Long userId
    ) {
        log.info("Sending Telegram message to chatId: {}", request.getChatId());

        TelegramMessage message = new TelegramMessage();
        message.setBotToken(request.getBotToken());
        message.setChatId(request.getChatId());
        message.setMessage(request.getMessage());
        message.setStatus(TelegramMessageStatus.PENDING);
        message.setCreatedBy(userId);

        message = telegramMessageRepository.save(message);

        try {
            String apiUrl = String.format(TELEGRAM_API_URL, request.getBotToken());
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("chat_id", request.getChatId());
            requestBody.put("text", request.getMessage());
            requestBody.put("parse_mode", "HTML");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<TelegramDto.TelegramApiResponse> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                entity,
                TelegramDto.TelegramApiResponse.class
            );

            TelegramDto.TelegramApiResponse apiResponse = response.getBody();

            if (apiResponse != null && Boolean.TRUE.equals(apiResponse.getOk())) {
                message.setStatus(TelegramMessageStatus.SENT);
                message.setTelegramMessageId(apiResponse.getResult().getMessageId());
                message.setSentAt(LocalDateTime.now());
                
                log.info("Telegram message sent successfully. Message ID: {}", 
                         apiResponse.getResult().getMessageId());
            } else {
                message.setStatus(TelegramMessageStatus.FAILED);
                String errorMsg = apiResponse != null ? apiResponse.getDescription() : "Unknown error";
                message.setErrorMessage(errorMsg);
                
                log.error("Failed to send Telegram message: {}", errorMsg);
            }

        } catch (Exception e) {
            message.setStatus(TelegramMessageStatus.FAILED);
            message.setErrorMessage(e.getMessage());
            
            log.error("Exception while sending Telegram message", e);
        }

        message = telegramMessageRepository.save(message);

        return mapToResponse(message);
    }

    @Transactional(readOnly = true)
    public List<TelegramDto.TelegramMessageResponse> getAllMessages() {
        log.info("Fetching all Telegram messages");
        
        return telegramMessageRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TelegramDto.TelegramMessageResponse getMessageById(Long id) {
        log.info("Fetching Telegram message by ID: {}", id);
        
        return telegramMessageRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new RuntimeException("Message not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<TelegramDto.TelegramMessageResponse> getMessagesByStatus(TelegramMessageStatus status) {
        log.info("Fetching Telegram messages by status: {}", status);
        
        return telegramMessageRepository.findByStatusOrderByCreatedAtDesc(status)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TelegramDto.TelegramStats getStatistics() {
        log.info("Fetching Telegram message statistics");
        
        long total = telegramMessageRepository.countTotal();
        long sent = telegramMessageRepository.countByStatus(TelegramMessageStatus.SENT);
        long failed = telegramMessageRepository.countByStatus(TelegramMessageStatus.FAILED);
        long pending = telegramMessageRepository.countByStatus(TelegramMessageStatus.PENDING);

        return new TelegramDto.TelegramStats(total, sent, failed, pending);
    }

    @Transactional
    public TelegramDto.TelegramMessageResponse retryMessage(Long messageId, Long userId) {
        log.info("Retrying Telegram message with ID: {}", messageId);
        
        TelegramMessage message = telegramMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found with id: " + messageId));

        TelegramDto.SendMessageRequest request = new TelegramDto.SendMessageRequest();
        request.setBotToken(message.getBotToken());
        request.setChatId(message.getChatId());
        request.setMessage(message.getMessage());

        return sendMessage(request, userId);
    }

    @Transactional
    public void deleteMessage(Long messageId) {
        log.info("Deleting Telegram message with ID: {}", messageId);
        
        if (!telegramMessageRepository.existsById(messageId)) {
            throw new RuntimeException("Message not found with id: " + messageId);
        }
        
        telegramMessageRepository.deleteById(messageId);
    }

    private TelegramDto.TelegramMessageResponse mapToResponse(TelegramMessage message) {
        TelegramDto.TelegramMessageResponse response = new TelegramDto.TelegramMessageResponse();
        response.setId(message.getId());
        response.setBotToken(maskToken(message.getBotToken()));
        response.setChatId(message.getChatId());
        response.setMessage(message.getMessage());
        response.setStatus(message.getStatus());
        response.setTelegramMessageId(message.getTelegramMessageId());
        response.setSentAt(message.getSentAt());
        response.setErrorMessage(message.getErrorMessage());
        response.setCreatedBy(message.getCreatedBy());
        response.setCreatedAt(message.getCreatedAt());
        response.setUpdatedAt(message.getUpdatedAt());
        return response;
    }

    private String maskToken(String token) {
        if (token == null || token.length() <= 4) {
            return "****";
        }
        return "****" + token.substring(token.length() - 4);
    }
}