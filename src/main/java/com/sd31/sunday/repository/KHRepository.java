package com.sd31.sunday.repository;

import com.sd31.sunday.model.KhachHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KHRepository extends JpaRepository<KhachHang, Integer> {
    Optional<KhachHang> findByEmail(String email);
    Optional<KhachHang> findBySoDienThoai(String soDienThoai);
    boolean existsByEmail(String email);
    boolean existsBySoDienThoai(String soDienThoai);
}