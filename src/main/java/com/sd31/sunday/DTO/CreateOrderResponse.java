package com.sd31.sunday.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CreateOrderResponse extends GHNBaseResponse {
    private OrderData data;

    @Data
    public static class OrderData {
        @JsonProperty("order_code")
        private String orderCode;
        @JsonProperty("total_fee")
        private Integer totalFee;
        @JsonProperty("expected_delivery_time")
        private String expectedDeliveryTime;
    }
}
