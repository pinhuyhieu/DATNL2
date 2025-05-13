package com.sd31.sunday.service;

import com.sd31.sunday.model.KhachHang;
import com.sd31.sunday.repository.KHRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class KHService {

    @Autowired
    private KHRepository khachHangRepository;

    public KhachHang findById(Integer id) {
        return khachHangRepository.findById(id).orElse(null);
    }

    public KhachHang findByEmail(String email) {
        return khachHangRepository.findByEmail(email).orElse(null);
    }

    public boolean updateSoDienThoai(Integer khachHangId, String soDienThoai) {
        // Kiểm tra số điện thoại đã tồn tại chưa
        if (khachHangRepository.existsBySoDienThoai(soDienThoai)) {
            KhachHang existing = khachHangRepository.findBySoDienThoai(soDienThoai).orElse(null);
            if (existing != null && !existing.getKhachHangId().equals(khachHangId)) {
                return false; // Số điện thoại đã được sử dụng bởi tài khoản khác
            }
        }

        KhachHang khachHang = khachHangRepository.findById(khachHangId).orElse(null);
        if (khachHang != null) {
            khachHang.setSoDienThoai(soDienThoai);
            khachHangRepository.save(khachHang);
            return true;
        }
        return false;
    }

    public boolean changePassword(Integer khachHangId, String oldPassword, String newPassword) {
        KhachHang khachHang = khachHangRepository.findById(khachHangId).orElse(null);
        if (khachHang != null) {
            // Kiểm tra mật khẩu cũ (plain text)
            if (oldPassword.equals(khachHang.getMatKhau())) {
                // Cập nhật mật khẩu mới (plain text)
                khachHang.setMatKhau(newPassword);
                khachHangRepository.save(khachHang);
                return true;
            }
        }
        return false;
    }

    public KhachHang save(KhachHang khachHang) {
        return khachHangRepository.save(khachHang);
    }
}