package com.sd31.sunday.repository;

import com.sd31.sunday.model.ChiTietSanPham;
import com.sd31.sunday.model.SanPham;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SanPhamRepository extends JpaRepository<SanPham, Integer> {

    @Query("SELECT DISTINCT sp FROM SanPham sp LEFT JOIN FETCH sp.chiTietSanPhams " +
            "WHERE sp.trangThai = :trangThai AND sp.chiTietSanPhams IS NOT EMPTY")
    List<SanPham> findAllActiveWithDetails(@Param("trangThai") String trangThai);

    @Query("SELECT sp FROM SanPham sp " +
            "LEFT JOIN FETCH sp.chiTietSanPhams " +
            "LEFT JOIN FETCH sp.hinhAnhs " +
            "LEFT JOIN FETCH sp.danhMuc " +
            "LEFT JOIN FETCH sp.thuongHieu " +
            "LEFT JOIN FETCH sp.kieuDang " +
            "LEFT JOIN FETCH sp.chatLieu " +
            "WHERE sp.sanPhamId = :id")
    SanPham findByIdWithDetails(@Param("id") Integer id);

    boolean existsByMaSanPham(String maSanPham);

    boolean existsByTenSanPham(String tenSanPham);

    @Query("SELECT sp FROM SanPham sp WHERE " +
            "LOWER(sp.maSanPham) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(sp.tenSanPham) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(sp.moTa) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<SanPham> searchByKeyword(@Param("keyword") String keyword);

    @Query("SELECT sp FROM SanPham sp WHERE " +
            "(LOWER(sp.maSanPham) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(sp.tenSanPham) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(sp.moTa) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "sp.trangThai = :trangThai")
        // Sử dụng :trangThai parameter
    List<SanPham> searchByKeywordHome(@Param("keyword") String keyword, @Param("trangThai") String trangThai);

    @Query("SELECT sp FROM SanPham sp WHERE " +
            "(:danhMucId IS NULL OR sp.danhMuc.danhMucId = :danhMucId) AND " +
            "(:thuongHieuId IS NULL OR sp.thuongHieu.thuongHieuId = :thuongHieuId) AND " +
            "(:chatLieuId IS NULL OR sp.chatLieu.chatLieuId = :chatLieuId) AND " +
            "(:kieuDangId IS NULL OR sp.kieuDang.kieuDangId = :kieuDangId) AND " +
            "(:trangThai IS NULL OR sp.trangThai = :trangThai)")
    List<SanPham> filterProducts(
            @Param("danhMucId") Integer danhMucId,
            @Param("thuongHieuId") Integer thuongHieuId,
            @Param("chatLieuId") Integer chatLieuId,
            @Param("kieuDangId") Integer kieuDangId,
            @Param("trangThai") String trangThai);

    Page<SanPham> findAll(Specification<SanPham> spec, Pageable pageable); // Add this line

    // Top 5 sản phẩm bán chạy
    @Query("SELECT sp, SUM(cthd.soLuong) " +
            "FROM ChiTietHoaDon cthd " +
            "JOIN cthd.chiTietSanPham ctsp " +
            "JOIN ctsp.sanPham sp " +
            "JOIN cthd.hoaDon hd " +
            "WHERE hd.trangThai = :trangThai " +
            "GROUP BY sp " +
            "ORDER BY SUM(cthd.soLuong) DESC")
    List<Object[]> findTop5SanPhamBanChay(String trangThai);

    // Top 5 sản phẩm tồn kho nhiều nhất
    @Query("SELECT sp, SUM(ctsp.soLuongTon) " + // Loại bỏ alias 'as tonKho'
            "FROM ChiTietSanPham ctsp " +
            "JOIN ctsp.sanPham sp " +
            "GROUP BY sp " +
            "ORDER BY SUM(ctsp.soLuongTon) DESC")
    List<Object[]> findTop5SanPhamTonKho();


    @Query("SELECT ctsp FROM ChiTietSanPham ctsp " +
            "JOIN FETCH ctsp.sanPham sp " +
            "JOIN FETCH ctsp.mauSac ms " +
            "JOIN FETCH ctsp.kichCo kc " +
            "WHERE ctsp.soLuongTon <= :threshold " +
            "AND ctsp.soLuongTon >= 0 " +
            "AND ctsp.trangThai = :trangThaiCTSP " +
            "ORDER BY ctsp.soLuongTon ASC, sp.tenSanPham ASC")
    List<ChiTietSanPham> findSanPhamSapHetHang(
            @Param("threshold") Integer threshold,
            @Param("trangThaiCTSP") String trangThaiCTSP,
            Pageable pageable);

    @Query("SELECT cthd.chiTietSanPham.sanPham, SUM(cthd.soLuong) AS totalSoLuong " +
            "FROM ChiTietHoaDon cthd JOIN cthd.hoaDon hd " +
            "WHERE hd.trangThai IN (:statuses) " + // <-- Use IN here
            "AND (:startDate IS NULL OR hd.ngayTaoHoaDon >= :startDate) " +
            "AND (:endDate IS NULL OR hd.ngayTaoHoaDon <= :endDate) " +
            "GROUP BY cthd.chiTietSanPham.sanPham " +
            "ORDER BY totalSoLuong DESC")
    List<Object[]> findTopSanPhamBanChayByDateRange(
            @Param("statuses") List<String> statuses, // <-- Change parameter type
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);


    @Query("SELECT COUNT(sp) FROM SanPham sp WHERE sp.trangThai = :trangThai")
    long countActiveSanPham(@Param("trangThai") String trangThai);

    @Query("SELECT DISTINCT sp FROM SanPham sp " +
            "LEFT JOIN FETCH sp.hinhAnhs " +
            "LEFT JOIN FETCH sp.chiTietSanPhams ctsp " +
            "WHERE sp.sanPhamId IN :ids")
    List<SanPham> findAllByIdInOrderByIds(@Param("ids") List<Integer> ids);


}