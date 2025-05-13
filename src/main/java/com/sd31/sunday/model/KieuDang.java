package com.sd31.sunday.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Table(name = "kieu_dang")
@Data
public class KieuDang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "kieu_dang_id")
    private Integer kieuDangId;

    @Column(name = "ten_kieu_dang", nullable = false, length = 255, columnDefinition = "NVARCHAR(255)")
    private String tenKieuDang;

    @Column(name = "trang_thai", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String trangThai;

    @OneToMany(mappedBy = "kieuDang")
    private List<SanPham> sanPhams;
}