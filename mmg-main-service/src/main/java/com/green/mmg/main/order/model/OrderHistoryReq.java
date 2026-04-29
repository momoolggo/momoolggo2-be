package com.green.mmg.main.order.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor   // @ModelAttribute 바인딩용 (Spring reflection)
public class OrderHistoryReq {
    private long userId;        // 사용자 고유 번호
    private int currentPage;    // 현재 페이지 (1, 2, 3...)
    private int size;           // 한 페이지당 보여줄 개수
    private int startIdx;       // MyBatis에서 사용할 시작 인덱스 (Offset)

    // 생성자: 페이지 번호를 인덱스로 변환하는 로직 포함
    public OrderHistoryReq(long userId, int currentPage, int size) {
        this.userId = userId;
        this.currentPage = currentPage > 0 ? currentPage : 1; // 0 이하 방지
        this.size = size > 0 ? size : 10;                     // 기본값 설정
        this.startIdx = (this.currentPage - 1) * this.size;
    }
}
