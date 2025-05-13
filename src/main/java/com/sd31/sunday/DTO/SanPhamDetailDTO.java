package com.sd31.sunday.DTO;

import lombok.Data;

import java.util.List;

@Data
public class SanPhamDetailDTO {
    private Integer sanPhamId;
    private String tenSanPham;
    private String moTa;
    private String danhMuc;
    private String thuongHieu;
    private String chatLieu;
    private String kieuDang;
    private List<String> hinhAnhUrls;
    private List<ChiTietSanPhamDTO> chiTietSanPhams;
}
