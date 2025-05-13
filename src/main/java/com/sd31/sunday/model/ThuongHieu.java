package com.sd31.sunday.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Table(name = "thuong_hieu") // Tên bảng snake_case
@Data
public class ThuongHieu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Sử dụng IDENTITY cho auto-increment
    @Column(name = "thuong_hieu_id")
    private Integer thuongHieuId; // Sử dụng Integer, không phải UUID

    @Column(name = "ten_thuong_hieu", nullable = false, length = 255, columnDefinition = "NVARCHAR(255)")
    private String tenThuongHieu;

    @Column(name = "trang_thai", nullable = false, columnDefinition = "NVARCHAR(255)") // Đặt nullable = false và columnDefinition = "NVARCHAR(255)"
    private String trangThai;

    @OneToMany(mappedBy = "thuongHieu") // Quan hệ 1-n với SanPham
    private List<SanPham> sanPhams; // Danh sách sản phẩm thuộc thương hiệu
}