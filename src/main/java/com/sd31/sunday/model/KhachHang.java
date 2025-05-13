package com.sd31.sunday.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "khach_hang") // Tên bảng snake_case
@Data
public class KhachHang {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increment của SQL Server
    @Column(name = "khach_hang_id")
    private Integer khachHangId; // Sử dụng Integer, không phải UUID

    @Column(name = "ma_khach_hang", unique = true) // Thêm unique = true
    private String maKhachHang;

    @Column(name = "ten", columnDefinition = "nvarchar(255)")
    private String ten;

    @Column(name = "email", unique = true) // Thêm unique = true
    private String email;

    @Column(name = "mat_khau")
    private String matKhau;

    @Column(name = "so_dien_thoai", unique = true) // Thêm unique = true
    private String soDienThoai;

    @Column(name = "trang_thai", columnDefinition = "nvarchar(255)") // Giữ NVARCHAR cho đồng nhất
    private String trangThai; // Thay đổi thành String để map với NVARCHAR

}