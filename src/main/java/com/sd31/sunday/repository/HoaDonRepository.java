package com.sd31.sunday.repository;

import com.sd31.sunday.model.HoaDon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface HoaDonRepository extends JpaRepository<HoaDon, Integer> , JpaSpecificationExecutor<HoaDon> {

    Optional<HoaDon> findByMaHoaDon(String maHoaDon);

    // Phương thức lọc theo mã hóa đơn (chứa) và trạng thái (chính xác)
    Page<HoaDon> findByMaHoaDonContainingAndTrangThai(String maHoaDon, String trangThai, Pageable pageable);

    // Phương thức lọc theo mã hóa đơn (chứa)
    Page<HoaDon> findByMaHoaDonContaining(String maHoaDon, Pageable pageable);

    // Phương thức lọc theo trạng thái (chính xác)
    Page<HoaDon> findByTrangThai(String trangThai, Pageable pageable);

    // Phương thức lọc theo ngày tạo hóa đơn trong khoảng
    // NOTE: Consider migrating these Date methods to use LocalDate for consistency
    Page<HoaDon> findByNgayTaoHoaDonBetween(Date ngayTaoTu, Date ngayTaoDen, Pageable pageable);

    // Phương thức lọc theo tất cả các tiêu chí (mã hóa đơn, trạng thái, ngày tạo)
    Page<HoaDon> findByMaHoaDonContainingAndTrangThaiAndNgayTaoHoaDonBetween(
            String maHoaDon, String trangThai, Date ngayTaoTu, Date ngayTaoDen, Pageable pageable);

    // New methods for Kenh Ban Hang filter:

    // Filter by Kenh Ban Hang
    Page<HoaDon> findByKenhBanHang(String kenhBanHang, Pageable pageable);

    // Filter by Kenh Ban Hang and Date range
    Page<HoaDon> findByKenhBanHangAndNgayTaoHoaDonBetween(String kenhBanHang, Date ngayTaoTu, Date ngayTaoDen, Pageable pageable);

    // Filter by Trang Thai and Kenh Ban Hang
    Page<HoaDon> findByTrangThaiAndKenhBanHang(String trangThai, String kenhBanHang, Pageable pageable);

    // Filter by Ma Hoa Don, Kenh Ban Hang
    Page<HoaDon> findByMaHoaDonContainingAndKenhBanHang(String maHoaDon, String kenhBanHang, Pageable pageable);

    // Filter by Trang Thai, Kenh Ban Hang, Date range
    Page<HoaDon> findByTrangThaiAndKenhBanHangAndNgayTaoHoaDonBetween(String trangThai, String kenhBanHang, Date ngayTaoTu, Date ngayTaoDen, Pageable pageable);

    // Filter by Ma Hoa Don, Kenh Ban Hang, Date range
    Page<HoaDon> findByMaHoaDonContainingAndKenhBanHangAndNgayTaoHoaDonBetween(String maHoaDon, String kenhBanHang, Date ngayTaoTu, Date ngayTaoDen, Pageable pageable);

    // Filter by Ma Hoa Don, Trang Thai, Kenh Ban Hang
    Page<HoaDon> findByMaHoaDonContainingAndTrangThaiAndKenhBanHang(String maHoaDon, String trangThai, String kenhBanHang, Pageable pageable);

    // Filter by Ma Hoa Don, Trang Thai, Kenh Ban Hang, Date range
    Page<HoaDon> findByMaHoaDonContainingAndTrangThaiAndKenhBanHangAndNgayTaoHoaDonBetween(String maHoaDon, String trangThai, String kenhBanHang, Date ngayTaoTu, Date ngayTaoDen, Pageable pageable);

    // Missing methods identified previously (keeping them as is)
    Page<HoaDon> findByMaHoaDonContainingAndNgayTaoHoaDonBetween(String maHoaDon, Date ngayTaoTu, Date ngayTaoDen, Pageable pageable);
    Page<HoaDon> findByTrangThaiAndNgayTaoHoaDonBetween(String trangThai, Date ngayTaoTu, Date ngayTaoDen, Pageable pageable);

    // New method to find HoaDon by KhachHangId
    List<HoaDon> findByKhachHangKhachHangId(Integer khachHangId);

    Page<HoaDon> findByKhachHangKhachHangIdAndKenhBanHang(Integer khachHangId, String kenhBanHang, Pageable pageable);


    long count();
    long countByTrangThai(String trangThai);

    List<HoaDon> findByTrangThai(String trangThai);
    boolean existsByMaHoaDon(String maHoaDon);

    Optional<HoaDon> findFirstByOrderByMaHoaDonDesc();

    @Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END FROM hoa_don WHERE ma_hoa_don = :maHoaDon", nativeQuery = true)
    int existsByMaHoaDonQuery(@Param("maHoaDon") String maHoaDon);

    @Query(value = "SELECT * FROM hoa_don ORDER BY ma_hoa_don DESC LIMIT 1", nativeQuery = true)
    Optional<HoaDon> findFirstByOrderByMaHoaDonDescQuery();

    @Query("SELECT HD FROM HoaDon HD WHERE HD.hoaDonId =:id ")
    HoaDon findid(Integer id);

    @Query("SELECT h FROM HoaDon h WHERE h.khachHang IS NULL")
    List<HoaDon> findPendingHoaDonsWithoutKhachHang();

    // Doanh thu tổng cộng theo trạng thái (Original single status - maybe keep for other uses?)
    @Query("SELECT SUM(hd.tongTien) FROM HoaDon hd WHERE hd.trangThai = :trangThai")
    BigDecimal findTongDoanhThu(@Param("trangThai") String trangThai);

    // Doanh thu theo ngày (Native Query)
    @Query(value = "SELECT CONVERT(date, hd.ngay_ban) AS ngay, SUM(hd.tong_tien) AS doanhThu " +
            "FROM hoa_don hd " +
            "WHERE hd.trang_thai = :trangThai " +
            "GROUP BY CONVERT(date, hd.ngay_ban) " +
            "ORDER BY ngay", nativeQuery = true)
    List<Object[]> findDoanhThuTheoNgay(@Param("trangThai") String trangThai);

    // Doanh thu theo tháng (Native Query)
    @Query(value = "SELECT YEAR(hd.ngay_ban) AS nam, MONTH(hd.ngay_ban) AS thang, SUM(hd.tong_tien) AS doanhThu " +
            "FROM hoa_don hd " +
            "WHERE hd.trang_thai = :trangThai " +
            "GROUP BY YEAR(hd.ngay_ban), MONTH(hd.ngay_ban) " +
            "ORDER BY nam, thang", nativeQuery = true)
    List<Object[]> findDoanhThuTheoThang(@Param("trangThai") String trangThai);

    // Doanh thu theo năm (Native Query)
    @Query(value = "SELECT YEAR(hd.ngay_ban) AS nam, SUM(hd.tong_tien) AS doanhThu " +
            "FROM hoa_don hd " +
            "WHERE hd.trang_thai = :trangThai " +
            "GROUP BY YEAR(hd.ngay_ban) " +
            "ORDER BY nam", nativeQuery = true)
    List<Object[]> findDoanhThuTheoNam(@Param("trangThai") String trangThai);

    // Lợi nhuận tổng cộng theo trạng thái (Original single status)
    @Query("SELECT SUM((ctsp.giaBan - ctsp.giaNhap) * cthd.soLuong) " +
            "FROM ChiTietHoaDon cthd " +
            "JOIN cthd.chiTietSanPham ctsp " +
            "JOIN cthd.hoaDon hd " +
            "WHERE hd.trangThai = :trangThai")
    BigDecimal findTongLoiNhuan(@Param("trangThai") String trangThai);

    @Query("SELECT h FROM HoaDon h WHERE h.trangThai = :trangThai")
    List<HoaDon> findHoaDonsByTrangThai(@Param("trangThai") String trangThai);

    @Query("SELECT h FROM HoaDon h WHERE h.khachHang.khachHangId IS NULL ORDER BY h.hoaDonId DESC")
    List<HoaDon> findHoaDonsDesc();

    @Query("SELECT HD FROM HoaDon HD WHERE HD.hoaDonId =:id ")
    HoaDon finid(Integer id);

    @Query("SELECT CASE WHEN COUNT(hd) > 0 THEN true ELSE false END " +
            "FROM HoaDon hd " +
            "WHERE hd.maGiamGia.maGiamGiaId = :voucherId " +
            "AND hd.khachHang.khachHangId = :khachHangId")
    boolean checkmavoucher(
            @Param("voucherId") int voucher,
            @Param("khachHangId") int khachHang
    );

    // NOTE: findTongDoanhThuByDateRange still only takes one status.
    // If you need this to include multiple statuses, update it like findTongDoanhThuTongThe.
    @Query("SELECT SUM(h.tongTien) FROM HoaDon h WHERE h.trangThai IN (:statuses) AND (:startDate IS NULL OR h.ngayBan >= :startDate) AND (:endDate IS NULL OR h.ngayBan <= :endDate)")
    BigDecimal findTongDoanhThuByDateRange(@Param("statuses") List<String> statuses,
                                           @Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime  endDate);


    // NOTE: findTongLoiNhuanByDateRange still only takes one status.
    // If you need this to include multiple statuses, update it like findTongDoanhThuTongThe.
    @Query("SELECT SUM(cthd.gia * cthd.soLuong) - SUM(ctsp.giaNhap * cthd.soLuong) " +
            "FROM HoaDon h " +
            "JOIN h.chiTietHoaDons cthd " +
            "JOIN cthd.chiTietSanPham ctsp " +
            "WHERE h.trangThai IN (:statuses) " +
            "AND (:startDate IS NULL OR h.ngayBan >= :startDate) " +
            "AND (:endDate IS NULL OR h.ngayBan <= :endDate)")
    BigDecimal findTongLoiNhuanByDateRange(@Param("statuses") List<String> statuses,
                                           @Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);


    /**
     * Tính tổng doanh thu TỔNG THỂ cho nhiều trạng thái.
     * Modified to accept a List of statuses.
     */
    @Query("SELECT SUM(h.tongTien) FROM HoaDon h WHERE h.trangThai IN (:trangThaiList)")
    BigDecimal findTongDoanhThuTongThe(@Param("trangThaiList") List<String> trangThaiList);


    /**
     * Đếm tổng số lượng hóa đơn TỔNG THỂ cho nhiều trạng thái.
     * Modified to accept a List of statuses.
     */
    @Query("SELECT COUNT(h.hoaDonId) FROM HoaDon h WHERE h.trangThai IN (:trangThaiList)")
    Long countHoaDonTongThe(@Param("trangThaiList") List<String> trangThaiList);

    // Added: Find total revenue by channel and statuses
    @Query("SELECT SUM(h.tongTien) FROM HoaDon h WHERE h.kenhBanHang = :kenhBanHang AND h.trangThai IN (:trangThaiList)")
    BigDecimal findTongDoanhThuByKenhAndTrangThaiIn(
            @Param("kenhBanHang") String kenhBanHang,
            @Param("trangThaiList") List<String> trangThaiList);

    // Added: Count total orders by channel and statuses
    @Query("SELECT COUNT(h.hoaDonId) FROM HoaDon h WHERE h.kenhBanHang = :kenhBanHang AND h.trangThai IN (:trangThaiList)")
    Long countHoaDonByKenhAndTrangThaiIn(
            @Param("kenhBanHang") String kenhBanHang,
            @Param("trangThaiList") List<String> trangThaiList);


    @Query("SELECT MONTH(hd.ngayTaoHoaDon) as month, SUM(hd.tongTien) as monthlyRevenue " +
            "FROM HoaDon hd " +
            "WHERE hd.trangThai IN (:trangThaiList) " + // <-- THAY ĐỔI: Sử dụng IN thay vì =
            "AND YEAR(hd.ngayTaoHoaDon) = :year " +
            "GROUP BY MONTH(hd.ngayTaoHoaDon) " +
            "ORDER BY MONTH(hd.ngayTaoHoaDon) ASC")
    List<Object[]> findDoanhThuTungThangTrongNam(
            @Param("year") Integer year,
            @Param("trangThaiList") List<String> trangThaiList // <-- THAY ĐỔI: Tham số là List<String>
    );

    /** Đếm số lượng hóa đơn theo từng trạng thái trong khoảng ngày tùy chọn (cho biểu đồ tròn). */
    @Query("SELECT h.trangThai, COUNT(h.hoaDonId) " +
            "FROM HoaDon h " +
            "WHERE (:startDate IS NULL OR h.ngayTaoHoaDon >= :startDate) " + // REMOVED CAST
            "AND (:endDate IS NULL OR h.ngayTaoHoaDon <= :endDate) " +     // REMOVED CAST
            "GROUP BY h.trangThai " +
            "ORDER BY h.trangThai")
    List<Object[]> countByTrangThaiAndDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);




}