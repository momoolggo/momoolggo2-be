package com.green.mmg.rider.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 라이더 도메인 설정 (rider.* prefix).
 *
 * <h3>D11 임시 운영 (admin-service 도입 전)</h3>
 * {@code autoApprove}는 ADR-001 D11 결정 — admin-service approve endpoint 의존성 우회.
 * <ul>
 *   <li>local / dev / 학원 발표: {@code true} (가입 직후 자동 ACTIVE)</li>
 *   <li>prod (admin 도입 후): {@code false} (PENDING 정상 흐름, admin approve 대기)</li>
 * </ul>
 *
 * <p>관련 ADR: {@code docs/adr/rider/ADR-001-service-boundary.md} "임시 운영" 섹션.</p>
 *
 * @param autoApprove 가입 직후 라이더를 자동으로 ACTIVE 전환할지 여부 (D11 임시 처리 toggle)
 */
@ConfigurationProperties(prefix = "rider")
public record RiderProperties(boolean autoApprove) {
}
