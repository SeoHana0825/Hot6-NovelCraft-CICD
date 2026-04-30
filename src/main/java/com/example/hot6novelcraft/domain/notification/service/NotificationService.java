package com.example.hot6novelcraft.domain.notification.service;

import com.example.hot6novelcraft.common.dto.PageResponse;
import com.example.hot6novelcraft.common.exception.ServiceErrorException;
import com.example.hot6novelcraft.common.exception.domain.NotificationExceptionEnum;
import com.example.hot6novelcraft.domain.notification.dto.event.NotificationEvent;
import com.example.hot6novelcraft.domain.notification.dto.response.NotificationResponse;
import com.example.hot6novelcraft.domain.notification.entity.Notification;
import com.example.hot6novelcraft.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @KafkaListener(
            topics = "${notification.kafka.topic}",
            containerFactory = "notificationKafkaListenerContainerFactory"
    )
    public void consume(NotificationEvent event) {
        log.debug("[Kafka] 알림 수신 userId={} type={}", event.userId(), event.type());

        // DB 저장: save()는 자체 트랜잭션을 가지므로 즉시 커밋됨
        Notification notification = Notification.create(
                event.userId(),
                event.type(),
                event.title(),
                event.message(),
                event.referenceId(),
                event.referenceType()
        );
        notificationRepository.save(notification);

        // WebSocket 전송: DB 커밋 후 별도로 수행, 실패해도 Kafka 재소비 방지
        try {
            NotificationResponse response = NotificationResponse.from(notification);
            messagingTemplate.convertAndSend("/topic/notifications/" + event.userId(), response);
            log.info("[알림] 저장 및 WebSocket 전송 완료 userId={} type={}", event.userId(), event.type());
        } catch (Exception e) {
            log.warn("[알림] WebSocket 전송 실패 (DB 저장은 완료) userId={} type={}", event.userId(), event.type(), e);
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getNotifications(Long userId, Pageable pageable) {
        Page<NotificationResponse> page = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(NotificationResponse::from);
        return PageResponse.register(page);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public NotificationResponse markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ServiceErrorException(NotificationExceptionEnum.ERR_NOTIFICATION_NOT_FOUND));

        if (!notification.getUserId().equals(userId)) {
            throw new ServiceErrorException(NotificationExceptionEnum.ERR_NOT_MY_NOTIFICATION);
        }

        notification.markAsRead();
        return NotificationResponse.from(notification);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }
}
