package com.sd31.sunday.repository;

import com.sd31.sunday.model.MauSac;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MauSacRepository extends JpaRepository<MauSac, Integer> {
    Optional<MauSac> findByTenMauSac(String tenMauSac);

    @Query("SELECT ms FROM MauSac ms WHERE (:trangThai is null or ms.trangThai = :trangThai)")
    List<MauSac> findByTrangThai(@Param("trangThai") String trangThai);

    @Query("SELECT COUNT(ms) > 0 FROM MauSac ms WHERE LOWER(ms.tenMauSac) = LOWER(:tenMauSac) AND ms.mauSacId <> :mauSacId")
    boolean existsByTenMauSacIgnoreCaseAndMauSacIdNot(@Param("tenMauSac") String tenMauSac, @Param("mauSacId") Integer mauSacId);

    Optional<MauSac> findByTenMauSacIgnoreCase(String tenMauSac);
}
