package com.sd31.sunday.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "chi_tiet_hoa_don")
@Data
public class ChiTietHoaDon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chi_tiet_hoa_don_id")
    private Integer chiTietHoaDonId;

    @ManyToOne
    @JoinColumn(name = "hoa_don_id", referencedColumnName = "hoa_don_id")
    private HoaDon hoaDon;

    @ManyToOne
    @JoinColumn(name = "chi_tiet_san_pham_id", referencedColumnName = "chi_tiet_san_pham_id")
    private ChiTietSanPham chiTietSanPham;

    @Column(name = "so_luong")
    private Integer soLuong;

    @Column(name = "gia")
    private BigDecimal gia;

    @Column(name = "tong_tien")
    private BigDecimal tongTien;
}