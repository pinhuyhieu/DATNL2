package com.sd31.sunday.service;

import com.sd31.sunday.model.HoaDon;
import com.sd31.sunday.model.MaGiamGia;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class OrderCalculationService {

    public BigDecimal calculateSubtotal(HoaDon hoaDon) {
        if (hoaDon == null || hoaDon.getChiTietHoaDons() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal subtotal = BigDecimal.ZERO;
        for (var ct : hoaDon.getChiTietHoaDons()) {
            if (ct.getGia() != null && ct.getSoLuong() != null) {
                subtotal = subtotal.add(ct.getGia().multiply(BigDecimal.valueOf(ct.getSoLuong())));
            }
        }
        return subtotal;
    }

    public BigDecimal calculateDiscountAmount(HoaDon hoaDon) {
        if (hoaDon == null || hoaDon.getMaGiamGia() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal subtotal = calculateSubtotal(hoaDon);
        MaGiamGia maGiamGia = hoaDon.getMaGiamGia();
        BigDecimal giaTriGiamGia = maGiamGia.getGiaTriGiamGia();
        if (giaTriGiamGia == null) {
            return BigDecimal.ZERO;
        }
        if ("Phần trăm".equals(maGiamGia.getLoaiGiamGia())) {
            return subtotal.multiply(giaTriGiamGia).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        } else if ("Số tiền cố định".equals(maGiamGia.getLoaiGiamGia())) {
            return giaTriGiamGia;
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal calculateTotalPayment(HoaDon hoaDon) {
        BigDecimal subtotal = calculateSubtotal(hoaDon);
        BigDecimal discountAmount = calculateDiscountAmount(hoaDon);
        BigDecimal total = subtotal.subtract(discountAmount);
        if (hoaDon != null && hoaDon.getPhiVanChuyenGhn() != null) {
            total = total.add(hoaDon.getPhiVanChuyenGhn());
        }
        return total;
    }
}