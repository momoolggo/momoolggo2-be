package com.green.mmg.rider.notice.model;

/**
 * 공지 카테고리 — ADR-002 line 184 + ADR-009 (Figma 정정 8).
 *
 * <ul>
 *   <li>{@link #IMPORTANT}: 중요 — 핵심 권고/안전 지침 등 (UI 적색 — 프론트 책임)</li>
 *   <li>{@link #SAFETY}: 안전 — 교통/장비 안전 (UI 황색)</li>
 *   <li>{@link #GENERAL}: 일반 — 기타 안내 (UI 회색)</li>
 * </ul>
 *
 * <p>UI 색상 매핑은 프론트 책임 (백엔드는 enum만 — ADR-009 line 154).</p>
 */
public enum NoticeCategory {
    IMPORTANT,
    SAFETY,
    GENERAL
}
