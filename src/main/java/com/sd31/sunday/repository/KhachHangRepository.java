package com.sd31.sunday.repository;

import com.sd31.sunday.model.KhachHang;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface KhachHangRepository extends JpaRepository<KhachHang, Integer> {
    Optional<KhachHang> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsBySoDienThoai(String soDienThoai);
    Optional<KhachHang> findFirstByOrderByMaKhachHangDesc();

    boolean existsByMaKhachHang(String maKhachHang);
    // TODO: Thêm method cho searchKhachHang (ví dụ: tìm kiếm theo tên, email, sdt - sử dụng @Query hoặc JPA naming conventions)
    // Ví dụ (JPA naming conventions):
    Page<KhachHang> findByTenContainingIgnoreCaseOrEmailContainingIgnoreCaseOrSoDienThoaiContainingIgnoreCase(
            String ten, String email, String soDienThoai, Pageable pageable);

    // TODO: Thêm method cho filterKhachHangByTrangThai
    Page<KhachHang> findByTrangThai(String trangThai, Pageable pageable);

    @Query(value = "SELECT TOP 5 kh.khach_hang_id, kh.ma_khach_hang, kh.ten, kh.email, kh.so_dien_thoai, " +
            "SUM(hd.tong_tien) as tongChiTieu, COUNT(hd.hoa_don_id) as soHoaDon " +
            "FROM hoa_don hd " +
            "JOIN khach_hang kh ON hd.khach_hang_id = kh.khach_hang_id " +
            "WHERE hd.trang_thai = :trangThai " +
            "GROUP BY kh.khach_hang_id, kh.ma_khach_hang, kh.ten, kh.email, kh.so_dien_thoai " +
            "ORDER BY tongChiTieu DESC", nativeQuery = true)
    List<Object[]> findTop5KhachHangMuaNhieu(@Param("trangThai") String trangThai);

//
@Query("SELECT kh from KhachHang  kh where kh.soDienThoai is not null")
List<KhachHang> timkhachhang();
KhachHang findBySoDienThoai(String sdt);
//
// Phương thức JPQL mới để lấy top khách hàng (đã có)
@Query("SELECT h.khachHang, SUM(h.tongTien) AS totalSpent " +
        "FROM HoaDon h " +
        "WHERE h.khachHang IS NOT NULL " + // Exclude guest checkouts if any
        "AND h.trangThai IN (:statuses) " + // <-- Use IN here
        "AND (:startDate IS NULL OR h.ngayTaoHoaDon >= :startDate) " +
        "AND (:endDate IS NULL OR h.ngayTaoHoaDon <= :endDate) " +
        "GROUP BY h.khachHang " +
        "ORDER BY totalSpent DESC")
List<Object[]> findTopKhachHangMuaNhieuNhat(
        @Param("statuses") List<String> statuses, // <-- Change parameter type
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable);




}
