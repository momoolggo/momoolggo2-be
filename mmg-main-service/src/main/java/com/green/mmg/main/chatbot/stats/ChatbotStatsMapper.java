package com.green.mmg.main.chatbot.stats;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 챗봇 트렌드/개인화 추천용 집계 쿼리 — MyBatis 영구 (복잡 JOIN).
 *
 * <p>Lv.5~9: 오늘 인기 카테고리 TOP N.<br>
 * Lv.10+: 사용자 1개월 이력 카테고리 빈도 (P-5).</p>
 */
@Mapper
public interface ChatbotStatsMapper {

    /**
     * 오늘 ORDER_STATE >= 3 (조리중부터)인 주문 카테고리 TOP N.
     * 결과 row: { category_name, cnt }
     */
    List<Map<String, Object>> findTodayTopCategories(@Param("limit") int limit);

    /**
     * 사용자 최근 30일 ORDER_STATE >= 3 주문 카테고리 빈도 TOP N (P-5).
     * 결과 row: { category_name, cnt }
     */
    List<Map<String, Object>> findUserRecentTopCategories(@Param("userNo") Long userNo,
                                                          @Param("since") LocalDate since,
                                                          @Param("limit") int limit);
}
