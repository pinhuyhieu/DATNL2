package com.sd31.sunday.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ma_giam_gia")
@Data
public class MaGiamGia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_giam_gia_id")
    private Integer maGiamGiaId;

    @Column(name = "ten_ma_giam_gia", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String tenMaGiamGia;

    @Column(name = "loai_giam_gia", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String loaiGiamGia;

    @Column(name = "gia_tri_giam_gia", nullable = false, precision = 10, scale = 2)
    private BigDecimal giaTriGiamGia;

    @Column(name = "ngay_bat_dau", nullable = false)
    private LocalDateTime ngayBatDau;

    @Column(name = "ngay_ket_thuc", nullable = false)
    private LocalDateTime ngayKetThuc;

    @Column(name = "so_luong", nullable = false)
    private Integer soLuong;

    @Column(name = "gia_tri_toi_thieu_don_hang", nullable = false, precision = 10, scale = 2, columnDefinition = "DECIMAL(10,2) DEFAULT 0")
    private BigDecimal giaTriToiThieuDonHang;

    @Column(name = "trang_thai", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String trangThai;

}
