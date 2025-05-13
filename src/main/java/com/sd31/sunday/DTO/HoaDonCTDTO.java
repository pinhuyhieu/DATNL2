package com.sd31.sunday.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HoaDonCTDTO {

    private Integer chiTietHoaDonId;
    private Integer soLuong;
    private BigDecimal gia;
    private Integer idSanPhamCT;
    private String maspct;
    private Integer idHoadon;
    private BigDecimal thanhTien;
    private Integer trangThai;

}
