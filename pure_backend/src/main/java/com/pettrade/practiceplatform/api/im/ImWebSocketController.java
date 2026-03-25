package com.pettrade.practiceplatform.api.im;

import com.pettrade.practiceplatform.domain.User;
import com.pettrade.practiceplatform.service.ImMessagingService;
import com.pettrade.practiceplatform.service.im.ImSecurityContextUserService;
import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class ImWebSocketController {

    private final ImMessagingService messagingService;
    private final ImSecurityContextUserService userService;
    private final SimpMessagingTemplate messagingTemplate;

    public ImWebSocketController(
            ImMessagingService messagingService,
            ImSecurityContextUserService userService,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.messagingService = messagingService;
        this.userService = userService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/im/{sessionId}/send")
    public void send(
            @DestinationVariable Long sessionId,
            @Valid @Payload SendMessageRequest request,
            Principal principal
    ) {
        User actor = userService.fromPrincipal(principal);
        ImMessageEvent event = messagingService.sendMessage(sessionId, request, actor);
        messagingTemplate.convertAndSend("/topic/im/" + sessionId + "/messages", event);
    }

    @MessageMapping("/im/{sessionId}/establish")
    public void establish(
            @DestinationVariable Long sessionId,
            Principal principal
    ) {
        User actor = userService.fromPrincipal(principal);
        ImSessionEstablishedEvent event = messagingService.establishSession(sessionId, actor);
        messagingTemplate.convertAndSendToUser(actor.getUsername(), "/queue/im/session", event);
    }

    @MessageMapping("/im/{sessionId}/recall")
    public void recall(
            @DestinationVariable Long sessionId,
            @Valid @Payload RecallMessageRequest request,
            Principal principal
    ) {
        User actor = userService.fromPrincipal(principal);
        ImMessageEvent event = messagingService.recallMessage(sessionId, request, actor);
        messagingTemplate.convertAndSend("/topic/im/" + sessionId + "/messages", event);
    }

    @MessageMapping("/im/{sessionId}/read")
    public void read(
            @DestinationVariable Long sessionId,
            @Valid @Payload ReadReceiptRequest request,
            Principal principal
    ) {
        User actor = userService.fromPrincipal(principal);
        ImReadEvent event = messagingService.markRead(sessionId, request, actor);
        messagingTemplate.convertAndSend("/topic/im/" + sessionId + "/reads", event);
    }
}
