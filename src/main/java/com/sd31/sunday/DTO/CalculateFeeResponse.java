package com.sd31.sunday.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CalculateFeeResponse {
    private int code;
    private String message;
    private FeeData data;

    @Getter
    @Setter
    public static class FeeData {
        private int total;
        private int service_fee;
        private int insurance_fee;
        // Các trường phí khác nếu cần
    }
}