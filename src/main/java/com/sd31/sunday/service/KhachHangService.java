package com.sd31.sunday.service;

import com.sd31.sunday.model.KhachHang;
import com.sd31.sunday.repository.KhachHangRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

@Service
public class KhachHangService {
    private static final String EMAIL_PATTERN =
            "^[a-zA-Z0-9_+&*-]+(?:\\."+
                    "[a-zA-Z0-9_+&*-]+)*@" +
                    "(?:[a-zA-Z0-9-]+\\.)+[a-z" +
                    "A-Z]{2,7}$";
    private static final Pattern pattern = Pattern.compile(EMAIL_PATTERN);

    @Autowired
    private KhachHangRepository khachHangRepository;

    public KhachHang login(String email, String password) {
        Optional<KhachHang> khachHangOptional = khachHangRepository.findByEmail(email);
        if (khachHangOptional.isPresent()) {
            KhachHang khachHang = khachHangOptional.get();
            if (password.equals(khachHang.getMatKhau())) {
                return khachHang;
            }
        }
        return null;
    }


    public String dangKyKhachHang(KhachHang khachHang, BindingResult result) {

        if (khachHang.getTen() == null || khachHang.getTen().trim().isEmpty()) {
            result.addError(new FieldError("khachHang", "ten", "Tên không được để trống"));
        }

        if (khachHang.getEmail() == null || khachHang.getEmail().trim().isEmpty()) {
            result.addError(new FieldError("khachHang", "email", "Email không được để trống"));
        } else if (!pattern.matcher(khachHang.getEmail()).matches()) {
            result.addError(new FieldError("khachHang", "email", "Email không đúng định dạng"));
        } else if (khachHangRepository.existsByEmail(khachHang.getEmail())) {
            result.addError(new FieldError("khachHang", "email", "Email này đã được sử dụng."));

        }

        if (khachHang.getSoDienThoai() == null || khachHang.getSoDienThoai().trim().isEmpty()) {
            result.addError(new FieldError("khachHang", "soDienThoai", "Số điện thoại không được để trống"));
        }else if(!Pattern.matches("^\\d{10}$",khachHang.getSoDienThoai())){
            result.addError(new FieldError("khachHang","soDienThoai","Số điện thoại phải có 10 chữ số"));
        } else if (khachHangRepository.existsBySoDienThoai(khachHang.getSoDienThoai())){
            result.addError(new FieldError("khachHang","soDienThoai","Số điện thoại này đã được sử dụng."));
        }

        if (khachHang.getMatKhau() == null || khachHang.getMatKhau().trim().isEmpty()) {
            result.addError(new FieldError("khachHang", "matKhau", "Mật khẩu không được để trống"));
        }else if (khachHang.getMatKhau().trim().length() < 6){
            result.addError(new FieldError("khachHang", "matKhau", "Mật khẩu phải có ít nhất 6 ký tự"));
        }
        if (result.hasErrors()){
            return null;
        }

        // Tạo mã khách hàng
        String maKhachHang = generateMaKhachHang();
        khachHang.setMaKhachHang(maKhachHang);
        khachHang.setTrangThai("Hoạt động");

        khachHangRepository.save(khachHang);
        return "Đăng ký thành công";
    }


    private String generateMaKhachHang() {
        // Get current time in milliseconds
        long timeMillis = System.currentTimeMillis();

        // Get a random long
        long randomLong = ThreadLocalRandom.current().nextLong();

        // Combine time and random parts (e.g., using XOR to mix bits)
        long combined = timeMillis ^ randomLong;

        // Convert the combined long to a hexadecimal string
        // Long.toHexString(long) handles negative numbers by representing the bits
        String hexString = Long.toHexString(combined);

        // Prepend the prefix "KH"
        String maKhachHang = "KH" + hexString.toUpperCase(); // Use uppercase for consistency

        // Note: While statistically very improbable, a collision is theoretically possible
        // if two registrations happen at the *exact* same millisecond with the *exact*
        // same random long generated (across threads/instances). The database's
        // UNIQUE constraint is the final guarantee of uniqueness, which is why
        // catching DataIntegrityViolationException is important.

        return maKhachHang;
    }


    public Page<KhachHang> getAllKhachHang(Pageable pageable) {
        return khachHangRepository.findAll(pageable);
    }

    public KhachHang getKhachHangById(Integer id) {
        return khachHangRepository.findById(id).orElse(null);
    }

    // Đã triển khai - không còn là TODO
    public Page<KhachHang> searchKhachHang(String keyword, Pageable pageable) {
        return khachHangRepository.findByTenContainingIgnoreCaseOrEmailContainingIgnoreCaseOrSoDienThoaiContainingIgnoreCase(
                keyword, keyword, keyword, pageable);
    }

    // Đã triển khai - không còn là TODO
    public Page<KhachHang> filterKhachHangByTrangThai(String trangThai, Pageable pageable) {
        return khachHangRepository.findByTrangThai(trangThai, pageable);
    }
}