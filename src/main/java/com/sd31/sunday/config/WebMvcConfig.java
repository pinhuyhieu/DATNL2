package com.sd31.sunday.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private LogoutInterceptor logoutInterceptor;

    @Autowired
    private NhanVienAuthInterceptor nhanVienAuthInterceptor; // Inject interceptor xác thực NhanVien

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Cấu hình xử lý các tài nguyên tĩnh như ảnh upload
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
        // Cấu hình xử lý các tài nguyên tĩnh khác (CSS, JS, ảnh, v.v.)
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
        registry.addResourceHandler("/css/**").addResourceLocations("classpath:/static/css/");
        registry.addResourceHandler("/js/**").addResourceLocations("classpath:/static/js/");
        registry.addResourceHandler("/images/**").addResourceLocations("classpath:/static/images/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        // 1. Đăng ký interceptor xác thực NhanVien ĐẦU TIÊN
        // Interceptor này kiểm tra xem NhanVien đã đăng nhập hay chưa.
        // Áp dụng cho tất cả các đường dẫn yêu cầu xác thực NhanVien.
        registry.addInterceptor(nhanVienAuthInterceptor)
                .addPathPatterns("/admin/**") // Bảo vệ tất cả các đường dẫn UI admin
                .addPathPatterns("/api/ma-giam-gia/**") // Bảo vệ các đường dẫn API mã giảm giá
                // QUAN TRỌNG: Loại trừ trang đăng nhập NhanVien chính nó!
                .excludePathPatterns("/nhanvien/login");

        // 2. Đăng ký interceptor Logout THỨ HAI
        // Interceptor này xử lý việc đăng xuất KhachHang trên các đường dẫn admin/api
        // và đăng xuất NhanVien trên các đường dẫn khách hàng/public.
        // Nó nên chạy SAU các kiểm tra xác thực nếu logic phụ thuộc vào loại người dùng.
        registry.addInterceptor(logoutInterceptor)
                .addPathPatterns("/**") // Áp dụng cho tất cả các đường dẫn mặc định
                // Loại trừ tài nguyên tĩnh khỏi TẤT CẢ các interceptor
                // Chúng không cần kiểm tra session hay logic logout
                .excludePathPatterns("/static/**", "/css/**", "/js/**", "/images/**", "/uploads/**");
        // Có thể xem xét loại trừ các trang đăng nhập/đăng ký chính của khách hàng
        // nếu logic logout trên các trang đó được xử lý riêng hoặc xung đột.
        // .excludePathPatterns("/login", "/register", "/nhanvien/login", "/nhanvien/register");

        // Có thể thêm các interceptor khác tại đây nếu cần
    }
}