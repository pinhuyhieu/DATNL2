package com.sd31.sunday.controller;

import com.sd31.sunday.model.KhachHang;
import com.sd31.sunday.repository.GioHangRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Lớp này sử dụng `@ControllerAdvice` để áp dụng các phương thức chung
 * cho tất cả các controller trong ứng dụng.
 */
@ControllerAdvice
public class GlobalControllerAdvice {

    @Autowired
    private GioHangRepository gioHangRepository;

    /**
     * Phương thức này thêm thuộc tính `user` vào Model, nhưng chỉ khi request path
     * không bắt đầu bằng `/admin/` hoặc `/nhanvien/` VÀ người dùng đăng nhập là KhachHang.
     *
     * @param session Đối tượng HttpSession chứa thông tin phiên đăng nhập người dùng.
     * @param request HttpServletRequest để kiểm tra đường dẫn.
     * @return Đối tượng `KhachHang` từ session nếu có, là KhachHang, và path không phải admin/nhanvien, ngược lại trả về null.
     */
    @ModelAttribute("user")
    public KhachHang loggedInUser(HttpSession session, HttpServletRequest request) {
        String path = request.getRequestURI();

        // If the path is for admin or nhanvien areas, we don't add a KhachHang user attribute
        if (path != null && (path.startsWith("/admin/") || path.startsWith("/nhanvien/"))) {
            return null;
        }

        // Retrieve the logged-in user object from the session
        Object loggedInUser = session.getAttribute("loggedInUser");

        // Check if the retrieved object is not null AND is an instance of KhachHang
        if (loggedInUser != null && loggedInUser instanceof KhachHang) {
            // If it is a KhachHang, safely cast and return it
            return (KhachHang) loggedInUser;
        }

        // If no user is logged in, or the logged-in user is not a KhachHang (e.g., NhanVien),
        // return null for non-admin/non-nhanvien paths.
        return null;
    }

    /**
     * Phương thức này thêm thuộc tính `cartCount` vào Model, nhưng chỉ khi request path
     * không bắt đầu bằng `/admin/` hoặc `/nhanvien/`.
     *
     * @param user KhachHang đã đăng nhập (có thể null).
     * @param request HttpServletRequest để kiểm tra đường dẫn.
     * @return Số lượng sản phẩm trong giỏ hàng nếu user đăng nhập và path không phải admin/nhanvien, ngược lại trả về 0.
     */
    @ModelAttribute("cartCount")
    public Integer cartCount(@ModelAttribute("user") KhachHang user, HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path != null && (path.startsWith("/admin/") || path.startsWith("/nhanvien/"))) {
            return 0; // Không áp dụng cho admin hoặc nhanvien paths
        }
        // This relies on the 'user' attribute correctly being a KhachHang or null
        if (user != null) {
            return gioHangRepository.countByTaiKhoanId(user.getKhachHangId());
        }
        return 0; // Trả về 0 nếu user không đăng nhập (or is NhanVien) or is admin/nhanvien path handled above
    }
}