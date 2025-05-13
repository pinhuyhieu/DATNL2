package com.sd31.sunday.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "san_pham")
@Data
public class SanPham {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "san_pham_id")
    private Integer sanPhamId;

    @Column(name = "ma_san_pham")
    private String maSanPham;

//    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Column(name = "ten_san_pham")
    private String tenSanPham;

    @Column(name = "mo_ta", columnDefinition = "NVARCHAR(MAX)")
    private String moTa;

    @OneToMany(mappedBy = "sanPham", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<HinhAnh> hinhAnhs = new ArrayList<>();

    @OneToMany(mappedBy = "sanPham", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ChiTietSanPham> chiTietSanPhams;

    @NotNull(message = "Danh mục không được để trống")
    @ManyToOne
    @JoinColumn(name = "danh_muc_id", referencedColumnName = "danh_muc_id")
    private DanhMuc danhMuc;

    @NotNull(message = "Thương hiệu không được để trống")
    @ManyToOne
    @JoinColumn(name = "thuong_hieu_id", referencedColumnName = "thuong_hieu_id")
    private ThuongHieu thuongHieu;

    @NotNull(message = "Chất liệu không được để trống")
    @ManyToOne
    @JoinColumn(name = "chat_lieu_id", referencedColumnName = "chat_lieu_id")
    private ChatLieu chatLieu;

    @NotNull(message = "Kiểu dáng không được để trống")
    @ManyToOne
    @JoinColumn(name = "kieu_dang_id", referencedColumnName = "kieu_dang_id")
    private KieuDang kieuDang;

    @Column(name = "ngay_them")
    private Date ngayThem;

    @Column(name = "trang_thai")
    private String trangThai;

    public void setGiaBanHienThi(BigDecimal zero) {
    }
    @Transient
    private String imageUrl;
}
