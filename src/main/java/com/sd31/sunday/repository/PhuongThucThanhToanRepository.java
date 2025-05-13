package com.sd31.sunday.repository;

import com.sd31.sunday.model.HoaDon;
import com.sd31.sunday.model.PhuongThucThanhToan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhuongThucThanhToanRepository extends JpaRepository<PhuongThucThanhToan, Integer> {
    List<PhuongThucThanhToan> findByHoaDon(HoaDon hoaDon);
}