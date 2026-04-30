package com.example.hot6novelcraft.domain.notification.dto.event;

import com.example.hot6novelcraft.domain.notification.entity.enums.NotificationType;

public record NotificationEvent(
        Long userId,
        NotificationType type,
        String title,
        String message,
        Long referenceId,
        String referenceType
) {
    public static NotificationEvent episodePurchase(Long userId, String episodeTitle, long price, Long episodeId) {
        return new NotificationEvent(userId, NotificationType.EPISODE_PURCHASE,
                "회차 구매 완료",
                String.format("'%s' 회차를 구매했습니다. (-%dP)", episodeTitle, price),
                episodeId, "EPISODE");
    }

    public static NotificationEvent novelBulkPurchase(Long userId, String novelTitle, int count, long price, Long novelId) {
        return new NotificationEvent(userId, NotificationType.NOVEL_BULK_PURCHASE,
                "소설 전체 구매 완료",
                String.format("'%s' %d개 회차를 구매했습니다. (-%dP)", novelTitle, count, price),
                novelId, "NOVEL");
    }

    public static NotificationEvent authorRevenue(Long authorId, String novelTitle, long revenue, Long revenueId) {
        return new NotificationEvent(authorId, NotificationType.AUTHOR_REVENUE,
                "작품 수익 발생",
                String.format("'%s' 작품에서 %d원의 수익이 발생했습니다.", novelTitle, revenue),
                revenueId, "REVENUE");
    }

    public static NotificationEvent mentorshipRequest(Long mentorUserId, String applicantName, Long mentorshipId) {
        return new NotificationEvent(mentorUserId, NotificationType.MENTORSHIP_REQUEST,
                "멘토링 신청 도착",
                String.format("%s님이 멘토링을 신청했습니다.", applicantName),
                mentorshipId, "MENTORSHIP");
    }

    public static NotificationEvent mentorshipAccepted(Long applicantUserId, String mentorName, Long mentorshipId) {
        return new NotificationEvent(applicantUserId, NotificationType.MENTORSHIP_ACCEPTED,
                "멘토링 수락",
                String.format("%s 멘토님이 멘토링을 수락했습니다.", mentorName),
                mentorshipId, "MENTORSHIP");
    }

    public static NotificationEvent episodePublished(Long followerId, String authorName, String novelTitle, Long episodeId) {
        return new NotificationEvent(followerId, NotificationType.EPISODE_PUBLISHED,
                "구독 작가 신작 발행",
                String.format("%s 작가님의 '%s' 신규 회차가 발행되었습니다.", authorName, novelTitle),
                episodeId, "EPISODE");
    }

    public static NotificationEvent paymentSuccess(Long userId, String orderName, long amount, Long paymentId) {
        return new NotificationEvent(userId, NotificationType.PAYMENT_SUCCESS,
                "결제 성공",
                String.format("'%s' 결제가 완료되었습니다. (%d원)", orderName, amount),
                paymentId, "PAYMENT");
    }

    public static NotificationEvent paymentFailed(Long userId, String orderName, Long paymentId) {
        return new NotificationEvent(userId, NotificationType.PAYMENT_FAILED,
                "결제 실패",
                String.format("'%s' 결제에 실패했습니다. 다시 시도해주세요.", orderName),
                paymentId, "PAYMENT");
    }

    public static NotificationEvent subscriptionActivated(Long userId, Long subscriptionId) {
        return new NotificationEvent(userId, NotificationType.SUBSCRIPTION_ACTIVATED,
                "구독 활성화",
                "NovelCraft 프리미엄 구독이 시작되었습니다.",
                subscriptionId, "SUBSCRIPTION");
    }

    public static NotificationEvent subscriptionCancelled(Long userId, Long subscriptionId) {
        return new NotificationEvent(userId, NotificationType.SUBSCRIPTION_CANCELLED,
                "구독 취소",
                "NovelCraft 프리미엄 구독이 취소되었습니다.",
                subscriptionId, "SUBSCRIPTION");
    }

    public static NotificationEvent pointCharge(Long userId, long amount, long balance) {
        return new NotificationEvent(userId, NotificationType.POINT_CHARGE,
                "포인트 충전 완료",
                String.format("%dP 충전이 완료되었습니다. (잔액: %dP)", amount, balance),
                null, "POINT");
    }

    public static NotificationEvent paymentRefunded(Long userId, long amount, Long paymentId) {
        return new NotificationEvent(userId, NotificationType.PAYMENT_REFUNDED,
                "환불 완료",
                String.format("%d원 환불이 완료되었습니다.", amount),
                paymentId, "PAYMENT");
    }
}
