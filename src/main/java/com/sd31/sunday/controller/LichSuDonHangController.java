package com.sd31.sunday.controller;

import com.sd31.sunday.model.HoaDon;
import com.sd31.sunday.model.KhachHang;
import com.sd31.sunday.service.HoaDonService;
import com.sd31.sunday.service.OrderCalculationService;
import org.slf4j.Logger; // Import Logger
import org.slf4j.LoggerFactory; // Import LoggerFactory
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.SessionAttribute; // **QUAN TRỌNG: Import đúng**

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
public class LichSuDonHangController {

    private static final Logger logger = LoggerFactory.getLogger(LichSuDonHangController.class);

    @Autowired
    private HoaDonService hoaDonService;

    @Autowired
    private OrderCalculationService orderCalculationService; // Giả định service này tồn tại và được inject

    /**
     * Hiển thị trang lịch sử đơn hàng của khách hàng.
     * Lấy thông tin người dùng từ session attribute "loggedInUser".
     */
    @GetMapping("/order-history")
    public String viewOrderHistory(
            // Lấy user từ session với tên "loggedInUser"
            @SessionAttribute(name = "loggedInUser", required = false) KhachHang user,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5") int size,
            Model model) {

        // Kiểm tra xem người dùng có tồn tại trong session không
        if (user == null || user.getKhachHangId() == null) {
            logger.warn("User not logged in or user ID is null (from session 'loggedInUser'). Redirecting to login.");
            // Có thể thêm flash attribute để hiển thị thông báo trên trang login
            // redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập để xem lịch sử đơn hàng.");
            return "redirect:/login"; // Chuyển hướng đến trang đăng nhập
        }

        logger.info("Fetching order history for customer ID: {}", user.getKhachHangId());

        // Phân trang và sắp xếp theo ngày tạo hóa đơn giảm dần (mới nhất lên đầu)
        Pageable pageable = PageRequest.of(page, size, Sort.by("ngayTaoHoaDon").descending());
        Page<HoaDon> orderPage = hoaDonService.getHoaDonByKhachHangId(user.getKhachHangId(), pageable);

        // Thêm dữ liệu vào model để view sử dụng
        model.addAttribute("orderPage", orderPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", orderPage.getTotalPages());
        // Thêm lại user vào model nếu view cần truy cập trực tiếp (mặc dù đã có trong session)
        model.addAttribute("user", user);
        // Tính totalPayment cho từng đơn hàng
        java.util.List<java.math.BigDecimal> totalPaymentList = orderPage.getContent().stream()
            .map(orderCalculationService::calculateTotalPayment)
            .toList();
        model.addAttribute("totalPaymentList", totalPaymentList);
        return "customer/order-history"; // Trả về tên view
    }

    /**
     * Hiển thị trang chi tiết đơn hàng.
     * Lấy thông tin người dùng từ session attribute "loggedInUser" để kiểm tra quyền.
     */
    @GetMapping("/order-detail/{id}")
    public String viewOrderDetail(@PathVariable("id") Integer orderId,
                                  // Lấy user từ session với tên "loggedInUser"
                                  @SessionAttribute(name = "loggedInUser", required = false) KhachHang user,
                                  Model model) {

        // Kiểm tra xem người dùng có đăng nhập không
        if (user == null) {
            logger.warn("Unauthorized attempt to view order detail ({}). User not in session.", orderId);
            // Chuyển hướng rõ ràng hơn, ví dụ: báo session hết hạn
            return "redirect:/login?error=session_expired";
        }

        // Tìm hóa đơn theo ID
        HoaDon hoaDon = hoaDonService.findById(orderId)
                .orElseThrow(() -> {
                    logger.error("Order not found with ID: {}", orderId);
                    // Ném lỗi 404 nếu không tìm thấy hóa đơn
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Hóa đơn không tồn tại");
                });

        // **Kiểm tra bảo mật quan trọng:** Đảm bảo người dùng đăng nhập chính là chủ của hóa đơn này
        if (user.getKhachHangId() == null || !hoaDon.getKhachHang().getKhachHangId().equals(user.getKhachHangId())) {
            logger.warn("Unauthorized attempt to view order detail. Order ID: {}, User ID: {}", orderId, user.getKhachHangId());
            // Chuyển hướng về lịch sử đơn hàng với thông báo lỗi không có quyền
            return "redirect:/order-history?error=unauthorized";
        }

        // Tính toán các giá trị hiển thị (giả định OrderCalculationService hoạt động đúng)
        BigDecimal subtotal = orderCalculationService.calculateSubtotal(hoaDon);
        BigDecimal discountAmount = orderCalculationService.calculateDiscountAmount(hoaDon);
        BigDecimal totalPayment = orderCalculationService.calculateTotalPayment(hoaDon);

        // Thêm dữ liệu vào model
        model.addAttribute("hoaDon", hoaDon);
        model.addAttribute("subtotal", subtotal); // Tạm tính
        model.addAttribute("discountAmount", discountAmount); // Số tiền giảm giá
        model.addAttribute("totalPayment", totalPayment); // Tổng tiền thanh toán
        model.addAttribute("user", user); // Thêm user vào model nếu view cần

        return "customer/order-detail"; // Trả về tên view chi tiết đơn hàng
    }

    /**
     * Xử lý yêu cầu hủy đơn hàng từ khách hàng.
     * Lấy thông tin người dùng từ session attribute "loggedInUser".
     * Trả về JSON response cho AJAX call.
     */
    @PostMapping("/order/cancel/{id}")
    @ResponseBody // Đánh dấu phương thức trả về dữ liệu JSON
    public ResponseEntity<Map<String, Object>> cancelOrder(
            @PathVariable("id") Integer id,
            // Lấy user từ session với tên "loggedInUser"
            @SessionAttribute(name = "loggedInUser", required = false) KhachHang user) {

        Map<String, Object> response = new HashMap<>(); // Tạo map để chứa response JSON

        // Kiểm tra xem người dùng có đăng nhập không
        if (user == null || user.getKhachHangId() == null) {
            logger.warn("Attempt to cancel order ID: {} without logged-in user.", id);
            response.put("success", false);
            response.put("message", "Vui lòng đăng nhập để hủy đơn hàng.");
            // Trả về lỗi 401 Unauthorized vì chưa xác thực
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        logger.info("Customer ID: {} attempting to cancel order ID: {}", user.getKhachHangId(), id);

        try {
            // Gọi phương thức service đã được cập nhật để xử lý việc hủy đơn
            // Phương thức này sẽ tự kiểm tra quyền, trạng thái và thực hiện cập nhật
            hoaDonService.cancelOrderByCustomer(id, user.getKhachHangId());

            // Nếu không có lỗi ném ra, tức là hủy thành công
            logger.info("Order ID: {} successfully cancelled by customer ID: {}", id, user.getKhachHangId());
            response.put("success", true);
            response.put("message", "Đơn hàng đã được hủy thành công."); // Thông báo thành công
            return ResponseEntity.ok(response); // Trả về 200 OK

        } catch (IllegalStateException | SecurityException e) {
            // Bắt lỗi về trạng thái đơn hàng không hợp lệ hoặc không có quyền
            logger.warn("Failed to cancel order ID: {}. Reason: {}", id, e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage()); // Gửi thông báo lỗi từ exception
            // Trả về lỗi 403 Forbidden nếu là lỗi quyền, 400 Bad Request nếu là lỗi trạng thái
            if (e instanceof SecurityException) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        } catch (ResponseStatusException e) {
            // Bắt lỗi không tìm thấy đơn hàng (từ findById trong service)
            logger.error("Error cancelling order ID: {}. Order not found.", id, e);
            response.put("success", false);
            response.put("message", "Không tìm thấy đơn hàng.");
            return ResponseEntity.status(e.getStatusCode()).body(response); // Trả về 404 Not Found
        } catch (Exception e) {
            // Bắt các lỗi không mong muốn khác
            logger.error("Unexpected error cancelling order ID: {} for customer ID: {}", id, user.getKhachHangId(), e);
            response.put("success", false);
            response.put("message", "Đã xảy ra lỗi trong quá trình hủy đơn. Vui lòng thử lại.");
            // Trả về lỗi 500 Internal Server Error
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}