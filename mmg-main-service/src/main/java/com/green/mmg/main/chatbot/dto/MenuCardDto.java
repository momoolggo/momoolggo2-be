package com.green.mmg.main.chatbot.dto;

import com.green.mmg.main.store.model.StoreGetRes;
import lombok.Getter;

/**
 * 챗봇 메뉴 추천 카드 — 멘토 피드백 #1+#2 (2026-06-06).
 * Gemini 추천 키워드로 storeMapper.searchStore 후 가게 단위로 노출.
 * FE 클릭 시 /store/{storeId}로 이동.
 */
@Getter
public class MenuCardDto {
    private final long storeId;
    private final String storeName;
    private final String storePic;
    private final int minPrice;
    private final int ratingAvg;
    private final int ratingCount;
    private final String matchedKeyword;

    public MenuCardDto(StoreGetRes store, String matchedKeyword) {
        this.storeId = store.getId();
        this.storeName = store.getName();
        this.storePic = store.getPic();
        this.minPrice = store.getMin();
        this.ratingAvg = store.getAvg();
        this.ratingCount = store.getSum();
        this.matchedKeyword = matchedKeyword;
    }
}
