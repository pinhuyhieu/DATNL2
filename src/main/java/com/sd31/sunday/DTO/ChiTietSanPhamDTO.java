package com.sd31.sunday.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class ChiTietSanPhamDTO {
    private String kichCo;
    private String mauSac;
    private BigDecimal giaBan;
    private Integer soLuongTon;
}
