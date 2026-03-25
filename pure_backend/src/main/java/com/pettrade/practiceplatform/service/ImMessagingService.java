package com.pettrade.practiceplatform.service;

import com.pettrade.practiceplatform.api.im.ImMessageEvent;
import com.pettrade.practiceplatform.api.im.ImReadEvent;
import com.pettrade.practiceplatform.api.im.ImSessionEstablishedEvent;
import com.pettrade.practiceplatform.api.im.RecallMessageRequest;
import com.pettrade.practiceplatform.api.im.ReadReceiptRequest;
import com.pettrade.practiceplatform.api.im.SendMessageRequest;
import com.pettrade.practiceplatform.domain.ImImageAsset;
import com.pettrade.practiceplatform.domain.ImMessage;
import com.pettrade.practiceplatform.domain.ImSession;
import com.pettrade.practiceplatform.domain.ImSessionMember;
import com.pettrade.practiceplatform.domain.User;
import com.pettrade.practiceplatform.domain.enumtype.ImMessageStatus;
import com.pettrade.practiceplatform.domain.enumtype.ImMessageType;
import com.pettrade.practiceplatform.domain.enumtype.NotificationEventType;
import com.pettrade.practiceplatform.exception.BusinessRuleException;
import com.pettrade.practiceplatform.exception.NotFoundException;
import com.pettrade.practiceplatform.repository.ImImageAssetRepository;
import com.pettrade.practiceplatform.repository.ImMessageRepository;
import com.pettrade.practiceplatform.repository.ImSessionMemberRepository;
import com.pettrade.practiceplatform.repository.ImSessionRepository;
import com.pettrade.practiceplatform.service.im.ImMessageTypeResolver;
import com.pettrade.practiceplatform.service.im.ImSessionGuardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
public class ImMessagingService {

    private static final long DUP_WINDOW_SECONDS = 10L;
    private static final long MAX_IMAGE_BYTES = 2L * 1024L * 1024L;

    private final ImSessionRepository sessionRepository;
    private final ImSessionMemberRepository sessionMemberRepository;
    private final ImMessageRepository messageRepository;
    private final ImImageAssetRepository imageAssetRepository;
    private final ImSessionGuardService sessionGuardService;
    private final NotificationService notificationService;
    private final Clock clock;

    public ImMessagingService(
            ImSessionRepository sessionRepository,
            ImSessionMemberRepository sessionMemberRepository,
            ImMessageRepository messageRepository,
            ImImageAssetRepository imageAssetRepository,
            ImSessionGuardService sessionGuardService,
            NotificationService notificationService,
            Clock clock
    ) {
        this.sessionRepository = sessionRepository;
        this.sessionMemberRepository = sessionMemberRepository;
        this.messageRepository = messageRepository;
        this.imageAssetRepository = imageAssetRepository;
        this.sessionGuardService = sessionGuardService;
        this.notificationService = notificationService;
        this.clock = clock;
    }

    @Transactional
    public ImMessageEvent sendMessage(Long sessionId, SendMessageRequest request, User actor) {
        ImSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("IM session not found"));
        sessionGuardService.ensureMember(sessionId, actor);

        ImMessageType type = ImMessageTypeResolver.parse(request.type());
        LocalDateTime now = LocalDateTime.now(clock);
        String dedupKey;
        String text = null;
        ImImageAsset imageAsset = null;

        if (type == ImMessageType.TEXT) {
            text = normalizeText(request.text());
            if (text.isBlank()) {
                throw new BusinessRuleException("Text message cannot be empty");
            }
            dedupKey = hash("TEXT:" + text);
        } else {
            validateImage(request.imageMimeType(), request.imageSizeBytes(), request.imageFingerprint(), request.imageUrl());
            dedupKey = hash("IMAGE:" + request.imageFingerprint());
            imageAsset = imageAssetRepository.findByFingerprint(request.imageFingerprint())
                    .orElseGet(() -> {
                        ImImageAsset asset = new ImImageAsset();
                        asset.setFingerprint(request.imageFingerprint());
                        asset.setMimeType(request.imageMimeType().toLowerCase());
                        asset.setSizeBytes(request.imageSizeBytes());
                        asset.setImageUrl(request.imageUrl());
                        asset.setCreatedBy(actor);
                        return imageAssetRepository.save(asset);
                    });
        }

        ImMessage folded = messageRepository
                .findTopBySessionIdAndSenderIdAndDedupKeyAndMessageTypeOrderByCreatedAtDesc(
                        sessionId,
                        actor.getId(),
                        dedupKey,
                        type
                )
                .filter(msg -> Duration.between(msg.getCreatedAt(), now).getSeconds() <= DUP_WINDOW_SECONDS)
                .orElse(null);

        ImMessage message;
        String eventType;
        if (folded != null && folded.getStatus() == ImMessageStatus.SENT) {
            folded.setFoldedCount(folded.getFoldedCount() + 1);
            folded.setLastFoldedAt(now);
            message = messageRepository.save(folded);
            eventType = "IM_MESSAGE_FOLDED";
        } else {
            ImMessage m = new ImMessage();
            m.setSession(session);
            m.setSender(actor);
            m.setMessageType(type);
            m.setStatus(ImMessageStatus.SENT);
            m.setContent(text);
            m.setImageAsset(imageAsset);
            m.setDedupKey(dedupKey);
            m.setFoldedCount(1);
            m.setLastFoldedAt(now);
            message = messageRepository.save(m);
            eventType = "IM_MESSAGE_SENT";
        }

        return toMessageEvent(eventType, message);
    }

    @Transactional
    public ImMessageEvent recallMessage(Long sessionId, RecallMessageRequest request, User actor) {
        sessionGuardService.ensureMember(sessionId, actor);
        ImMessage message = messageRepository.findByIdAndSessionId(request.messageId(), sessionId)
                .orElseThrow(() -> new NotFoundException("Message not found"));

        boolean sender = message.getSender().getId().equals(actor.getId());
        boolean admin = actor.getRoles().stream().anyMatch(r -> "ROLE_PLATFORM_ADMIN".equals(r.getName()));
        if (!sender && !admin) {
            throw new BusinessRuleException("Not allowed to recall this message");
        }
        if (message.getStatus() == ImMessageStatus.RECALLED) {
            return toMessageEvent("IM_MESSAGE_RECALLED", message);
        }

        message.setStatus(ImMessageStatus.RECALLED);
        ImMessage saved = messageRepository.save(message);
        notificationService.publish(
                NotificationEventType.IM_MESSAGE_RECALLED,
                java.util.Map.of(
                        "sessionId", sessionId,
                        "messageId", saved.getId(),
                        "senderId", saved.getSender().getId()
                ),
                actor
        );
        return toMessageEvent("IM_MESSAGE_RECALLED", saved);
    }

    @Transactional
    public ImReadEvent markRead(Long sessionId, ReadReceiptRequest request, User actor) {
        ImSessionMember member = sessionMemberRepository.findBySessionIdAndUserId(sessionId, actor.getId())
                .orElseThrow(() -> new NotFoundException("IM session not found"));
        ImMessage msg = messageRepository.findByIdAndSessionId(request.lastReadMessageId(), sessionId)
                .orElseThrow(() -> new NotFoundException("Message not found"));

        LocalDateTime now = LocalDateTime.now(clock);
        member.setLastReadMessage(msg);
        member.setLastReadAt(now);
        sessionMemberRepository.save(member);

        return new ImReadEvent("IM_READ_UPDATED", sessionId, actor.getId(), actor.getUsername(), msg.getId(), now);
    }

    @Transactional(readOnly = true)
    public ImSessionEstablishedEvent establishSession(Long sessionId, User actor) {
        ImSessionMember member = sessionMemberRepository.findBySessionIdAndUserId(sessionId, actor.getId())
                .orElseThrow(() -> new NotFoundException("IM session not found"));

        Long lastReadId = member.getLastReadMessage() == null ? null : member.getLastReadMessage().getId();
        long unreadCount = (lastReadId == null)
                ? messageRepository.countBySessionIdAndStatus(sessionId, ImMessageStatus.SENT)
                : messageRepository.countBySessionIdAndIdGreaterThanAndStatus(sessionId, lastReadId, ImMessageStatus.SENT);

        return new ImSessionEstablishedEvent(
                "IM_SESSION_ESTABLISHED",
                sessionId,
                actor.getId(),
                actor.getUsername(),
                lastReadId,
                unreadCount
        );
    }

    private void validateImage(String mimeType, Long sizeBytes, String fingerprint, String imageUrl) {
        if (mimeType == null || sizeBytes == null || fingerprint == null || imageUrl == null) {
            throw new BusinessRuleException("Image fields are required for IMAGE message");
        }
        String normalized = mimeType.toLowerCase();
        if (!"image/jpeg".equals(normalized) && !"image/jpg".equals(normalized) && !"image/png".equals(normalized)) {
            throw new BusinessRuleException("Only JPG or PNG images are allowed");
        }
        if (sizeBytes > MAX_IMAGE_BYTES) {
            throw new BusinessRuleException("Image size must be <= 2MB");
        }
    }

    private String normalizeText(String text) {
        return text == null ? "" : text.trim().replaceAll("\\s+", " ");
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private ImMessageEvent toMessageEvent(String eventType, ImMessage message) {
        return new ImMessageEvent(
                eventType,
                message.getSession().getId(),
                message.getId(),
                message.getSender().getId(),
                message.getSender().getUsername(),
                message.getMessageType(),
                message.getStatus(),
                message.getContent(),
                message.getImageAsset() == null ? null : message.getImageAsset().getImageUrl(),
                message.getFoldedCount(),
                message.getCreatedAt()
        );
    }
}
