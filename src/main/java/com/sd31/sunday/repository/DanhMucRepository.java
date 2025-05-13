package com.sd31.sunday.repository;

import com.sd31.sunday.model.DanhMuc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DanhMucRepository extends JpaRepository<DanhMuc, Integer> {
    Optional<DanhMuc> findByTenDanhMuc(String tenDanhMuc);

    @Query("SELECT dm FROM DanhMuc dm WHERE (:trangThai is null or dm.trangThai = :trangThai)")
    List<DanhMuc> findByTrangThai(@Param("trangThai") String trangThai);

    @Query("SELECT COUNT(dm) > 0 FROM DanhMuc dm WHERE LOWER(dm.tenDanhMuc) = LOWER(:tenDanhMuc) AND dm.danhMucId <> :danhMucId")
    boolean existsByTenDanhMucIgnoreCaseAndDanhMucIdNot(@Param("tenDanhMuc") String tenDanhMuc, @Param("danhMucId") Integer danhMucId);

    Optional<DanhMuc> findByTenDanhMucIgnoreCase(String tenDanhMuc);

    @Query("SELECT dm FROM DanhMuc dm WHERE LOWER(dm.tenDanhMuc) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<DanhMuc> findByTenDanhMucContainingIgnoreCase(@Param("keyword") String keyword);
}
