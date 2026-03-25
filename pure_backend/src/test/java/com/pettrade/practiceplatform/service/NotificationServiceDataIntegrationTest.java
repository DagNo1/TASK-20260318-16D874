package com.pettrade.practiceplatform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pettrade.practiceplatform.domain.User;
import com.pettrade.practiceplatform.domain.enumtype.NotificationEventType;
import com.pettrade.practiceplatform.repository.NotificationDeliveryRepository;
import com.pettrade.practiceplatform.repository.NotificationEventRepository;
import com.pettrade.practiceplatform.repository.NotificationSubscriptionRepository;
import com.pettrade.practiceplatform.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@DataJpaTest
@ActiveProfiles("test")
class NotificationServiceDataIntegrationTest {

    @Autowired
    private NotificationEventRepository eventRepository;
    @Autowired
    private NotificationSubscriptionRepository subscriptionRepository;
    @Autowired
    private NotificationDeliveryRepository deliveryRepository;
    @Autowired
    private UserRepository userRepository;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-03-25T12:00:00Z"), ZoneOffset.UTC);
        service = new NotificationService(
                eventRepository,
                subscriptionRepository,
                deliveryRepository,
                new ObjectMapper(),
                clock
        );
    }

    @Test
    void publishCreatesDeliveryTimestampAndReadInitiallyNull() {
        User actor = createUser("actor");
        User recipient = createUser("recipient");

        service.upsertSubscription(recipient, NotificationEventType.PRACTICE_STEP_COMPLETED, true);
        service.publish(NotificationEventType.PRACTICE_STEP_COMPLETED, Map.of("stepId", 33), actor);

        var deliveries = service.listForUser(recipient, false);
        assertNotNull(deliveries.get(0).deliveredAt());
        assertNull(deliveries.get(0).readAt());
    }

    @Test
    void markReadSetsReadTimestamp() {
        User actor = createUser("actor2");
        User recipient = createUser("recipient2");

        service.upsertSubscription(recipient, NotificationEventType.IM_MESSAGE_RECALLED, true);
        service.publish(NotificationEventType.IM_MESSAGE_RECALLED, Map.of("messageId", 77), actor);

        var deliveries = service.listForUser(recipient, true);
        var updated = service.markRead(deliveries.get(0).deliveryId(), recipient);

        assertNotNull(updated.readAt());
    }

    private User createUser(String username) {
        User u = new User();
        u.setUsername(username);
        u.setPasswordHash("$2a$10$7EqJtq98hPqEX7fNZaFWoOHiR0Jw4u7v6/9erjRzCQXDpUe1koX6.");
        u.setEnabled(true);
        return userRepository.save(u);
    }
}
