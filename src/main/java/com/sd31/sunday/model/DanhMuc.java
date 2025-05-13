package com.sd31.sunday.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Table(name = "danh_muc")
@Data
public class DanhMuc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "danh_muc_id")
    private Integer danhMucId;

    @Column(name = "ten_danh_muc", nullable = false, length = 255, columnDefinition = "NVARCHAR(255)")
    private String tenDanhMuc;

    @Column(name = "trang_thai", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String trangThai;

    @OneToMany(mappedBy = "danhMuc")
    private List<SanPham> sanPhams;
}