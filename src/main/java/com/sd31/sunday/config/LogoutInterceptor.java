package com.sd31.sunday.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;


@Component
public class LogoutInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(LogoutInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false); // Lấy session hiện có, không tạo mới
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();

        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }

        logger.debug("LogoutInterceptor: Checking path: {}", path);
        logger.debug("LogoutInterceptor: Session exists: {}", (session != null));

        if (session != null) {
            Object loggedInUser = session.getAttribute("loggedInUser");
            logger.debug("LogoutInterceptor: Logged in user type: {}", (loggedInUser != null ? loggedInUser.getClass().getSimpleName() : "null"));

            // Logic để đăng xuất NhanVien khi truy cập các trang của khách hàng
            // Thêm các đường dẫn trang khách hàng cần kiểm tra ở đây
            boolean isSpecificCustomerPathForNhanVienLogout =
                    path != null && (
                            path.startsWith("/login") ||
                                    path.startsWith("/home") ||
                                    path.startsWith("/bo-suu-tap") ||
                                    path.startsWith("/products/") ||
                                    path.startsWith("/register")
                            // Thêm các đường dẫn trang khách hàng khác tại đây nếu cần
                    );

            if (isSpecificCustomerPathForNhanVienLogout) {
                // Nếu đường dẫn khớp VÀ người dùng là NhanVien
                if (loggedInUser != null && loggedInUser instanceof com.sd31.sunday.model.NhanVien) {
                    logger.warn("LogoutInterceptor: Phát hiện NhanVien trên đường dẫn khách hàng ({}) . Hủy session.", path);
                    session.invalidate(); // Đăng xuất NhanVien cố gắng truy cập trang khách hàng
                    // Tùy chọn: Chuyển hướng NhanVien ngay lập tức về trang đăng nhập của họ
                    // response.sendRedirect(contextPath + "/nhanvien/login"); // Nếu bỏ comment dòng này, phải return false
                    // return false; // Ngừng xử lý nếu đã chuyển hướng
                } else {
                    logger.debug("LogoutInterceptor: Người dùng trên đường dẫn khách hàng ({}) không phải NhanVien hoặc không có session. Tiếp tục xử lý.", path);
                }
            }

        } else {
            logger.debug("LogoutInterceptor: Không tìm thấy session cho đường dẫn {}. Tiếp tục xử lý.", path);
        }

        logger.debug("LogoutInterceptor: Cho phép request tiếp tục cho đường dẫn {}.", path);
        // Tiếp tục xử lý request
        return true;
    }

    // postHandle và afterCompletion không cần thiết cho logic logout
}