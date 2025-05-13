package com.sd31.sunday.repository;

import com.sd31.sunday.model.LichSuTrangThai;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LichSuTrangThaiRepository  extends JpaRepository<LichSuTrangThai, Integer> {
    List<LichSuTrangThai> findByHoaDon_HoaDonId(Integer hoaDonId);

}
