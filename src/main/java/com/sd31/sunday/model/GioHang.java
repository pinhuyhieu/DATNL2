package com.sd31.sunday.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "gio_hang")
public class GioHang {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gio_hang_id", nullable = false)
    private Integer id;

    @NotNull
    @Column(name = "tai_khoan_id", nullable = false)
    private Integer taiKhoanId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chi_tiet_san_pham_id", nullable = false)
    private ChiTietSanPham chiTietSanPham;

    @NotNull
    @Column(name = "so_luong", nullable = false)
    private Integer soLuong;

}