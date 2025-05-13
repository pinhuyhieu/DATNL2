package com.sd31.sunday.config;


import com.sd31.sunday.model.NhanVien;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class NhanVienAuthInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(NhanVienAuthInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false); // Lấy session hiện có, không tạo mới

        boolean isLoggedInNhanVien = false;

        if (session != null) {
            Object loggedInUser = session.getAttribute("loggedInUser");
            logger.debug("NhanVienAuthInterceptor: Path: {}, Session exists: {}, User Type: {}",
                    request.getRequestURI(),
                    (session != null),
                    (loggedInUser != null ? loggedInUser.getClass().getSimpleName() : "null"));

            // Kiểm tra nếu có người dùng đăng nhập trong session
            if (loggedInUser != null) {
                // Kiểm tra nếu người dùng đó là NhanVien
                if (loggedInUser instanceof NhanVien) {
                    isLoggedInNhanVien = true;
                    logger.debug("NhanVienAuthInterceptor: Người dùng là NhanVien. Cho phép truy cập.");
                } else {
                    // Người dùng đã đăng nhập nhưng không phải NhanVien (ví dụ: KhachHang)
                    // Người dùng này không được phép truy cập trang admin
                    logger.warn("NhanVienAuthInterceptor: Phát hiện người dùng không phải NhanVien ({}) trên đường dẫn admin. Hủy session.", loggedInUser.getClass().getSimpleName());
                    session.invalidate(); // Đăng xuất họ
                    // isLoggedInNhanVien vẫn là false, sẽ dẫn đến việc chuyển hướng bên dưới
                }
            } else {
                logger.debug("NhanVienAuthInterceptor: Không tìm thấy thuộc tính 'loggedInUser' trong session.");
            }
        } else {
            logger.debug("NhanVienAuthInterceptor: Không tìm thấy session.");
        }

        // Nếu người dùng KHÔNG phải là NhanVien đã đăng nhập (không có session, không có user, hoặc sai loại user)
        if (!isLoggedInNhanVien) {
            logger.debug("NhanVienAuthInterceptor: Người dùng không phải NhanVien đã đăng nhập. Chuyển hướng đến trang đăng nhập.");
            // Chuyển hướng đến trang đăng nhập NhanVien
            response.sendRedirect(request.getContextPath() + "/nhanvien/login");
            // Ngừng xử lý request thêm
            return false;
        }

        // Nếu người dùng LÀ NhanVien đã đăng nhập, cho phép request tiếp tục
        return true;
    }
    // postHandle và afterCompletion không cần thiết cho logic này
}