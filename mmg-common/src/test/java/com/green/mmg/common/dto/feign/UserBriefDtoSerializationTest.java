package com.green.mmg.common.dto.feign;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 4-A 백필: UserBriefDto Jackson 직렬화/역직렬화 동결.
 *
 * <p>Feign consumer 4곳(StoreService.storeOneGet/getStoreReviews,
 * OrderService.getOrderInfo, OwnerService.getOrders)이 의존하는 필드:
 * userNo, name, tel, address. address는 항상 "" — main이 자체 user_address 채움.</p>
 *
 * <p>필드 추가/제거 시 consumer가 조용히 깨지는 위험 차단. 필드명·타입·라운드트립 동결.</p>
 */
@DisplayName("UserBriefDto Jackson 직렬화 동결 (Feign consumer 보호)")
class UserBriefDtoSerializationTest {

    private final ObjectMapper mapper = JsonMapper.builder().build();

    @Test
    @DisplayName("라운드트립 동결: 직렬화 키 4개(userNo/name/tel/address) + 역직렬화 값 보존")
    void serializationRoundtrip_freezesFieldNames() throws Exception {
        UserBriefDto original = new UserBriefDto(42L, "준하", "010-1111-2222", "");

        // 1) 직렬화
        String json = mapper.writeValueAsString(original);

        // 2) JSON 키 동결 — 필드 추가/제거 시 consumer 깨짐 검출
        JsonNode node = mapper.readTree(json);
        assertThat(node.size())
                .as("UserBriefDto JSON 키 개수 = 4 (필드 변동 감지)")
                .isEqualTo(4);
        assertThat(node.has("userNo")).isTrue();
        assertThat(node.has("name")).isTrue();
        assertThat(node.has("tel")).isTrue();
        assertThat(node.has("address")).isTrue();

        // 3) 역직렬화 라운드트립 — Feign decoder 호환성 (NoArgs+Setter 또는 AllArgs 생성자)
        UserBriefDto roundtrip = mapper.readValue(json, UserBriefDto.class);
        assertThat(roundtrip.getUserNo()).isEqualTo(42L);
        assertThat(roundtrip.getName()).isEqualTo("준하");
        assertThat(roundtrip.getTel()).isEqualTo("010-1111-2222");
        assertThat(roundtrip.getAddress()).isEqualTo("");
    }
}
