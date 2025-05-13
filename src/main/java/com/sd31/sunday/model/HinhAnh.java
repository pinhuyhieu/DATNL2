package com.sd31.sunday.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Entity
@Table(name = "hinh_anh")
@Data
public class HinhAnh {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hinh_anh_id")
    private Integer hinhAnhId;

    @NotBlank(message = "URL hình ảnh không được để trống")
    @Column(name = "hinh_anh_url")
    private String hinhAnhUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "san_pham_id")
    private SanPham sanPham;

}