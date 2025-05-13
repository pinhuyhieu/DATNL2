package com.sd31.sunday.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "phuong_thuc_thanh_toan")
@Data
public class PhuongThucThanhToan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "thanh_toan_id")
    private Integer thanhToanId;

    @ManyToOne
    @JoinColumn(name = "hoa_don_id", referencedColumnName = "hoa_don_id")
    private HoaDon hoaDon;

    @Column(name = "phuong_thuc")
    private String phuongThuc;

    @Column(name = "ngay_thanh_toan")
    private LocalDateTime ngayThanhToan;

    @Column(name = "so_tien")
    private BigDecimal soTien;

    @Column(name = "trang_thai")
    private String trangThai;
}