package com.pettrade.practiceplatform.api.notification;

import com.pettrade.practiceplatform.domain.User;
import com.pettrade.practiceplatform.domain.enumtype.NotificationEventType;
import com.pettrade.practiceplatform.service.CurrentUserService;
import com.pettrade.practiceplatform.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentUserService currentUserService;

    public NotificationController(NotificationService notificationService, CurrentUserService currentUserService) {
        this.notificationService = notificationService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List notifications")
    public ResponseEntity<List<NotificationDeliveryView>> listMyNotifications(
            @RequestParam(defaultValue = "false") boolean unreadOnly
    ) {
        User user = currentUserService.currentUser();
        return ResponseEntity.ok(notificationService.listForUser(user, unreadOnly));
    }

    @PostMapping("/read")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mark notification as read")
    public ResponseEntity<NotificationDeliveryView> markRead(@Valid @RequestBody NotificationReadRequest request) {
        User user = currentUserService.currentUser();
        return ResponseEntity.ok(notificationService.markRead(request.deliveryId(), user));
    }

    @GetMapping("/subscriptions")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List notification subscriptions")
    public ResponseEntity<List<NotificationSubscriptionView>> listSubscriptions() {
        User user = currentUserService.currentUser();
        return ResponseEntity.ok(notificationService.listAllSubscriptions(user));
    }

    @PostMapping("/subscriptions")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Upsert notification subscription")
    public ResponseEntity<NotificationSubscriptionView> upsertSubscription(
            @Valid @RequestBody NotificationSubscriptionRequest request
    ) {
        User user = currentUserService.currentUser();
        NotificationEventType type = NotificationEventType.valueOf(request.eventType().trim().toUpperCase());
        return ResponseEntity.ok(notificationService.upsertSubscription(user, type, request.enabled()));
    }
}
