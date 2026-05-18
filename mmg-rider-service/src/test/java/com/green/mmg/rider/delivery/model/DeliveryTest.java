package com.green.mmg.rider.delivery.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Delivery entity 단위 테스트 (JPA / Repository / DB 의존 0).
 *
 * <p>R1-A RiderServiceTest Mockito 패턴 일관 — 학원 DB 의존성 회피 (Q-DB (다) 보류).
 * R3 DeliveryService 진입 시 통합 테스트(@DataJpaTest 또는 @SpringBootTest) 추가.</p>
 *
 * <p>검증 본질: 명시 생성자 결과 (가입 시점 status WAITING_ASSIGN 고정 / extra_fee 0 고정 /
 * 필드 매핑 정확 / nullable / DECIMAL 정밀도 보존).</p>
 */
@DisplayName("Delivery entity 단위")
class DeliveryTest {

    @Test
    @DisplayName("생성자: 필수 필드 + status WAITING_ASSIGN 고정 + extra_fee 0 고정")
    void constructor_setsRequiredFields_andDefaults() {
        Delivery delivery = new Delivery(
                "00001ABC", 1L,
                "010-1111-1111", "010-2222-2222",
                "가게 주소", 37.1234567890123, 127.1234567890123,
                "손님 주소", 37.5678901234567, 127.5678901234567,
                3000
        );

        assertThat(delivery.getDeliveryNo()).isEqualTo("00001ABC");
        assertThat(delivery.getOrderId()).isEqualTo(1L);
        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.WAITING_ASSIGN);
        assertThat(delivery.getBaseFee()).isEqualTo(3000);
        assertThat(delivery.getExtraFee()).isEqualTo(0);
        assertThat(delivery.getRiderNo()).isNull();
        assertThat(delivery.getPickupPhone()).isEqualTo("010-1111-1111");
        assertThat(delivery.getCustomerPhone()).isEqualTo("010-2222-2222");
        assertThat(delivery.getPickupAddress()).isEqualTo("가게 주소");
        assertThat(delivery.getDeliveryAddress()).isEqualTo("손님 주소");
        assertThat(delivery.getPickupLat()).isCloseTo(37.1234567890123, within(1e-13));
        assertThat(delivery.getPickupLng()).isCloseTo(127.1234567890123, within(1e-13));
        assertThat(delivery.getDeliveryLat()).isCloseTo(37.5678901234567, within(1e-13));
        assertThat(delivery.getDeliveryLng()).isCloseTo(127.5678901234567, within(1e-13));
    }

    @Test
    @DisplayName("생성자: 좌표/전화/주소 nullable 필드 null 허용 + status 고정 유지")
    void constructor_nullableFields_allowsNull() {
        Delivery delivery = new Delivery(
                "00002ABC", 2L,
                null, null, null, null, null, null, null, null,
                3000
        );

        assertThat(delivery.getPickupPhone()).isNull();
        assertThat(delivery.getCustomerPhone()).isNull();
        assertThat(delivery.getPickupAddress()).isNull();
        assertThat(delivery.getPickupLat()).isNull();
        assertThat(delivery.getPickupLng()).isNull();
        assertThat(delivery.getDeliveryAddress()).isNull();
        assertThat(delivery.getDeliveryLat()).isNull();
        assertThat(delivery.getDeliveryLng()).isNull();
        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.WAITING_ASSIGN);
        assertThat(delivery.getExtraFee()).isEqualTo(0);
    }

    @Test
    @DisplayName("생성자: DECIMAL(16,13) 좌표 정밀도 보존 (Java Double 한계 내)")
    void constructor_decimalCoordinates_precisionPreserved() {
        Delivery delivery = new Delivery(
                "00003ABC", 3L,
                null, null, null,
                37.1234567890123, 127.1234567890123,
                null,
                37.5678901234567, 127.5678901234567,
                3000
        );

        assertThat(delivery.getPickupLat()).isCloseTo(37.1234567890123, within(1e-13));
        assertThat(delivery.getPickupLng()).isCloseTo(127.1234567890123, within(1e-13));
        assertThat(delivery.getDeliveryLat()).isCloseTo(37.5678901234567, within(1e-13));
        assertThat(delivery.getDeliveryLng()).isCloseTo(127.5678901234567, within(1e-13));
    }
}
