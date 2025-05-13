    package com.sd31.sunday.model;

    import jakarta.persistence.*;
    import lombok.Data;

    import java.time.LocalDateTime;

    @Entity
    @Table(name = "lich_su_trang_thai")
    @Data
    public class LichSuTrangThai {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "lich_su_id")
        private Integer lichSuId;

        @ManyToOne
        @JoinColumn(name = "hoa_don_id", referencedColumnName = "hoa_don_id")
        private HoaDon hoaDon;

        @Column(name = "trang_thai_cu")
        private String trangThaiCu;

        @Column(name = "trang_thai_moi")
        private String trangThaiMoi;

        @Column(name = "ngay_thay_doi")
        private LocalDateTime ngayThayDoi;

        @Column(name = "nhan_vien_id")
        private Integer nhanVienId;

        @Column(name = "ghi_chu")
        private String ghiChu;
    }