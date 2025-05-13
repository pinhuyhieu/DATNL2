package com.sd31.sunday.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Table(name = "kich_co")
@Data
public class KichCo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "kich_co_id")
    private Integer kichCoId;

    @Column(name = "ten_kich_co", nullable = false, length = 255, columnDefinition = "NVARCHAR(255)")
    private String tenKichCo;

    @Column(name = "trang_thai", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String trangThai;

    @OneToMany(mappedBy = "kichCo")
    private List<ChiTietSanPham> chiTietSanPhams;

    @Transient // Không map vào database
    private List<Integer> mauSacIds;
}