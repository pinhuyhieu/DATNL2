package com.sd31.sunday.repository;

import com.sd31.sunday.model.MaGiamGia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MaGiamGiaRepository extends JpaRepository<MaGiamGia, Integer>{
    Optional<MaGiamGia> findByTenMaGiamGia(String ma);

    // Add this method to find coupons available now with quantity and status
    List<MaGiamGia> findByNgayBatDauBeforeAndNgayKetThucAfterAndSoLuongGreaterThanAndTrangThai(
            LocalDateTime dateBefore, LocalDateTime dateAfter, Integer minSoLuong, String trangThai
    );

    List<MaGiamGia> findByNgayKetThucAfterAndSoLuongGreaterThanAndTrangThai(
            LocalDateTime now,
            int minQuantity,
            String trangThai
    );
    List<MaGiamGia> findByTenMaGiamGiaContainingIgnoreCase(String tenMaGiamGia);
    List<MaGiamGia> findByTrangThai(String trangThai);
    List<MaGiamGia> findByLoaiGiamGia(String loaiGiamGia);
    List<MaGiamGia> findByTrangThaiAndLoaiGiamGia(String trangThai, String loaiGiamGia);
    boolean existsByTenMaGiamGia(String tenMaGiamGia);

}

