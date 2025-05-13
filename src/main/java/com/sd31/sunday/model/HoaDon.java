package com.sd31.sunday.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "hoa_don")
@Data
public class HoaDon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hoa_don_id")
    private Integer hoaDonId;

    @Column(name = "ma_hoa_don")
    private String maHoaDon;

    @ManyToOne
    @JoinColumn(name = "khach_hang_id", referencedColumnName = "khach_hang_id")
    private KhachHang khachHang;

    @ManyToOne
    @JoinColumn(name = "nhan_vien_id", referencedColumnName = "nhan_vien_id")
    private NhanVien nhanVien;

    @Column(name = "ngay_tao_hoa_don")
    private LocalDateTime ngayTaoHoaDon;

    @Column(name = "ngay_ban")
    private LocalDateTime ngayBan;

    @ManyToOne
    @JoinColumn(name = "dia_chi_id", referencedColumnName = "dia_chi_id")
    private DiaChi diaChi; // Thay thế diaChiGiaoHang bằng quan hệ với DiaChi

    @ManyToOne
    @JoinColumn(name = "ma_giam_gia_id", referencedColumnName = "ma_giam_gia_id")
    private MaGiamGia maGiamGia;

    @Column(name = "kenh_ban_hang")
    private String kenhBanHang;

    @Column(name = "tong_tien")
    private BigDecimal tongTien;

    @Column(name = "trang_thai")
    private String trangThai;

    @Column(name = "nguoi_nhan_hang")
    private String nguoiNhanHang;

    @Column(name = "sdt_nhan_hang")
    private String sdtNhanHang;


    @OneToMany(mappedBy = "hoaDon", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ChiTietHoaDon> chiTietHoaDons;

    @OneToMany(mappedBy = "hoaDon", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PhuongThucThanhToan> phuongThucThanhToans;

    // Thêm mới các cột liên quan đến GHN
    @Column(name = "ma_don_hang_ghn")
    private String maDonHangGhn; // Mã đơn hàng do GHN trả về

    @Column(name = "phi_van_chuyen_ghn")
    private BigDecimal phiVanChuyenGhn; // Phí vận chuyển do GHN tính toán
    public HoaDon(Integer hoaDonId, String maHoaDon, KhachHang khachHang, NhanVien nhanVien, LocalDateTime ngayTaoHoaDon, LocalDateTime ngayBan, DiaChi diaChi, MaGiamGia maGiamGia, String kenhBanHang,String nguoiNhanHang, String sdtNhanHang, BigDecimal tongTien, String trangThai, List<ChiTietHoaDon> chiTietHoaDons, List<PhuongThucThanhToan> phuongThucThanhToans) {
        this.hoaDonId = hoaDonId;
        this.maHoaDon = maHoaDon;
        this.khachHang = khachHang;
        this.nhanVien = nhanVien;
        this.ngayTaoHoaDon = ngayTaoHoaDon;
        this.ngayBan = ngayBan;
        this.diaChi = diaChi;
        this.maGiamGia = maGiamGia;
        this.kenhBanHang = kenhBanHang;
        this.tongTien = tongTien;
        this.trangThai = trangThai;
        this.nguoiNhanHang = nguoiNhanHang;
        this.sdtNhanHang = sdtNhanHang;
        this.chiTietHoaDons = chiTietHoaDons;
        this.phuongThucThanhToans = phuongThucThanhToans;
    }


    //
    public HoaDon() {

    }

    public HoaDon(Integer id) {
        this.hoaDonId = id;
    }

    //
    public HoaDon( String maHoaDon, KhachHang khachHang, LocalDateTime ngayTaoHoaDon, LocalDateTime ngayBan,  String kenhBanHang, BigDecimal tongTien, String trangThai, String nguoiNhanHang, String sdtNhanHang) {
        this.maHoaDon = maHoaDon;
        this.khachHang = khachHang;
        this.ngayTaoHoaDon = ngayTaoHoaDon;
        this.ngayBan = ngayBan;
        this.kenhBanHang = kenhBanHang;
        this.tongTien = tongTien;
        this.trangThai = trangThai;
        this.nguoiNhanHang = nguoiNhanHang;
        this.sdtNhanHang = sdtNhanHang;

    }
}
