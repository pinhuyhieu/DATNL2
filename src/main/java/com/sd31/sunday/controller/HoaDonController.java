package com.sd31.sunday.controller;

import com.sd31.sunday.exception.InsufficientStockException;
import com.sd31.sunday.model.*;
import com.sd31.sunday.repository.MaGiamGiaRepository;
import jakarta.persistence.EntityNotFoundException;
import com.sd31.sunday.repository.ChiTietSanPhamRepository;
import com.sd31.sunday.service.HoaDonService;
import com.sd31.sunday.service.LichSuTrangThaiService;
import com.sd31.sunday.service.OrderCalculationService;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Controller
@RequestMapping("/admin/hoa-don")
public class HoaDonController {

    private static final Logger logger = LoggerFactory.getLogger(HoaDonController.class); // Logger


    @Autowired
    private HoaDonService hoaDonService;

    @Autowired
    private LichSuTrangThaiService lichSuTrangThaiService;

    @Autowired
    private ChiTietSanPhamRepository chiTietSanPhamRepository; // Cần cho việc trừ/hoàn tồn kho

    @Autowired
    private MaGiamGiaRepository maGiamGiaRepository;

    @Autowired
    private OrderCalculationService orderCalculationService;


    @GetMapping
    public String macDinh(Model model) {
        // Redirect đến trang hiển thị mặc định
        return "redirect:/admin/hoa-don/hien-thi";
    }

    @GetMapping("/hien-thi")
    public String viewHoaDon(Model model,
                             @RequestParam(name = "page", defaultValue = "0") int page,
                             @RequestParam(name = "size", defaultValue = "10") int size,
                             @RequestParam(name = "maHoaDon", required = false) String maHoaDon,
                             @RequestParam(name = "trangThai", required = false) String trangThai,
                             @RequestParam(name = "kenhBanHang", required = false) String kenhBanHang,
                             @RequestParam(name = "ngayTaoTu", required = false) String ngayTaoTuStr, // Giữ tên gốc từ form
                             @RequestParam(name = "ngayTaoDen", required = false) String ngayTaoDenStr) { // Giữ tên gốc từ form
        model.addAttribute("activePage", "hoaDon");

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "ngayTaoHoaDon"));
        Page<HoaDon> hoaDonPage;

        LocalDateTime startDate = null;
        LocalDateTime endDate = null;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        try {
            if (StringUtils.hasText(ngayTaoTuStr)) {
                LocalDate localStartDate = LocalDate.parse(ngayTaoTuStr, formatter);
                startDate = localStartDate.atStartOfDay(); // Bắt đầu ngày (00:00:00)
            }
            if (StringUtils.hasText(ngayTaoDenStr)) {
                LocalDate localEndDate = LocalDate.parse(ngayTaoDenStr, formatter);
                endDate = localEndDate.atTime(LocalTime.MAX); // Kết thúc ngày (23:59:59...) để bao gồm cả ngày đó
            }
        } catch (DateTimeParseException e) {
            logger.error("Error parsing date parameters: From='{}', To='{}'. Error: {}", ngayTaoTuStr, ngayTaoDenStr, e.getMessage());
            model.addAttribute("errorMessage", "Định dạng ngày không hợp lệ (yyyy-MM-dd). Vui lòng kiểm tra lại.");
            // Không lọc theo ngày nếu parse lỗi
            startDate = null;
            endDate = null;
        }

        // Gọi service với kiểu LocalDateTime mới và các tham số khác
        hoaDonPage = hoaDonService.filterHoaDon(maHoaDon, trangThai, kenhBanHang, startDate, endDate, pageable);

        // Thêm dữ liệu vào model để hiển thị
        model.addAttribute("hoaDonPage", hoaDonPage);
        model.addAttribute("maHoaDon", maHoaDon); // Giữ lại giá trị để hiển thị trên form
        model.addAttribute("trangThai", trangThai); // Giữ lại giá trị để hiển thị trên form
        model.addAttribute("kenhBanHang", kenhBanHang); // Giữ lại giá trị để hiển thị trên form
        model.addAttribute("ngayTaoTu", ngayTaoTuStr); // Quan trọng: Đưa chuỗi gốc trở lại form
        model.addAttribute("ngayTaoDen", ngayTaoDenStr); // Quan trọng: Đưa chuỗi gốc trở lại form

        // Tính totalPayment cho từng hóa đơn
        java.util.List<java.math.BigDecimal> totalPaymentList = hoaDonPage.getContent().stream()
            .map(orderCalculationService::calculateTotalPayment)
            .toList();
        model.addAttribute("totalPaymentList", totalPaymentList);

        // Có thể thêm danh sách cho dropdown nếu cần
        model.addAttribute("trangThaiList", Arrays.asList("Chờ xác nhận", "Chờ thanh toán", "Đã xác nhận", "Đang giao hàng", "Đã giao hàng", "Đã hủy", "Đã thanh toán", "Đã thanh toán online"));
        model.addAttribute("kenhBanHangList", Arrays.asList("Website", "POS"));

        return "admin/hoa-don/hoa-don"; // Đường dẫn tới file view Thymeleaf
    }

    @GetMapping("/detail/{id}")
    public String getHoaDonDetail(@PathVariable("id") Integer id, Model model) {
        model.addAttribute("activePage", "hoaDon");
        HoaDon hoaDon = hoaDonService.getHoaDonById(id);

        if (hoaDon == null) {
            model.addAttribute("errorMessage", "Không tìm thấy hóa đơn với ID: " + id);
            return "admin/hoa-don/hoa-don"; // Redirect back or show an error view
        }

        model.addAttribute("hoaDon", hoaDon);
        // Fetch LichSuTrangThai for this HoaDon
        List<LichSuTrangThai> lichSuTrangThais = lichSuTrangThaiService.getLichSuByHoaDonId(id);
        model.addAttribute("lichSuTrangThais", lichSuTrangThais); // Add to the model

        // Đồng bộ logic với order-detail phía khách hàng
        java.math.BigDecimal subtotal = orderCalculationService.calculateSubtotal(hoaDon);
        java.math.BigDecimal discountAmount = orderCalculationService.calculateDiscountAmount(hoaDon);
        java.math.BigDecimal totalPayment = orderCalculationService.calculateTotalPayment(hoaDon);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("discountAmount", discountAmount);
        model.addAttribute("totalPayment", totalPayment);

        // Determine if the "Next Status" button should be enabled/visible
        model.addAttribute("canAdvanceStatus", getNextStatus(hoaDon.getTrangThai()) != null);

        // *** MODIFICATION START: Update canCancel logic ***
        // Determine if the "Cancel" button should be enabled/visible
        // (Allow cancel unless already delivered, cancelled, or shipping)
        String currentStatus = hoaDon.getTrangThai();
        boolean canCancelOrder = !currentStatus.equals("Đã giao hàng") // Cannot cancel if Delivered
                && !currentStatus.equals("Đã hủy")       // Cannot cancel if already Cancelled
                && !currentStatus.equals("Đang giao hàng"); // Cannot cancel if Shipping
        model.addAttribute("canCancel", canCancelOrder);
        // *** MODIFICATION END ***

        return "admin/hoa-don/hoa-don-chi-tiet";
    }


    // Helper method to check if status is changing to "Đã xác nhận" from "Chờ xác nhận"
    private boolean trangThaiMoiLaDaXacNhan(String trangThaiHienTai, String trangThaiMoi) {
        return "Chờ xác nhận".equals(trangThaiHienTai) && "Đã xác nhận".equals(trangThaiMoi);
    }

    @PostMapping("/next-status/{id}")
    @Transactional(rollbackOn = Exception.class)
    public String nextStatus(@PathVariable("id") Integer id,
                             @RequestParam("ghiChu") String ghiChu,
                             RedirectAttributes redirectAttributes) {
        HoaDon hoaDon = null;
        String trangThaiHienTai = null;
        logger.info("Attempting to change status for order ID: {}", id); // LOG START

        try {
            hoaDon = hoaDonService.getHoaDonById(id);
            if (hoaDon == null) {
                logger.error("Order ID {} not found.", id); // LOG ERROR
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy hóa đơn");
                return "redirect:/admin/hoa-don/hien-thi";
            }

            trangThaiHienTai = hoaDon.getTrangThai();
            String trangThaiMoi = getNextStatus(trangThaiHienTai);

            logger.info("Order ID {}: Current Status='{}', Target Next Status='{}'", id, trangThaiHienTai, trangThaiMoi); // LOG STATUSES

            if (trangThaiMoi == null) {
                String message = "Không thể chuyển trạng thái tiếp theo.";
                if (trangThaiHienTai.equals("Đã giao hàng") || trangThaiHienTai.equals("Đã hủy") || trangThaiHienTai.equals("Thanh toán thất bại")) {
                    message = "Hóa đơn đã ở trạng thái cuối cùng (" + trangThaiHienTai + ").";
                }
                logger.warn("Order ID {}: Cannot transition from status '{}'", id, trangThaiHienTai); // LOG WARN
                redirectAttributes.addFlashAttribute("error", message);
                return "redirect:/admin/hoa-don/detail/" + id;
            }

            String trangThaiCu = trangThaiHienTai;

            // --- BẮT ĐẦU LOGIC KIỂM TRA VÀ GIẢM TỒN KHO ---
            boolean isConfirming = trangThaiMoiLaDaXacNhan(trangThaiCu, trangThaiMoi);
            logger.info("Order ID {}: Is transition 'Chờ xác nhận' -> 'Đã xác nhận'? {}", id, isConfirming); // LOG CHECK

            if (isConfirming) {
                logger.info("Order ID {}: Entering stock reduction logic for confirmation.", id); // LOG ENTERING REDUCE
                // Logic giảm tồn kho khi xác nhận
                reduceStockForOrder(hoaDon); // This method will throw InsufficientStockException if stock is low
                logger.info("Order ID {}: Stock reduction check passed (or no exception thrown).", id); // LOG PASSED REDUCE
            }
            // --- KẾT THÚC LOGIC KIỂM TRA VÀ GIẢM TỒN KHO ---


            // If we reach here without an exception from reduceStockForOrder:
            hoaDon.setTrangThai(trangThaiMoi); // Cập nhật trạng thái mới
            logger.info("Order ID {}: Status updated in object to '{}'", id, trangThaiMoi); // LOG STATUS OBJECT UPDATE

            // Logic cập nhật ngayBan
            if ("Đang giao hàng".equals(trangThaiCu) && "Đã giao hàng".equals(trangThaiMoi)) {
                logger.info("Transitioning order {} from 'Đang giao hàng' to 'Đã giao hàng'. Setting ngayBan.", id);
                hoaDon.setNgayBan(LocalDateTime.now());
                logger.info("ngayBan updated to: {}", hoaDon.getNgayBan());
            }

            hoaDonService.saveHoaDon(hoaDon); // Lưu các thay đổi
            logger.info("Order ID {}: HoaDon saved successfully with new status '{}'.", id, trangThaiMoi); // LOG SAVE SUCCESS


            // Lưu lịch sử trạng thái
            saveLichSuTrangThai(hoaDon, trangThaiCu, trangThaiMoi, ghiChu);

            redirectAttributes.addFlashAttribute("success", "Cập nhật trạng thái thành công");
            return "redirect:/admin/hoa-don/detail/" + id;

        } catch (InsufficientStockException stockException) {
            // Exception được throw từ reduceStockForOrder
            logger.error("Order ID {}: Insufficient stock detected! Message: {}", id, stockException.getMessage(), stockException);
            // ---> DÒNG QUAN TRỌNG: Đảm bảo dòng này đang gửi thông báo lỗi <---
            redirectAttributes.addFlashAttribute("error", stockException.getMessage());
            // Transaction should rollback here automatically
            return "redirect:/admin/hoa-don/detail/" + id;
        } catch (Exception e) {
            logger.error("Order ID {}: General error updating status from '{}'. Message: {}", id, trangThaiHienTai, e.getMessage(), e);
            // ---> Đảm bảo cũng có addFlashAttribute ở đây cho các lỗi khác <---
            redirectAttributes.addFlashAttribute("error", "Lỗi cập nhật trạng thái: " + e.getMessage());
            // Transaction should rollback here automatically
            return "redirect:/admin/hoa-don/detail/" + id;
        }
    }

    @PostMapping("/cancel/{id}")
    @Transactional(rollbackOn = Exception.class)
    public String cancelOrder(@PathVariable("id") Integer id,
                              @RequestParam("ghiChu") String ghiChu,
                              RedirectAttributes redirectAttributes) {
        HoaDon hoaDon = null;
        String trangThaiHienTai = null;
        logger.info("Attempting to cancel order ID: {}", id);

        try {
            hoaDon = hoaDonService.getHoaDonById(id);
            if (hoaDon == null) {
                logger.error("Order ID {} not found for cancellation.", id);
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy hóa đơn");
                return "redirect:/admin/hoa-don/hien-thi";
            }

            trangThaiHienTai = hoaDon.getTrangThai();
            logger.info("Order ID {}: Current status before cancellation is '{}'", id, trangThaiHienTai);

            // *** MODIFICATION START: Update cancellation check ***
            // Check if cancellation is allowed based on current status
            if (trangThaiHienTai.equals("Đã hủy")
                    || trangThaiHienTai.equals("Đã giao hàng")
                    || trangThaiHienTai.equals("Đang giao hàng") // Added this condition
                    || trangThaiHienTai.equals("Thanh toán thất bại")) {
                logger.warn("Order ID {}: Cancellation forbidden from status '{}'.", id, trangThaiHienTai);
                redirectAttributes.addFlashAttribute("error", "Không thể hủy hóa đơn ở trạng thái '" + trangThaiHienTai + "'.");
                return "redirect:/admin/hoa-don/detail/" + id;
            }
            // *** MODIFICATION END ***


            // --- START: GET DISCOUNT CODE BEFORE STATUS CHANGE ---
            MaGiamGia associatedCode = hoaDon.getMaGiamGia();
            // --- END: GET DISCOUNT CODE BEFORE STATUS CHANGE ---

            String trangThaiCu = trangThaiHienTai;
            hoaDon.setTrangThai("Đã hủy");
            hoaDonService.saveHoaDon(hoaDon); // Save the status change FIRST
            logger.info("Order ID {}: Status updated to 'Đã hủy' in DB.", id);


            // --- START: RESTORE DISCOUNT CODE LOGIC ---
            if (associatedCode != null) {
                logger.info("Order ID {}: Attempting to restore discount code ID {} ({})", id, associatedCode.getMaGiamGiaId(), associatedCode.getTenMaGiamGia());
                try {
                    // Fetch the latest state of the discount code *within the transaction*
                    MaGiamGia codeToUpdate = maGiamGiaRepository.findById(associatedCode.getMaGiamGiaId())
                            .orElseThrow(() -> {
                                logger.error("Order ID {}: MaGiamGia not found with ID: {} during restoration attempt.", id, associatedCode.getMaGiamGiaId());
                                return new EntityNotFoundException("Không tìm thấy Mã giảm giá ID: " + associatedCode.getMaGiamGiaId() + " để hoàn lại số lượng.");
                            });

                    codeToUpdate.setSoLuong(codeToUpdate.getSoLuong() + 1); // Increment the available quantity
                    maGiamGiaRepository.save(codeToUpdate); // Save the updated discount code
                    logger.info("Order ID {}: Discount code ID {} quantity incremented. New quantity: {}", id, codeToUpdate.getMaGiamGiaId(), codeToUpdate.getSoLuong());
                } catch (Exception e) {
                    logger.error("Order ID {}: Failed to restore quantity for discount code ID {}. Error: {}", id, associatedCode.getMaGiamGiaId(), e.getMessage(), e);
                    // Consider re-throwing if this is critical
                    // throw new RuntimeException("Lỗi nghiêm trọng: Không thể hoàn lại mã giảm giá ID " + associatedCode.getMaGiamGiaId(), e);
                }
            } else {
                logger.info("Order ID {}: No discount code associated with this order. Skipping discount code restoration.", id);
            }
            // --- END: RESTORE DISCOUNT CODE LOGIC ---


            // --- START: CONDITIONAL RESTOCK LOGIC ---
            // Only restock if the order was cancelled from a state AFTER confirmation
            // (Also ensure it wasn't cancelled from a payment-related status where stock might not have been reduced yet, depending on exact flow)
            // This simplified logic assumes stock is reduced ONLY upon 'Chờ xác nhận' -> 'Đã xác nhận'
            if (!trangThaiCu.equals("Chờ xác nhận")) {
                logger.info("Order ID {}: Order was cancelled from status '{}'. Proceeding to restock items.", id, trangThaiCu);
                restockItemsForCancelledOrder(hoaDon);
            } else {
                logger.info("Order ID {}: Order was cancelled from status 'Chờ xác nhận'. Skipping restock.", id);
            }
            // --- END: CONDITIONAL RESTOCK LOGIC ---

            saveLichSuTrangThai(hoaDon, trangThaiCu, "Đã hủy", ghiChu);

            redirectAttributes.addFlashAttribute("success", "Hủy đơn thành công." + (associatedCode != null ? " Mã giảm giá đã được hoàn lại." : "")); // Updated message
            return "redirect:/admin/hoa-don/detail/" + id;

        } catch (Exception e) {
            logger.error("Error cancelling order ID {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Lỗi hủy đơn: " + e.getMessage());
            // Transaction should rollback here automatically due to @Transactional
            return "redirect:/admin/hoa-don/detail/" + id;
        }
    }

    // Helper method to determine the next status
    private String getNextStatus(String currentStatus) {
        switch (currentStatus) {
            case "Chờ xác nhận": return "Đã xác nhận";
            // Thêm các chuyển đổi cho các trạng thái khác nếu có
            case "Đã xác nhận": return "Đang giao hàng"; // Chuyển từ đã xác nhận sang đang giao
            case "Đã thanh toán online": return "Đang giao hàng"; // Hoặc trực tiếp sang "Đang giao hàng" tùy quy trình
            case "Đang giao hàng": return "Đã giao hàng"; // Chuyển từ đang giao sang đã giao
            // Các trạng thái kết thúc hoặc không cho phép chuyển tiếp qua nút "next"
            case "Đã giao hàng": return null;
            case "Đã hủy": return null;
            case "Thanh toán thất bại": return null;
            // Các trạng thái khác nếu có (ví dụ: "Chờ thanh toán" cho chuyển khoản thủ công)
            // case "Chờ thanh toán": return "Đã thanh toán";
            // case "Đã thanh toán": return "Đã xác nhận"; // Hoặc "Đang giao hàng"
            default: return null; // Trạng thái không hợp lệ hoặc chưa được định nghĩa chuyển tiếp
        }
    }

    // Helper method to save status history
    private void saveLichSuTrangThai(HoaDon hoaDon, String trangThaiCu, String trangThaiMoi, String ghiChu) {
        try {
            LichSuTrangThai lichSu = new LichSuTrangThai();
            lichSu.setHoaDon(hoaDon);
            lichSu.setTrangThaiCu(trangThaiCu);
            lichSu.setTrangThaiMoi(trangThaiMoi);
            lichSu.setNgayThayDoi(LocalDateTime.now());
            // Lấy ID nhân viên từ session
            Integer nhanVienId = getCurrentNhanVienId();
            // Cần đảm bảo trường NhanVien trong LichSuTrangThai có thể null nếu không tìm thấy nhân viên
            // hoặc bạn cần xử lý lỗi/mặc định nếu session hết hạn/không có thông tin NV
            if(nhanVienId != null) {
                NhanVien nhanVien = new NhanVien(); // Tạo đối tượng NhanVien giả với chỉ ID
                nhanVien.setNhanVienId(nhanVienId);
                lichSu.setNhanVienId(nhanVienId); // Gán đối tượng NhanVien
            } else {
                logger.warn("Could not get current NhanVien ID from session for order history. Recording history without staff ID.");
                // Trường NhanVien trong LichSuTrangThai entity phải được đánh dấu là optional (@ManyToOne(optional = true))
                // hoặc cho phép null trong DB schema nếu không set ID.
            }

            lichSu.setGhiChu(ghiChu);
            lichSuTrangThaiService.saveLichSu(lichSu);
            logger.info("Saved status history for order ID {}: {} -> {}", hoaDon.getHoaDonId(), trangThaiCu, trangThaiMoi);
        } catch (Exception e) {
            logger.error("Error saving status history for order ID {}: {}", hoaDon.getHoaDonId(), e.getMessage(), e);
            // Không throw exception ở đây để không làm ảnh hưởng đến transaction chính của update trạng thái
        }
    }

    // Method to reduce stock when order is confirmed (Chờ xác nhận -> Đã xác nhận)
    // @Transactional ở đây bảo vệ logic trừ tồn kho. Nếu có lỗi, rollback.
    // Vẫn còn tiềm ẩn vấn đề với đơn VNPAY đã trừ tồn kho ở CheckoutService. Cần refactor.
    @Transactional(rollbackOn = Exception.class) // Although parent method is transactional, keep it here for clarity/safety
    public void reduceStockForOrder(HoaDon hoaDon) throws InsufficientStockException, Exception {
        Integer orderId = hoaDon.getHoaDonId(); // Get order ID once
        logger.info("Attempting to reduce stock for order ID: {}", orderId);
        if (hoaDon.getChiTietHoaDons() == null || hoaDon.getChiTietHoaDons().isEmpty()) {
            logger.warn("Order ID {}: No items found in ChiTietHoaDons list to reduce stock for.", orderId);
            return;
        }

        logger.debug("Order ID {}: Processing {} items for stock reduction.", orderId, hoaDon.getChiTietHoaDons().size());

        for (ChiTietHoaDon chiTietHoaDon : hoaDon.getChiTietHoaDons()) {
            Integer ctspId = chiTietHoaDon.getChiTietSanPham().getChiTietSanPhamId();
            int soLuongMua = chiTietHoaDon.getSoLuong();

            logger.debug("Order ID {}: Checking item CTSP ID: {}, Quantity requested: {}", orderId, ctspId, soLuongMua);

            // Fetch FRESH data within the transaction
            ChiTietSanPham chiTietSanPham = chiTietSanPhamRepository.findById(ctspId)
                    .orElseThrow(() -> {
                        logger.error("Order ID {}: ChiTietSanPham not found for ID: {}", orderId, ctspId); // LOG NOT FOUND
                        return new Exception("Chi tiết sản phẩm không tồn tại: ID " + ctspId);
                    });

            Integer currentStock = chiTietSanPham.getSoLuongTon();
            logger.info("Order ID {}: CTSP ID: {}, Current Stock: {}, Requested: {}", orderId, ctspId, currentStock, soLuongMua); // LOG STOCK LEVELS

            // THE CRITICAL CHECK
            if (currentStock < soLuongMua) {
                SanPham sanPham = chiTietSanPham.getSanPham();
                String productName = (sanPham != null) ? sanPham.getTenSanPham() : "ID " + ctspId; // More informative fallback
                String errorMessage = "Không đủ hàng cho sản phẩm '" + productName + "' (Mã CTSP: " + ctspId +"). Yêu cầu: " + soLuongMua + ", Chỉ còn: " + currentStock + " sản phẩm.";
                logger.error("Order ID {}: INSUFFICIENT STOCK DETECTED for CTSP ID: {}. Requested: {}, Available: {}",
                        orderId, ctspId, soLuongMua, currentStock); // LOG INSUFFICIENT STOCK
                throw new InsufficientStockException(errorMessage); // THROWING EXCEPTION
            }

            // If stock is sufficient
            logger.debug("Order ID {}: Stock sufficient for CTSP ID: {}. Reducing stock.", orderId, ctspId);
            chiTietSanPham.setSoLuongTon(currentStock - soLuongMua);
            chiTietSanPhamRepository.save(chiTietSanPham); // Save updated stock
            logger.info("Order ID {}: Stock reduced for CTSP ID: {}. New Stock: {}",
                    orderId, ctspId, chiTietSanPham.getSoLuongTon()); // LOG STOCK REDUCED
        }
        logger.info("Stock reduction process completed successfully (all items checked) for order ID: {}", orderId);
    }


    // Method to restock items when order is cancelled
    // @Transactional ở đây bảo vệ logic hoàn tồn kho. Nếu có lỗi, rollback.
    @Transactional(rollbackOn = Exception.class)
    public void restockItemsForCancelledOrder(HoaDon hoaDon) {
        logger.info("Attempting to restock items for cancelled order ID: {}", hoaDon.getHoaDonId());
        if (hoaDon.getChiTietHoaDons() == null || hoaDon.getChiTietHoaDons().isEmpty()) {
            logger.warn("Cancelled order {} has no items to restock for.", hoaDon.getHoaDonId());
            return;
        }
        for (ChiTietHoaDon chiTietHoaDon : hoaDon.getChiTietHoaDons()) {
            // Lấy lại ChiTietSanPham để đảm bảo lấy được dữ liệu tồn kho mới nhất trong transaction này
            ChiTietSanPham chiTietSanPham = chiTietSanPhamRepository.findById(chiTietHoaDon.getChiTietSanPham().getChiTietSanPhamId()).orElse(null);

            if(chiTietSanPham == null) {
                logger.warn("ChiTietSanPham with ID {} not found while trying to restock for cancelled order ID {}. Skipping this item.",
                        chiTietHoaDon.getChiTietSanPham().getChiTietSanPhamId(), hoaDon.getHoaDonId());
                continue; // Bỏ qua sản phẩm này nếu không tìm thấy
            }

            int soLuongMua = chiTietHoaDon.getSoLuong();

            logger.info("Restocking ChiTietSanPham ID: {}, current stock: {}, quantity to restock: {}",
                    chiTietSanPham.getChiTietSanPhamId(), chiTietSanPham.getSoLuongTon(), soLuongMua);
            chiTietSanPham.setSoLuongTon(chiTietSanPham.getSoLuongTon() + soLuongMua);
            chiTietSanPhamRepository.save(chiTietSanPham); // Lưu tồn kho đã cập nhật
            logger.info("Stock restocked for ChiTietSanPham ID: {}, updated stock: {}",
                    chiTietSanPham.getChiTietSanPhamId(), chiTietSanPham.getSoLuongTon());
        }
        logger.info("Restock completed for cancelled order ID: {}", hoaDon.getHoaDonId());
    }


    // Phương thức lấy ID nhân viên hiện tại từ session
    private Integer getCurrentNhanVienId() {
        try {
            ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attr != null) {
                HttpSession session = attr.getRequest().getSession(false); // false để tránh tạo session mới nếu chưa có
                if (session != null) {
                    // Giả định key "loggedInUser" lưu đối tượng NhanVien
                    NhanVien nhanVien = (NhanVien) session.getAttribute("loggedInUser");
                    if (nhanVien != null && nhanVien.getNhanVienId() != null) {
                        return nhanVien.getNhanVienId();
                    } else if (nhanVien != null) {
                        logger.warn("loggedInUser in session is NhanVien but has no ID.");
                    } else {
                        logger.debug("No loggedInUser found in session.");
                    }
                } else {
                    logger.debug("No active session found.");
                }
            } else {
                logger.debug("No RequestAttributes found.");
            }
        } catch (Exception e) {
            logger.error("Error getting current NhanVien ID from session: {}", e.getMessage(), e);
        }
        return null; // Trả về null nếu không tìm thấy ID nhân viên
    }


    // Optional: Helper method to define valid status transitions
    // This can be used in nextStatus to prevent invalid manual jumps
    private boolean isValidStatusTransition(String trangThaiHienTai, String trangThaiMoi) {
        // Định nghĩa các chuyển đổi trạng thái hợp lệ
        Map<String, List<String>> validTransitions = new HashMap<>();
        validTransitions.put("Chờ xác nhận", Arrays.asList("Đã xác nhận", "Đã hủy"));
        validTransitions.put("Đã xác nhận", Arrays.asList("Đang giao hàng", "Đã hủy")); // Có thể thêm "Chờ thanh toán" cho luồng khác
        validTransitions.put("Đã thanh toán online", Arrays.asList( "Đang giao hàng", "Đã hủy")); // Từ online có thể xác nhận hoặc giao ngay
        validTransitions.put("Đang giao hàng", Arrays.asList("Đã giao hàng", "Đã hủy"));
        // Các trạng thái cuối không có chuyển tiếp qua nút next, nhưng có thể bị hủy (tùy business logic)
        // validTransitions.put("Đã giao hàng", Collections.emptyList()); // Không chuyển tiếp
        // validTransitions.put("Đã hủy", Collections.emptyList()); // Không chuyển tiếp
        // validTransitions.put("Thanh toán thất bại", Collections.emptyList()); // Không chuyển tiếp


        List<String> allowedTransitions = validTransitions.get(trangThaiHienTai);
        return allowedTransitions != null && allowedTransitions.contains(trangThaiMoi);
    }


}