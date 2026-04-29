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
}
