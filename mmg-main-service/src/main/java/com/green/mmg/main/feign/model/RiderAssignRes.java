package com.green.mmg.main.feign.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RiderAssignRes {
    private Boolean assigned;
    private Long riderId;
}