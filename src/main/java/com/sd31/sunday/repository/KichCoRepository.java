package com.sd31.sunday.repository;

import com.sd31.sunday.model.KichCo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface KichCoRepository extends JpaRepository<KichCo, Integer> {
    Optional<KichCo> findByTenKichCo(String tenKichCo);

    @Query("SELECT kc FROM KichCo kc WHERE (:trangThai is null or kc.trangThai = :trangThai)")
    List<KichCo> findByTrangThai(@Param("trangThai") String trangThai);

    @Query("SELECT COUNT(kc) > 0 FROM KichCo kc WHERE LOWER(kc.tenKichCo) = LOWER(:tenKichCo) AND kc.kichCoId <> :kichCoId")
    boolean existsByTenKichCoIgnoreCaseAndKichCoIdNot(@Param("tenKichCo") String tenKichCo, @Param("kichCoId") Integer kichCoId);

    Optional<KichCo> findByTenKichCoIgnoreCase(String tenKichCo);
}
