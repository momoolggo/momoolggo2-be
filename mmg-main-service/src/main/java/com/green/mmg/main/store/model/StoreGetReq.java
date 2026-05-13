package com.green.mmg.main.store.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.web.bind.annotation.BindParam;
@Getter
@Setter
@ToString

public class StoreGetReq {
    private int currentPage;
    private int size;
    private String searchText;
    private int startIdx;
    private int categoryId;
    private String sortType = "order_count";  // 정렬 기준
    private String sortOrder = "DESC";

    public StoreGetReq(int currentPage, int size,int categoryId, @BindParam("search_text") String searchText) {
        this.currentPage = currentPage;
        this.size = size;
        this.searchText = searchText;
        this.startIdx = (currentPage - 1) * size;
        this.categoryId=categoryId;
    }
}
