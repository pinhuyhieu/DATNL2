package com.sd31.sunday.DTO;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartItemDTO {
    private Integer gioHangId;
    private Integer sanPhamId;
    private Integer chiTietSanPhamId;
    private String tenSanPham;
    private String mauSac;
    private String kichCo;
    private Integer soLuong; // Quantity the user wants
    private BigDecimal gia;
    private String hinhAnhUrl;

    // New fields for availability check
    private boolean isAvailable; // True if requested quantity <= stock
    private String availabilityMessage; // Message if not available (e.g., "Hết hàng", "Chỉ còn X sản phẩm")
    private Integer maxAvailableQuantity; // The actual stock available for this item variant
}