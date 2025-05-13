package com.sd31.sunday.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class CouponResultDTO {
    private boolean success;
    private String message;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private String appliedCouponCode;

    // Constructor with 4 parameters (matching the current usage in CheckoutService)
    public CouponResultDTO(boolean success, String message, BigDecimal totalAmount, BigDecimal discountAmount) {
        this.success = success;
        this.message = message;
        this.totalAmount = totalAmount;
        this.discountAmount = discountAmount;
        this.appliedCouponCode = null; // Default to null when not specified
    }

    // Constructor with all 5 parameters
    public CouponResultDTO(boolean success, String message, BigDecimal totalAmount, BigDecimal discountAmount, String appliedCouponCode) {
        this.success = success;
        this.message = message;
        this.totalAmount = totalAmount;
        this.discountAmount = discountAmount;
        this.appliedCouponCode = appliedCouponCode;
    }
}