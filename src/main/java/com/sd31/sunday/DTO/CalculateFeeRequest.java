package com.sd31.sunday.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CalculateFeeRequest {
    private int from_district_id;
    private String from_ward_code;
    private int service_id;
    private int to_district_id;
    private String to_ward_code;
    private int height;
    private int length;
    private int weight;
    private int width;
    private int insurance_value;
    // Các trường khác nếu cần thiết theo API docs
}
