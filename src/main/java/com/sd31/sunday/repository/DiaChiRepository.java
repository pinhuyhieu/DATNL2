package com.sd31.sunday.repository;

import com.sd31.sunday.model.DiaChi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiaChiRepository extends JpaRepository<DiaChi, Integer> {

    // Phương thức để tìm kiếm địa chỉ theo khachHangId
    List<DiaChi> findByKhachHangKhachHangId(Integer khachHangId);

    // Các phương thức truy vấn khác có thể được thêm vào nếu cần
    // Ví dụ:
    // Page<DiaChi> findByThanhPhoContainingIgnoreCase(String thanhPho, Pageable pageable);
}