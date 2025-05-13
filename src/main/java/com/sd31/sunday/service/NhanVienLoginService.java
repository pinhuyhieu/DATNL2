package com.sd31.sunday.service;

import com.sd31.sunday.model.NhanVien;
import com.sd31.sunday.repository.NhanVienLoginRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class NhanVienLoginService {

    private static final String PHONE_PATTERN = "^0[0-9]{9}$";
    private static final Pattern pattern = Pattern.compile(PHONE_PATTERN);

    @Autowired
    private NhanVienLoginRepository nhanVienRepository;

    // Đăng nhập với số điện thoại và mật khẩu
    public NhanVien login(String soDienThoai, String password) {
        Optional<NhanVien> nhanVienOptional = nhanVienRepository.findBySoDienThoai(soDienThoai);
        if (nhanVienOptional.isPresent()) {
            NhanVien nhanVien = nhanVienOptional.get();
            if (password.equals(nhanVien.getMatKhau())) {
                return nhanVien;
            }
        }
        return null;
    }

    // Đăng ký nhân viên
    public String dangKyNhanVien(NhanVien nhanVien, BindingResult result) {
        // 1. Validation Tên
        if (isEmptyString(nhanVien.getTen())) { // Kiểm tra tên rỗng hoặc chỉ chứa khoảng trắng
            result.addError(new FieldError("nhanVien", "ten", "Tên không được để trống"));
        }

        // 2. Validation Số điện thoại
        String soDienThoai = trimInput(nhanVien.getSoDienThoai()); // Loại bỏ khoảng trắng đầu cuối
        if (isEmptyString(soDienThoai)) { // Kiểm tra số điện thoại rỗng
            result.addError(new FieldError("nhanVien", "soDienThoai", "Số điện thoại không được để trống"));
        } else if (!pattern.matcher(soDienThoai).matches()) { // Kiểm tra định dạng số điện thoại
            result.addError(new FieldError("nhanVien", "soDienThoai", "Số điện thoại phải có 10 chữ số và bắt đầu bằng 0"));
        } else if (nhanVienRepository.existsBySoDienThoai(soDienThoai)) { // Kiểm tra số điện thoại đã tồn tại
            result.addError(new FieldError("nhanVien", "soDienThoai", "Số điện thoại này đã được sử dụng."));
        }

        // 3. Validation Mật khẩu
        String matKhau = trimInput(nhanVien.getMatKhau()); // Loại bỏ khoảng trắng đầu cuối
        if (isEmptyString(matKhau)) { // Kiểm tra mật khẩu rỗng
            result.addError(new FieldError("nhanVien", "matKhau", "Mật khẩu không được để trống"));
        } else if (matKhau.length() < 6 || matKhau.length() > 20) { // Kiểm tra độ dài mật khẩu
            result.addError(new FieldError("nhanVien", "matKhau", "Mật khẩu phải từ 6 đến 20 ký tự"));
        } else if (!isValidPasswordFormat(matKhau)) { // Kiểm tra định dạng mật khẩu mạnh
            result.addError(new FieldError("nhanVien", "matKhau", "Mật khẩu phải chứa ít nhất 1 chữ hoa, 1 chữ thường và 1 số"));
        }

        // 4. Xử lý lỗi Validation
        if (result.hasErrors()) {
            result.getAllErrors().forEach(error -> {
                System.out.println("Lỗi: " + error.getDefaultMessage()); // In lỗi ra console (có thể tùy chỉnh xử lý lỗi khác)
            });
            return null; // Trả về null hoặc thông báo lỗi cho Controller xử lý
        }

        // 5. Tạo và lưu Nhân viên mới nếu validation thành công
        String maNhanVien = generateMaNhanVien(); // Tạo mã nhân viên tự động
        nhanVien.setMaNhanVien(maNhanVien);
        nhanVien.setTrangThai("Active");  // Set trạng thái mặc định

        nhanVienRepository.save(nhanVien); // Lưu nhân viên vào database
        return "Đăng ký thành công"; // Trả về thông báo thành công
    }


    // Tạo mã nhân viên tự động (tăng dần)
    private String generateMaNhanVien() {
        Optional<NhanVien> nhanVienOptional = nhanVienRepository.findFirstByOrderByNhanVienIdDesc();
        int nextNumber = 1;

        if (nhanVienOptional.isPresent()) {
            nextNumber = nhanVienOptional.get().getNhanVienId() + 1;  // Lấy ID tự tăng
        }

        return String.format("NV%04d", nextNumber);
    }

    // Hàm kiểm tra chuỗi rỗng hoặc chỉ chứa khoảng trắng
    private boolean isEmptyString(String str) {
        return str == null || str.trim().isEmpty();
    }

    // Hàm loại bỏ khoảng trắng đầu và cuối chuỗi
    private String trimInput(String str) {
        return str == null ? "" : str.trim();
    }
    // Hàm kiểm tra định dạng mật khẩu mạnh (ít nhất 1 chữ hoa, 1 chữ thường, 1 số)
    private boolean isValidPasswordFormat(String password) {
        Pattern passwordPattern = Pattern.compile("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).{6,20}$");
        return passwordPattern.matcher(password).matches();
    }
}