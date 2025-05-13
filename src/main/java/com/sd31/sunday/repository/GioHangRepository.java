package com.sd31.sunday.repository;

import com.sd31.sunday.model.GioHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GioHangRepository extends JpaRepository<GioHang,Integer> {
    List<GioHang> findByTaiKhoanId(Integer taiKhoanId);
    int countByTaiKhoanId(Integer taiKhoanId);

    @Query("SELECT gh FROM GioHang gh WHERE gh.taiKhoanId = :taiKhoanId AND gh.chiTietSanPham.id = :chiTietSanPhamId")
    GioHang findByTaiKhoanIdAndChiTietSanPham_Id(@Param("taiKhoanId") Integer taiKhoanId, @Param("chiTietSanPhamId") Integer chiTietSanPhamId);

    Optional<GioHang> findById(Integer id);
}
