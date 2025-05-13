package com.sd31.sunday.repository;

import com.sd31.sunday.model.KieuDang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface KieuDangRepository extends JpaRepository<KieuDang, Integer> {
    Optional<KieuDang> findByTenKieuDang(String tenKieuDang);

    @Query("SELECT kd FROM KieuDang kd WHERE (:trangThai is null or kd.trangThai = :trangThai)")
    List<KieuDang> findByTrangThai(@Param("trangThai") String trangThai);

    @Query("SELECT COUNT(kd) > 0 FROM KieuDang kd WHERE LOWER(kd.tenKieuDang) = LOWER(:tenKieuDang) AND kd.kieuDangId <> :kieuDangId")
    boolean existsByTenKieuDangIgnoreCaseAndKieuDangIdNot(@Param("tenKieuDang") String tenKieuDang, @Param("kieuDangId") Integer kieuDangId);

    Optional<KieuDang> findByTenKieuDangIgnoreCase(String tenKieuDang);
}
