package com.eldercare.service;

import com.eldercare.dto.ChatConversationDto;
import com.eldercare.dto.ChatMessageDto;
import com.eldercare.model.Conversation;
import com.eldercare.model.ElderlyCaregiver;
import com.eldercare.model.Message;
import com.eldercare.model.User;
import com.eldercare.model.enums.UserRole;
import com.eldercare.repository.ConversationRepository;
import com.eldercare.repository.ElderlyCaregiverRepository;
import com.eldercare.repository.MessageRepository;
import com.eldercare.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final UserRepository userRepository;
    private final ElderlyCaregiverRepository elderlyCaregiverRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final FoodAiService foodAiService;

    public List<ChatConversationDto> listConversations(Long currentUserId) {
        User me = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        List<Conversation> conversations = new ArrayList<>();

        if (me.getRole() == UserRole.ELDERLY) {
            List<ElderlyCaregiver> links = elderlyCaregiverRepository.findByElderly(me);
            for (ElderlyCaregiver link : links) {
                User caregiver = link.getCaregiver();
                Conversation c = getOrCreateConversation(me, caregiver);
                conversations.add(c);
            }
        } else if (me.getRole() == UserRole.CAREGIVER) {
            List<ElderlyCaregiver> links = elderlyCaregiverRepository.findByCaregiver(me);
            for (ElderlyCaregiver link : links) {
                User elderly = link.getElderly();
                Conversation c = getOrCreateConversation(elderly, me);
                conversations.add(c);
            }
        } else {
            return List.of();
        }

        return conversations.stream().map(c -> {
            User elderly = c.getElderly();
            User caregiver = c.getCaregiver();
            Message last = getLastMessage(c.getId());
            return ChatConversationDto.builder()
                    .id(c.getId())
                    .elderlyId(elderly.getId())
                    .elderlyName(elderly.getFullName())
                    .caregiverId(caregiver.getId())
                    .caregiverName(caregiver.getFullName())
                    .lastMessageText(last != null ? (last.getText() != null ? last.getText() : (last.getImageUrl() != null ? "[Ảnh]" : "")) : null)
                    .lastMessageAt(last != null && last.getCreatedAt() != null ? ISO.format(last.getCreatedAt()) : null)
                    .build();
        }).toList();
    }

    public List<ChatMessageDto> getMessages(Long conversationId, Long currentUserId, int limit) {
        Conversation c = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hội thoại"));
        requireParticipant(c, currentUserId);

        int pageSize = Math.min(Math.max(limit, 1), 50);
        List<Message> messages = messageRepository.findByConversationOrderByCreatedAtDesc(c, PageRequest.of(0, pageSize));
        // trả về theo thời gian tăng dần cho UI
        List<Message> asc = new ArrayList<>(messages);
        asc.sort((a, b) -> {
            if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
            if (a.getCreatedAt() == null) return -1;
            if (b.getCreatedAt() == null) return 1;
            return a.getCreatedAt().compareTo(b.getCreatedAt());
        });
        return asc.stream().map(this::toDto).toList();
    }

    @Transactional
    public ChatMessageDto sendText(Long conversationId, Long senderId, String text) {
        Conversation c = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hội thoại"));
        requireParticipant(c, senderId);

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người gửi"));

        Message msg = Message.builder()
                .conversation(c)
                .sender(sender)
                .text(text == null ? "" : text.trim())
                .build();
        msg = messageRepository.save(msg);

        ChatMessageDto dto = toDto(msg);
        messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, dto);
        return dto;
    }

    @Transactional
    public ChatMessageDto sendImage(Long conversationId, Long senderId, MultipartFile image, String text) {
        Conversation c = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hội thoại"));
        requireParticipant(c, senderId);

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người gửi"));

        String imageUrl = saveUpload(image);
        Message msg = Message.builder()
                .conversation(c)
                .sender(sender)
                .text(text == null ? null : text.trim())
                .imageUrl(imageUrl)
                .aiNote("Đang phân tích bữa ăn...")
                .build();
        msg = messageRepository.save(msg);

        ChatMessageDto dto = toDto(msg);
        messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, dto);

        Long messageId = msg.getId();
        String mime = image.getContentType();
        byte[] bytes;
        try {
            bytes = image.getBytes();
        } catch (IOException e) {
            bytes = null;
        }
        final byte[] bytesFinal = bytes;
        if (bytesFinal != null) {
            CompletableFuture.runAsync(() -> runAiAnalysis(conversationId, messageId, bytesFinal, mime));
        }
        return dto;
    }

    private void runAiAnalysis(Long conversationId, Long messageId, byte[] bytes, String mime) {
        try {
            var resultOpt = foodAiService.analyzeMealImage(bytes, mime);
            Message msg = messageRepository.findById(messageId).orElse(null);
            if (msg == null) return;
            if (resultOpt.isEmpty()) {
                // hiển thị rõ lỗi cấu hình / gọi AI cho client thay vì im lặng
                msg.setAiNote("AI chưa cấu hình hoặc không gọi được. Kiểm tra `ai_provider`, `ai_google_api_key`, `ai_google_model` (hoặc OpenAI key/model), quyền mạng và quota.");
                messageRepository.save(msg);
                messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, toDto(msg));
                return;
            }
            var r = resultOpt.get();
            msg.setAiFoodItemsJson(r.getFoodItemsJson());
            msg.setAiNote(r.getNote());
            messageRepository.save(msg);
            messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, toDto(msg));
        } catch (Exception e) {
            log.warn("AI analysis failed: {}", e.getMessage());
        }
    }

    private String saveUpload(MultipartFile file) {
        try {
            Files.createDirectories(Paths.get("uploads"));
            String ext = "";
            String original = file.getOriginalFilename();
            if (original != null && original.contains(".")) {
                ext = original.substring(original.lastIndexOf('.'));
                if (ext.length() > 10) ext = "";
            }
            String name = UUID.randomUUID() + ext;
            Path target = Paths.get("uploads").resolve(name);
            Files.write(target, file.getBytes());
            // Return relative URL so mobile clients can prefix correct host (10.0.2.2 vs LAN IP, etc.)
            return "/uploads/" + name;
        } catch (Exception e) {
            throw new RuntimeException("Không lưu được ảnh");
        }
    }

    public Conversation getOrCreateConversation(User elderly, User caregiver) {
        Optional<Conversation> existing = conversationRepository.findByElderlyAndCaregiver(elderly, caregiver);
        if (existing.isPresent()) return existing.get();
        return conversationRepository.save(Conversation.builder()
                .elderly(elderly)
                .caregiver(caregiver)
                .build());
    }

    private void requireParticipant(Conversation c, Long userId) {
        if (userId == null) throw new RuntimeException("Chưa đăng nhập");
        if (c.getElderly() == null || c.getCaregiver() == null) {
            throw new RuntimeException("Hội thoại không hợp lệ");
        }
        if (!userId.equals(c.getElderly().getId()) && !userId.equals(c.getCaregiver().getId())) {
            throw new RuntimeException("Bạn không có quyền truy cập hội thoại này");
        }
    }

    private Message getLastMessage(Long conversationId) {
        Conversation c = conversationRepository.findById(conversationId).orElse(null);
        if (c == null) return null;
        List<Message> list = messageRepository.findByConversationOrderByCreatedAtDesc(c, PageRequest.of(0, 1));
        return list.isEmpty() ? null : list.get(0);
    }

    public ChatMessageDto toDto(Message m) {
        return ChatMessageDto.builder()
                .id(m.getId())
                .conversationId(m.getConversation() != null ? m.getConversation().getId() : null)
                .senderId(m.getSender() != null ? m.getSender().getId() : null)
                .senderName(m.getSender() != null ? m.getSender().getFullName() : null)
                .text(m.getText())
                .imageUrl(m.getImageUrl())
                .aiNote(m.getAiNote())
                .aiFoodItemsJson(m.getAiFoodItemsJson())
                .createdAt(m.getCreatedAt() != null ? ISO.format(m.getCreatedAt()) : null)
                .build();
    }
}

