package com.green.mmg.main.store.model;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class StoreGetRes {
    private String name;
    private int id;
    private int min;
    private int sum;
    private int count;
    private int avg;
    private String pic;
    private double distance;
    private int state;
}
