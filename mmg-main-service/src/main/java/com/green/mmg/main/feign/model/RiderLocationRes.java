package com.green.mmg.main.feign.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class RiderLocationRes {
    private Long riderNo;
    private Double lat;
    private Double lng;
    private LocalDateTime updatedAt;
}