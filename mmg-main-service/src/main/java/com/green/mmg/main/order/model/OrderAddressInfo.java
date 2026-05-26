package com.green.mmg.main.order.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor   // Phase 3-D-B: UserAddressRepository.findDefaultByUserNo JPQL constructor expression
public class OrderAddressInfo {
    private String address;
    private String addressDetail;
    // 2026-05-25 9건 트랙 정정 — 거리 기반 배달팁 계산용 좌표
    private Double latitude;
    private Double longitude;
}
