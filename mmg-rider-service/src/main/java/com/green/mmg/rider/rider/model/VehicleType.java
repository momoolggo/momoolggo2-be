package com.green.mmg.rider.rider.model;

/**
 * 라이더 배달수단.
 *
 * <ul>
 *   <li>{@link #WALK} — 도보</li>
 *   <li>{@link #BICYCLE} — 자전거</li>
 *   <li>{@link #MOTORBIKE} — 오토바이</li>
 *   <li>{@link #CAR} — 자동차</li>
 * </ul>
 *
 * <p>Figma 정정 1 화이트리스트 4종. Rider.vehicleType + WorkSession.vehicleType 둘 다 사용.</p>
 *
 * <p>R2-c tech-debt 처리 — String → enum 마이그레이션 (2026-05-10).
 * 학원 DB 데이터 0건이라 마이그레이션 영향 0.</p>
 */
public enum VehicleType {
    WALK,
    BICYCLE,
    MOTORBIKE,
    CAR
}
