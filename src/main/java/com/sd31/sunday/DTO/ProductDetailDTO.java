package com.sd31.sunday.DTO;

import com.sd31.sunday.model.SanPham;
import lombok.Data;

import java.util.List;

@Data
public class ProductDetailDTO {
    private SanPham sanPham;
    private List<ColorSizeStockDTO> colorSizeStocks;
}
