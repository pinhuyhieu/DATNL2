package com.sd31.sunday.repository;

import com.sd31.sunday.model.HinhAnh;
import com.sd31.sunday.model.SanPham;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HinhAnhRepository extends JpaRepository<HinhAnh, Integer> {
    List<HinhAnh> findBySanPham(SanPham sanPham);
}