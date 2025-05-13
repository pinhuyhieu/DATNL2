package com.sd31.sunday.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class CreateOrderRequest {
    @JsonProperty("payment_type_id")
    private Integer paymentTypeId; // 1 for COD
    private String note;
    @JsonProperty("required_note")
    private String requiredNote; // KHONGCHOXEMHANG
    @JsonProperty("from_name")
    private String fromName;
    @JsonProperty("from_phone")
    private String fromPhone;
    @JsonProperty("from_address")
    private String fromAddress;
    @JsonProperty("from_ward_name")
    private String fromWardName;
    @JsonProperty("from_district_name")
    private String fromDistrictName;
    @JsonProperty("from_province_name")
    private String fromProvinceName;
    @JsonProperty("to_name")
    private String toName;
    @JsonProperty("to_phone")
    private String toPhone;
    @JsonProperty("to_address")
    private String toAddress;
    @JsonProperty("to_ward_code")
    private String toWardCode;
    @JsonProperty("to_district_id")
    private Integer toDistrictId;
    @JsonProperty("to_province_name")
    private String toProvinceName;
    @JsonProperty("cod_amount")
    private Integer codAmount; // For COD orders
    private Integer weight;
    private Integer length;
    private Integer width;
    private Integer height;
    @JsonProperty("service_id")
    private Integer serviceId;
    @JsonProperty("insurance_value")
    private Integer insuranceValue;
    private List<Item> items;

    @Data
    public static class Item {
        private String name;
        private String code;
        private Integer quantity;
        private Integer price;
        private Integer length;
        private Integer width;
        private Integer height;
    }
}