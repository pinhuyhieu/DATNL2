package com.sd31.sunday.repository;

import com.sd31.sunday.model.ThuongHieu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ThuongHieuRepository extends JpaRepository<ThuongHieu, Integer> {
    Optional<ThuongHieu> findByTenThuongHieu(String tenThuongHieu);

    @Query("SELECT th FROM ThuongHieu th WHERE (:trangThai is null or th.trangThai = :trangThai)")
    List<ThuongHieu> findByTrangThai(@Param("trangThai") String trangThai);

    @Query("SELECT COUNT(th) > 0 FROM ThuongHieu th WHERE LOWER(th.tenThuongHieu) = LOWER(:tenThuongHieu) AND th.thuongHieuId <> :thuongHieuId")
    boolean existsByTenThuongHieuIgnoreCaseAndThuongHieuIdNot(@Param("tenThuongHieu") String tenThuongHieu, @Param("thuongHieuId") Integer thuongHieuId);

    Optional<ThuongHieu> findByTenThuongHieuIgnoreCase(String tenThuongHieu);
}