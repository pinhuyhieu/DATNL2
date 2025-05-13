package com.sd31.sunday.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "chi_tiet_san_pham")
@Data
public class ChiTietSanPham {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chi_tiet_san_pham_id")
    private Integer chiTietSanPhamId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "san_pham_id", nullable = false)
    private SanPham sanPham;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "kich_co_id", nullable = false)
        private KichCo kichCo;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "mau_sac_id", nullable = false)
        private MauSac mauSac;

        @Column(name = "gia_nhap", nullable = false)
        private BigDecimal giaNhap;

        @Column(name = "gia_ban", nullable = false)
        private BigDecimal giaBan;

        @Column(name = "so_luong_ton", nullable = false)
        private Integer soLuongTon;

        @Column(name = "mo_ta")
        private String moTa;

        @Column(name = "trang_thai", nullable = false)
        private String trangThai;

        @Transient // Tells JPA to ignore this field for persistence
        private Integer availableStock;

        public Integer getAvailableStock() {
            return availableStock;
        }

        public void setAvailableStock(Integer availableStock) {
            this.availableStock = availableStock;
        }
    //
    public ChiTietSanPham(Integer id) {
        this.chiTietSanPhamId = id;
    }

        public ChiTietSanPham() {

    }}
