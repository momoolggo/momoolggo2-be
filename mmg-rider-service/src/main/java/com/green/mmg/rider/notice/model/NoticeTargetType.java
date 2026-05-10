package com.green.mmg.rider.notice.model;

/**
 * 공지 발송 대상 — POST /internal/rider/notice body.
 *
 * <ul>
 *   <li>{@link #ALL}: 전체 라이더</li>
 *   <li>{@link #RIDER}: 활성 라이더(ACTIVE/EATING)만 — R9 확장 후보</li>
 *   <li>{@link #SPECIFIC}: 특정 라이더 지정 — R9 확장 후보 (target_user_no 별도)</li>
 * </ul>
 */
public enum NoticeTargetType {
    ALL,
    RIDER,
    SPECIFIC
}
