package com.sd31.sunday.repository;

import com.sd31.sunday.model.ChiTietHoaDon;
import com.sd31.sunday.model.ChiTietSanPham;
import com.sd31.sunday.model.HoaDon;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ChiTietHoaDonRepository extends JpaRepository<ChiTietHoaDon, Integer> {
    // Bạn có thể thêm các phương thức truy vấn tùy chỉnh nếu cần
    @Query("SELECT h FROM ChiTietHoaDon h WHERE h.hoaDon.hoaDonId = :hoaDonId" )
    List<ChiTietHoaDon> findhdct(Integer hoaDonId);

    List<ChiTietHoaDon> findByHoaDon_HoaDonId(Integer hoaDonId);
    ChiTietHoaDon findByHoaDon_HoaDonIdAndChiTietSanPham_ChiTietSanPhamId(Integer hoaDonId, Integer chiTietSanPhamId);
//

    @Query("SELECT h FROM ChiTietHoaDon h WHERE h.hoaDon.hoaDonId = :hoaDonId AND h.chiTietSanPham.chiTietSanPhamId = :sanPhamId")
    ChiTietHoaDon findByHoaDonIdAndSanPhamId(Integer hoaDonId, Integer sanPhamId);
    @Query("SELECT COALESCE(SUM(hdct.tongTien), 0) FROM ChiTietHoaDon hdct WHERE hdct.hoaDon.hoaDonId = :hoaDonId")
    BigDecimal TongTienByHoaDonId(int hoaDonId);


    //
    @Query("SELECT cthd FROM ChiTietHoaDon cthd WHERE cthd.hoaDon.hoaDonId = :id")
    List<ChiTietHoaDon> findhoadonct(@Param("id") int id);

    List<ChiTietHoaDon> findByHoaDon(HoaDon hoaDon);

    @Query("SELECT SUM(cthd.soLuong) FROM ChiTietHoaDon cthd " +
            "WHERE cthd.chiTietSanPham.chiTietSanPhamId = :chiTietSanPhamId " +
            "AND cthd.hoaDon.trangThai IN :statuses")
    Integer findReservedQuantityByChiTietSanPhamIdAndStatuses(
            @Param("chiTietSanPhamId") Integer chiTietSanPhamId,
            @Param("statuses") List<String> statuses
    );

    @Modifying // Necessary for UPDATE queries
    @Query("UPDATE ChiTietHoaDon cthd SET cthd.gia = :newPrice, cthd.tongTien = (:newPrice * cthd.soLuong) " +
            "WHERE cthd.chiTietSanPham.chiTietSanPhamId = :ctspId " +
            "AND cthd.hoaDon.trangThai = :pendingStatus")
    int updatePriceForPendingOrderItems(
            @Param("ctspId") Integer chiTietSanPhamId,
            @Param("newPrice") BigDecimal newPrice,
            @Param("pendingStatus") String pendingStatus
    );

    @Query("SELECT cthd.chiTietSanPham.sanPham.sanPhamId, SUM(cthd.soLuong) as totalQuantity " +
            "FROM ChiTietHoaDon cthd " +
            "JOIN cthd.hoaDon hd " +
            "WHERE hd.trangThai = :status " + // Filter by completed status
            "AND FUNCTION('YEAR', hd.ngayBan) = :year " + // Filter by year
            "AND FUNCTION('MONTH', hd.ngayBan) = :month " + // Filter by month
            "GROUP BY cthd.chiTietSanPham.sanPham.sanPhamId " + // Group by SanPham ID
            "ORDER BY totalQuantity DESC") // Order by quantity sold descending
    List<Object[]> findTopSellingProductIdsAndQuantityByMonth(
            @Param("status") String status,
            @Param("year") int year,
            @Param("month") int month,
            Pageable pageable // Add Pageable parameter to limit results
    );

}