package com.green.mmg.main.owner.model;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class OwnerSalesRankingRes {
    private String menuName;
    private int totalQuantity;
    private long totalSales;
    private int prevQuantity;   // 전주 판매개수 추가
    private long prevSales;     // 전주 판매금액 추가
}