package com.sd31.sunday.service;

import com.sd31.sunday.DTO.CartItemDTO;
import com.sd31.sunday.model.GioHang;
import com.sd31.sunday.repository.GioHangRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class GioHangService {

    @Autowired
    private GioHangRepository gioHangRepository;


    public List<CartItemDTO> getCartItemsByKhachHangId(Integer khachHangId) {
        List<GioHang> gioHangs = gioHangRepository.findByTaiKhoanId(khachHangId); // Example query - adjust as needed

        List<CartItemDTO> cartItemDTOs = new ArrayList<>();
        for (GioHang gioHang : gioHangs) {
            CartItemDTO dto = new CartItemDTO();
            dto.setGioHangId(gioHang.getId());
            dto.setSanPhamId(gioHang.getChiTietSanPham().getSanPham().getSanPhamId()); // Example - get sanPhamId (if needed)
            dto.setChiTietSanPhamId(gioHang.getChiTietSanPham().getChiTietSanPhamId()); // **Crucially, set chiTietSanPhamId**
            dto.setTenSanPham(gioHang.getChiTietSanPham().getSanPham().getTenSanPham());
            dto.setMauSac(gioHang.getChiTietSanPham().getMauSac().getTenMauSac());
            dto.setKichCo(gioHang.getChiTietSanPham().getKichCo().getTenKichCo());
            dto.setSoLuong(gioHang.getSoLuong());
            dto.setGia(gioHang.getChiTietSanPham().getGiaBan());
            // Assuming you have a way to get the image URL from ChiTietSanPham or SanPham
            if (!gioHang.getChiTietSanPham().getSanPham().getHinhAnhs().isEmpty()) {
                dto.setHinhAnhUrl(gioHang.getChiTietSanPham().getSanPham().getHinhAnhs().get(0).getHinhAnhUrl()); // Example - get first image
            }

            cartItemDTOs.add(dto);
        }
        return cartItemDTOs;
    }

    public BigDecimal calculateTotalPrice(List<CartItemDTO> cartItems) {
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (CartItemDTO item : cartItems) {
            totalPrice = totalPrice.add(item.getGia().multiply(BigDecimal.valueOf(item.getSoLuong())));
        }
        return totalPrice;
    }

    public void clearCart(Integer khachHangId) {
        List<GioHang> gioHangs = gioHangRepository.findByTaiKhoanId(khachHangId);
        gioHangRepository.deleteAll(gioHangs);
    }
}