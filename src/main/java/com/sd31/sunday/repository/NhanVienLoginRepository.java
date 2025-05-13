package com.sd31.sunday.repository;

import com.sd31.sunday.model.NhanVien;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NhanVienLoginRepository extends JpaRepository<NhanVien, Integer> {
    Optional<NhanVien> findBySoDienThoai(String soDienThoai);
    boolean existsBySoDienThoai(String soDienThoai);
    Optional<NhanVien> findFirstByOrderByMaNhanVienDesc();
    Optional<NhanVien> findFirstByOrderByNhanVienIdDesc();
}
