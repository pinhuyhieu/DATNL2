package com.sd31.sunday.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Table(name = "dia_chi")
@Data
public class DiaChi {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dia_chi_id")
    private Integer diaChiId;

    @ManyToOne
    @JoinColumn(name = "khach_hang_id", referencedColumnName = "khach_hang_id", nullable = false)
    private KhachHang khachHang;

    @Column(name = "thanh_pho", nullable = false)
    private String thanhPho;

    @Column(name = "quan_huyen")
    private String quanHuyen;

    @Column(name = "phuong_xa")
    private String phuongXa;

    @Column(name = "ghi_chu")
    private String ghiChu;

    @Column(name = "mac_dinh", nullable = false)
    private Boolean macDinh;

    // Liên kết ngược với HoaDon
    @OneToMany(mappedBy = "diaChi", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<HoaDon> hoaDons;
}
