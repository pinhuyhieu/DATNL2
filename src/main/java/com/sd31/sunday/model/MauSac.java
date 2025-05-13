package com.sd31.sunday.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Table(name = "mau_sac")
@Data
public class MauSac {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mau_sac_id")
    private Integer mauSacId;

    @Column(name = "ten_mau_sac", nullable = false, length = 255, columnDefinition = "NVARCHAR(255)")
    private String tenMauSac;

    @Column(name = "trang_thai", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String trangThai;

    @OneToMany(mappedBy = "mauSac")
    private List<ChiTietSanPham> chiTietSanPhams;


}