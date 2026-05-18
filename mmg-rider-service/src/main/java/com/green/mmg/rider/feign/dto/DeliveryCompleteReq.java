package com.green.mmg.rider.feign.dto;

import java.time.LocalDateTime;

/**
 * Rider → Main: 배달 완료 처리 (interfaces.md §2.2).
 *
 * <p>main Provider 측 {@code DeliveryCompleteReq} (`mmg-main-service/.../internal/dto/`) 5 필드 1:1 일관.
 * 외부 endpoint용 {@code delivery/dto/DeliveryCompleteReq} (2 필드)와 영역 분리 (case-#34 강제 패턴 일관).</p>
 *
 * <p>{@code deliveryNo}: path variable로 전달되는 외부 호출자 데이터를 Feign 호출 시 body에 보강.
 * {@code riderNo}: {@code DeliveryTransitionResult.riderNo()}에서 매핑.
 * {@code completedAt}: {@code DeliveryTransitionResult.changedAt()}에서 매핑.</p>
 */
public record DeliveryCompleteReq(
        String deliveryNo,
        Long riderNo,
        String deliveredMethod,
        String deliveredPhotoUrl,
        LocalDateTime completedAt
) {
}
