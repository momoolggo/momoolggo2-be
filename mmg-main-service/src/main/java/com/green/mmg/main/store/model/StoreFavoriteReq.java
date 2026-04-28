package com.green.mmg.main.store.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class StoreFavoriteReq {
    private long userNo;
    private int currentPage;
    private int size;


    public int getStartIdx() {
        // currentPage가 0이나 음수로 들어오는 경우를 대비한 방어 코드
        int current = (currentPage < 1) ? 1 : currentPage;
        return (current - 1) * size;
    }
}