package com.pettrade.practiceplatform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pettrade.practiceplatform.api.notification.NotificationDeliveryView;
import com.pettrade.practiceplatform.domain.NotificationDelivery;
import com.pettrade.practiceplatform.domain.NotificationEvent;
import com.pettrade.practiceplatform.domain.NotificationSubscription;
import com.pettrade.practiceplatform.domain.User;
import com.pettrade.practiceplatform.domain.enumtype.NotificationEventType;
import com.pettrade.practiceplatform.repository.NotificationDeliveryRepository;
import com.pettrade.practiceplatform.repository.NotificationEventRepository;
import com.pettrade.practiceplatform.repository.NotificationSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceIntegrationTest {

    @Mock
    private NotificationEventRepository eventRepository;
    @Mock
    private NotificationSubscriptionRepository subscriptionRepository;
    @Mock
    private NotificationDeliveryRepository deliveryRepository;

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
    void publishCreatesDeliveryWithDeliveredTimestampAndReadInitiallyNull() {
        User actor = user(10L, "actor");
        User recipient = user(20L, "receiver");

        NotificationSubscription sub = new NotificationSubscription();
        sub.setUser(recipient);
        sub.setEventType(NotificationEventType.PRACTICE_STEP_COMPLETED);
        sub.setEnabled(true);

        NotificationEvent event = new NotificationEvent();
        ReflectionTestUtils.setField(event, "id", 1L);
        event.setEventType(NotificationEventType.PRACTICE_STEP_COMPLETED);
        event.setPayloadJson("{\"stepId\":99}");
        event.setActorUser(actor);

        when(eventRepository.save(any(NotificationEvent.class))).thenReturn(event);
        when(subscriptionRepository.findByEventTypeAndEnabledTrue(NotificationEventType.PRACTICE_STEP_COMPLETED))
                .thenReturn(List.of(sub));

        service.publish(NotificationEventType.PRACTICE_STEP_COMPLETED, Map.of("stepId", 99), actor);
        verify(deliveryRepository).saveAll(any());

        when(deliveryRepository.findByRecipientIdOrderByDeliveredAtDesc(20L)).thenReturn(List.of(delivery(event, recipient, null)));
        List<NotificationDeliveryView> list = service.listForUser(recipient, false);
        assertEquals(1, list.size());
        assertNotNull(list.get(0).deliveredAt());
        assertNull(list.get(0).readAt());
    }

    @Test
    void markReadSetsReadTimestamp() {
        User recipient = user(20L, "receiver");
        NotificationEvent event = new NotificationEvent();
        ReflectionTestUtils.setField(event, "id", 2L);
        event.setEventType(NotificationEventType.IM_MESSAGE_RECALLED);
        event.setPayloadJson("{}");

        NotificationDelivery delivery = delivery(event, recipient, null);
        ReflectionTestUtils.setField(delivery, "id", 100L);

        when(deliveryRepository.findByIdAndRecipientId(100L, 20L)).thenReturn(Optional.of(delivery));
        when(deliveryRepository.save(any(NotificationDelivery.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationDeliveryView view = service.markRead(100L, recipient);
        assertNotNull(view.readAt());
    }

    private NotificationDelivery delivery(NotificationEvent event, User recipient, LocalDateTime readAt) {
        NotificationDelivery d = new NotificationDelivery();
        d.setEvent(event);
        d.setRecipient(recipient);
        d.setDeliveredAt(LocalDateTime.of(2026, 3, 25, 12, 0, 0));
        d.setReadAt(readAt);
        return d;
    }

    private User user(Long id, String username) {
        User u = new User();
        ReflectionTestUtils.setField(u, "id", id);
        u.setUsername(username);
        return u;
    }
}
