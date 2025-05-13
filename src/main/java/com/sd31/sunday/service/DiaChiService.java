package com.sd31.sunday.service;

import com.sd31.sunday.model.DiaChi;
import com.sd31.sunday.repository.DiaChiRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DiaChiService {

    @Autowired
    private DiaChiRepository diaChiRepository;

    public DiaChi getDiaChiById(Integer id) {
        return diaChiRepository.findById(id).orElse(null);
    }

    public List<DiaChi> getDiaChiByKhachHangId(Integer khachHangId) {
        return diaChiRepository.findByKhachHangKhachHangId(khachHangId);
    }

    // Các phương thức CRUD khác có thể được thêm vào nếu cần
    // Ví dụ:
    // public DiaChi createDiaChi(DiaChi diaChi) { ... }
    // public DiaChi updateDiaChi(DiaChi diaChi) { ... }
    // public void deleteDiaChi(Integer id) { ... }
}