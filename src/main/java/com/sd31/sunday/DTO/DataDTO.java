package com.sd31.sunday.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor // Optional: Add if you need a no-args constructor
public class DataDTO {
    private Integer id; // chiTietSanPhamId
    private String tenKichCo;
    private BigDecimal giaBan;
    private Integer availableStock; // Add available stock count
    private Integer physicalStock; // Add physical stock count
}