package com.green.mmg.main.order.model;

import lombok.Data;

@Data
public class OrderReqDto {
    // userNo는 JWT에서 추출 → 프론트에서 받지 않음
    // storeId, 금액도 서버에서 계산 → 프론트에서 받지 않음
    private String  request;        // 가게 요청사항
    private String  riderRequest;   // 라이더 요청사항
    private Integer payState;       // 결제수단 (1:카드, 2:카카오, 3:네이버, 4:만나서)
    private Long    addressId;      // 선택한 주소 ID
}