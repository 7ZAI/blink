package com.blink.base.dto.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class PointVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String secretKey;

    public int x;

    public int y;
}