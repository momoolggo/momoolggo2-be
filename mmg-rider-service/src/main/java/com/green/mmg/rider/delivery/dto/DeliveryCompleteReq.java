package com.green.mmg.rider.delivery.dto;

/**
 * PUT /api/rider/order/{deliveryNo}/complete 요청 Body — interfaces.md §6.2.
 *
 * <p>{@code deliveredMethod}: DIRECT / CUSTOMER_REQUEST / CUSTOMER_ABSENT (Figma 정정 10).
 * {@code deliveredPhotoUrl}: 사진 URL (선택, main-service /uploads/delivery/...).</p>
 */
public record DeliveryCompleteReq(
        String deliveredMethod,
        String deliveredPhotoUrl
) {
}
