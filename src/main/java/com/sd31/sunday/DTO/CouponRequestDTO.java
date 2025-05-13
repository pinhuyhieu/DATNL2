package com.sd31.sunday.DTO;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CouponRequestDTO {
    private String couponCode;
    private BigDecimal subtotal;
}