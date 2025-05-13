package com.sd31.sunday.service;

import com.sd31.sunday.DTO.CartItemDTO;
import com.sd31.sunday.DTO.CouponResultDTO;
import com.sd31.sunday.exception.InsufficientStockException;
import com.sd31.sunday.model.*;
import com.sd31.sunday.repository.*;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CheckoutService {

    @Autowired
    private HoaDonRepository hoaDonRepository;
    @Autowired
    private ChiTietHoaDonRepository chiTietHoaDonRepository;
    @Autowired
    private KhachHangRepository khachHangRepository;
    @Autowired
    private DiaChiRepository diaChiRepository; // Cần để xóa địa chỉ
    @Autowired
    private MaGiamGiaRepository maGiamGiaRepository;
    @Autowired
    private PhuongThucThanhToanRepository phuongThucThanhToanRepository;
    @Autowired
    private ChiTietSanPhamRepository chiTietSanPhamRepository;
    @Autowired
    private SanPhamRepository sanPhamRepository; // Cần để lấy tên sản phẩm cho Exception

    @Autowired
    private SanPhamService sanPhamService;

    private static final Logger logger = LoggerFactory.getLogger(CheckoutService.class);

    // Phương thức đặt hàng thông thường (COD, chuyển khoản thủ công...)
    @Transactional(rollbackOn = Exception.class)
    public void placeOrder(KhachHang khachHang, String fullName, String phone, String fullAddress, String paymentMethod,
                           String couponCode, List<CartItemDTO> cartItems, BigDecimal shippingFee) throws Exception {
        logger.info("Starting placeOrder for customer ID: {}, paymentMethod: {}", khachHang != null ? khachHang.getKhachHangId() : "null", paymentMethod);
        if (khachHang == null) {
            throw new IllegalArgumentException("Khách hàng không được null.");
        }
        if (cartItems == null || cartItems.isEmpty()) {
            throw new IllegalArgumentException("Giỏ hàng không được rỗng.");
        }

        // 1. Create HoaDon (Order Header)
        HoaDon hoaDon = new HoaDon();
        hoaDon.setMaHoaDon(generateOrderCode());
        hoaDon.setKhachHang(khachHang);
        hoaDon.setNgayTaoHoaDon(LocalDateTime.now());
        hoaDon.setNgayBan(null); // Sale date set upon confirmation/payment/shipment
        hoaDon.setKenhBanHang("Website");
        hoaDon.setNguoiNhanHang(fullName);
        hoaDon.setSdtNhanHang(phone);
        // Status indicating order is placed but needs confirmation/processing
        hoaDon.setTrangThai("Chờ xác nhận"); // Ensure this status is in SanPhamService.RESERVING_STATUSES

        BigDecimal totalOrderAmountBeforeDiscount = calculateTotalOrderAmount(cartItems);
        hoaDon.setPhiVanChuyenGhn(shippingFee != null ? shippingFee : BigDecimal.ZERO);

        // 1.1. Handle Shipping Address
        // Creates a new address record for this specific order
        DiaChi diaChiGiaoHang = new DiaChi();
        diaChiGiaoHang.setKhachHang(khachHang);
        // Adapt parsing based on your confirmed address format
        diaChiGiaoHang.setGhiChu(getAddressPart(fullAddress, 3)); // e.g., Street/Detail
        diaChiGiaoHang.setPhuongXa(getAddressPart(fullAddress, 2)); // e.g., Ward
        diaChiGiaoHang.setQuanHuyen(getAddressPart(fullAddress, 1)); // e.g., District
        diaChiGiaoHang.setThanhPho(getAddressPart(fullAddress, 0)); // e.g., City
        diaChiGiaoHang.setMacDinh(false); // Not the default address
        diaChiRepository.save(diaChiGiaoHang);
        hoaDon.setDiaChi(diaChiGiaoHang);

        // 1.2. Apply Coupon
        MaGiamGia maGiamGia = null;
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (couponCode != null && !couponCode.trim().isEmpty()) {
            Optional<MaGiamGia> maGiamGiaOpt = maGiamGiaRepository.findByTenMaGiamGia(couponCode.trim());
            if (maGiamGiaOpt.isPresent()) {
                MaGiamGia potentialCoupon = maGiamGiaOpt.get();
                // Validate coupon: Active, Quantity, Expiry, Min Order Value
                if ("Hoạt động".equalsIgnoreCase(potentialCoupon.getTrangThai()) &&
                        potentialCoupon.getSoLuong() > 0 &&
                        (potentialCoupon.getNgayKetThuc() == null || potentialCoupon.getNgayKetThuc().isAfter(LocalDateTime.now())) &&
                        (potentialCoupon.getGiaTriToiThieuDonHang() == null || totalOrderAmountBeforeDiscount.compareTo(potentialCoupon.getGiaTriToiThieuDonHang()) >= 0))
                {
                    maGiamGia = potentialCoupon; // Valid coupon
                    discountAmount = calculateDiscountAmount(totalOrderAmountBeforeDiscount, maGiamGia);
                    hoaDon.setMaGiamGia(maGiamGia); // Associate with order
                    logger.info("Coupon '{}' validated and applied to order {}. Discount: {}", couponCode, hoaDon.getMaHoaDon(), discountAmount);
                } else {
                    // Log reason for invalidity but proceed without coupon
                    logger.warn("Coupon code '{}' is invalid for order {} (Active: {}, Qty: {}, Expired: {}, MinMet: {}). Not applied.",
                            couponCode, hoaDon.getMaHoaDon(),
                            "Hoạt động".equalsIgnoreCase(potentialCoupon.getTrangThai()),
                            potentialCoupon.getSoLuong() > 0,
                            (potentialCoupon.getNgayKetThuc() != null && potentialCoupon.getNgayKetThuc().isBefore(LocalDateTime.now())),
                            (potentialCoupon.getGiaTriToiThieuDonHang() == null || totalOrderAmountBeforeDiscount.compareTo(potentialCoupon.getGiaTriToiThieuDonHang()) >= 0));
                }
            } else {
                logger.warn("Coupon code '{}' not found. Not applied.", couponCode);
            }
        }

        BigDecimal finalTotalAmountWithoutShipping = totalOrderAmountBeforeDiscount.subtract(discountAmount).max(BigDecimal.ZERO);
        hoaDon.setTongTien(finalTotalAmountWithoutShipping); // Total item price after discount
        BigDecimal amountToPay = finalTotalAmountWithoutShipping.add(hoaDon.getPhiVanChuyenGhn()); // Final amount including shipping

        // Save HoaDon header *before* details to establish the relationship
        hoaDonRepository.save(hoaDon);
        logger.info("Saved HoaDon header. ID: {}, Code: {}", hoaDon.getHoaDonId(), hoaDon.getMaHoaDon());


        // 2. Create ChiTietHoaDon (Order Details) and Check AVAILABLE Stock
        // Does NOT deduct physical stock for standard orders at this stage.
        for (CartItemDTO item : cartItems) {
            if (item == null || item.getChiTietSanPhamId() == null || item.getSoLuong() == null || item.getSoLuong() <= 0) {
                logger.warn("Skipping invalid cart item: {}", item);
                continue; // Skip null or invalid items
            }

            Optional<ChiTietSanPham> chiTietSanPhamOpt = chiTietSanPhamRepository.findById(item.getChiTietSanPhamId());
            if (!chiTietSanPhamOpt.isPresent()) {
                logger.error("Chi tiết sản phẩm không tồn tại: ID {}", item.getChiTietSanPhamId());
                // Rollback transaction
                throw new Exception("Sản phẩm với ID " + item.getChiTietSanPhamId() + " không tồn tại trong giỏ hàng của bạn.");
            }
            ChiTietSanPham chiTietSanPham = chiTietSanPhamOpt.get();

            // --- AVAILABLE STOCK CHECK ---
            int availableStock = sanPhamService.calculateAvailableStock(chiTietSanPham);

            if (availableStock < item.getSoLuong()) {
                SanPham sanPham = chiTietSanPham.getSanPham(); // Get product from detail
                String productName = (sanPham != null && sanPham.getTenSanPham() != null) ? sanPham.getTenSanPham() : "Sản phẩm không xác định";
                String size = (chiTietSanPham.getKichCo() != null) ? " - Size " + chiTietSanPham.getKichCo().getTenKichCo() : "";
                String color = (chiTietSanPham.getMauSac() != null) ? " - Màu " + chiTietSanPham.getMauSac().getTenMauSac() : "";

                logger.error("Insufficient AVAILABLE stock for product '{}' (ID {}){}{} Requested: {}, Available: {}",
                        productName, item.getChiTietSanPhamId(), size, color, item.getSoLuong(), availableStock);
                // Rollback transaction
                throw new InsufficientStockException("Sản phẩm '" + productName + size + color + "' không đủ hàng tồn kho khả dụng. Chỉ còn " + availableStock + " sản phẩm.");
            }
            // --- END AVAILABLE STOCK CHECK ---

            // If stock is okay, create the order detail line
            ChiTietHoaDon chiTietHoaDon = new ChiTietHoaDon();
            chiTietHoaDon.setHoaDon(hoaDon); // Link to the saved HoaDon header
            chiTietHoaDon.setChiTietSanPham(chiTietSanPham);
            chiTietHoaDon.setSoLuong(item.getSoLuong());
            // Use the price from the cart item DTO, which should reflect the price at the time of adding to cart
            chiTietHoaDon.setGia(item.getGia() != null ? item.getGia() : chiTietSanPham.getGiaBan()); // Fallback to current price if DTO price is null
            chiTietHoaDonRepository.save(chiTietHoaDon);
            logger.debug("Saved ChiTietHoaDon for CTSP ID: {}, Qty: {}", chiTietSanPham.getChiTietSanPhamId(), item.getSoLuong());
        }

        // 3. Create PhuongThucThanhToan (Payment Method Record)
        PhuongThucThanhToan phuongThucThanhToan = new PhuongThucThanhToan();
        phuongThucThanhToan.setHoaDon(hoaDon);
        phuongThucThanhToan.setPhuongThuc(paymentMethod);
        phuongThucThanhToan.setNgayThanhToan(null); // Payment date set when actually paid
        phuongThucThanhToan.setTrangThai("Chưa thanh toán"); // Initial status
        phuongThucThanhToan.setSoTien(amountToPay); // Total amount including shipping and after discount
        phuongThucThanhToanRepository.save(phuongThucThanhToan);
        logger.info("Saved PhuongThucThanhToan for order {}. Method: {}, Amount: {}", hoaDon.getMaHoaDon(), paymentMethod, amountToPay);

        // 4. Reduce Coupon Quantity if Applied
        if (maGiamGia != null) { // Check if a valid coupon was actually applied
            // Fetch the latest state before updating to prevent race conditions if possible, though @Transactional helps
            Optional<MaGiamGia> freshCouponOpt = maGiamGiaRepository.findById(maGiamGia.getMaGiamGiaId());
            if(freshCouponOpt.isPresent()){
                MaGiamGia freshCoupon = freshCouponOpt.get();
                if(freshCoupon.getSoLuong() > 0) { // Double-check quantity before decrementing
                    freshCoupon.setSoLuong(freshCoupon.getSoLuong() - 1);
                    maGiamGiaRepository.save(freshCoupon);
                    logger.info("Reduced coupon quantity for code: {}, remaining: {}", freshCoupon.getTenMaGiamGia(), freshCoupon.getSoLuong());
                } else {
                    logger.warn("Coupon {} quantity became zero before reduction could be applied for order {}.", maGiamGia.getTenMaGiamGia(), hoaDon.getMaHoaDon());
                    // Optionally, throw an error here if using the coupon was critical and it ran out concurrently
                }
            } else {
                logger.error("Failed to re-fetch coupon ID {} before reducing quantity for order {}.", maGiamGia.getMaGiamGiaId(), hoaDon.getMaHoaDon());
                // Decide how to handle: proceed without reduction, or throw error? For now, log and proceed.
            }
        }

        logger.info("Finished placeOrder successfully for order ID: {}, orderCode: {}", hoaDon.getHoaDonId(), hoaDon.getMaHoaDon());
    }


    // Phương thức tạo đơn hàng ban đầu cho VNPAY (trước khi chuyển hướng)
    @Transactional(rollbackOn = Exception.class)
    public HoaDon createVnPayOrder(KhachHang khachHang, String orderCode, BigDecimal amount, String fullAddress,
                                   String couponCode, List<CartItemDTO> cartItems, BigDecimal shippingFee, String fullName, String phone) throws Exception {
        logger.info("Starting createVnPayOrder for customer ID: {}, orderCode: {}", khachHang != null ? khachHang.getKhachHangId() : "null", orderCode);
        if (khachHang == null) {
            throw new IllegalArgumentException("Khách hàng không được null.");
        }
        if (orderCode == null || orderCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã đơn hàng VNPAY không được rỗng.");
        }
        if (cartItems == null || cartItems.isEmpty()) {
            throw new IllegalArgumentException("Giỏ hàng không được rỗng.");
        }

        // 1. Create HoaDon (Order Header)
        HoaDon hoaDon = new HoaDon();
        hoaDon.setMaHoaDon(orderCode); // Use the code generated for VNPAY
        hoaDon.setKhachHang(khachHang);
        hoaDon.setSdtNhanHang(phone);
        hoaDon.setNguoiNhanHang(fullName);
        hoaDon.setNgayTaoHoaDon(LocalDateTime.now());
        hoaDon.setNgayBan(null); // Sale date set upon successful payment
        hoaDon.setKenhBanHang("Website");
        // Specific status indicating waiting for VNPAY payment confirmation
        hoaDon.setTrangThai("Chờ thanh toán VNPAY"); // Ensure this status is NOT counted by calculateAvailableStock unless intended
        hoaDon.setPhiVanChuyenGhn(shippingFee != null ? shippingFee : BigDecimal.ZERO);

        BigDecimal totalOrderAmountBeforeDiscount = calculateTotalOrderAmount(cartItems);

        // 1.1. Handle Shipping Address (Create temporary address for this VNPAY order)
        DiaChi diaChiGiaoHang = new DiaChi();
        diaChiGiaoHang.setKhachHang(khachHang);
        // Adapt parsing based on your confirmed address format
        diaChiGiaoHang.setGhiChu(getAddressPart(fullAddress, 3)); // e.g., Street/Detail
        diaChiGiaoHang.setPhuongXa(getAddressPart(fullAddress, 2)); // e.g., Ward
        diaChiGiaoHang.setQuanHuyen(getAddressPart(fullAddress, 1)); // e.g., District
        diaChiGiaoHang.setThanhPho(getAddressPart(fullAddress, 0)); // e.g., City
        diaChiGiaoHang.setMacDinh(false); // Not the default address
        diaChiRepository.save(diaChiGiaoHang);
        hoaDon.setDiaChi(diaChiGiaoHang);

        // 1.2. Apply Coupon AND Reduce Quantity Immediately
        MaGiamGia maGiamGia = null;
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (couponCode != null && !couponCode.trim().isEmpty()) {
            Optional<MaGiamGia> maGiamGiaOpt = maGiamGiaRepository.findByTenMaGiamGia(couponCode.trim());
            if (maGiamGiaOpt.isPresent()) {
                MaGiamGia potentialCoupon = maGiamGiaOpt.get();
                // Validate coupon: Active, Quantity, Expiry, Min Order Value
                if ("Hoạt động".equalsIgnoreCase(potentialCoupon.getTrangThai()) &&
                        potentialCoupon.getSoLuong() > 0 && // Must have quantity > 0 to be applied *and* reduced
                        (potentialCoupon.getNgayKetThuc() == null || potentialCoupon.getNgayKetThuc().isAfter(LocalDateTime.now())) &&
                        (potentialCoupon.getGiaTriToiThieuDonHang() == null || totalOrderAmountBeforeDiscount.compareTo(potentialCoupon.getGiaTriToiThieuDonHang()) >= 0))
                {
                    maGiamGia = potentialCoupon; // Valid coupon
                    discountAmount = calculateDiscountAmount(totalOrderAmountBeforeDiscount, maGiamGia);
                    hoaDon.setMaGiamGia(maGiamGia); // Associate with order

                    // Reduce coupon quantity *immediately* for VNPAY reservation
                    // Will be restored in updateOrderAfterPayment if payment fails
                    maGiamGia.setSoLuong(maGiamGia.getSoLuong() - 1);
                    maGiamGiaRepository.save(maGiamGia); // Save the reduced quantity
                    logger.info("Coupon '{}' validated, applied, and quantity reduced for pending VNPAY order {}. Discount: {}, Remaining Qty: {}",
                            couponCode, orderCode, discountAmount, maGiamGia.getSoLuong());
                } else {
                    // Log reason for invalidity but proceed without coupon
                    logger.warn("Coupon code '{}' is invalid for VNPAY order {} (Active: {}, Qty: {}, Expired: {}, MinMet: {}). Not applied or reduced.",
                            couponCode, orderCode,
                            "Hoạt động".equalsIgnoreCase(potentialCoupon.getTrangThai()),
                            potentialCoupon.getSoLuong() > 0,
                            (potentialCoupon.getNgayKetThuc() != null && potentialCoupon.getNgayKetThuc().isBefore(LocalDateTime.now())),
                            (potentialCoupon.getGiaTriToiThieuDonHang() == null || totalOrderAmountBeforeDiscount.compareTo(potentialCoupon.getGiaTriToiThieuDonHang()) >= 0));
                }
            } else {
                logger.warn("Coupon code '{}' not found for VNPAY order {}. Not applied.", couponCode, orderCode);
            }
        }

        BigDecimal finalTotalAmountWithoutShipping = totalOrderAmountBeforeDiscount.subtract(discountAmount).max(BigDecimal.ZERO);
        hoaDon.setTongTien(finalTotalAmountWithoutShipping); // Total item price after discount
        BigDecimal calculatedAmountToPay = finalTotalAmountWithoutShipping.add(hoaDon.getPhiVanChuyenGhn()); // Final amount including shipping

        // Sanity check: Compare calculated amount with the amount passed for VNPAY
        if (amount == null || calculatedAmountToPay.compareTo(amount) != 0) {
            logger.error("Amount mismatch for VNPAY order {}. Calculated: {}, Provided: {}", orderCode, calculatedAmountToPay, amount);
            // Decide handling: Throw error, use calculated amount, or log warning?
            // Throwing an error is safest to prevent incorrect payment amounts.
            throw new IllegalArgumentException("Tổng tiền tính toán (" + calculatedAmountToPay + ") không khớp với tổng tiền thanh toán VNPAY (" + amount + ").");
        }

        // Save HoaDon header *before* details
        hoaDonRepository.save(hoaDon);
        logger.info("Saved pending VNPAY HoaDon header. ID: {}, Code: {}", hoaDon.getHoaDonId(), hoaDon.getMaHoaDon());

        // 2. Create ChiTietHoaDon, Check AVAILABLE Stock, and Deduct PHYSICAL Stock
        for (CartItemDTO item : cartItems) {
            if (item == null || item.getChiTietSanPhamId() == null || item.getSoLuong() == null || item.getSoLuong() <= 0) {
                logger.warn("Skipping invalid cart item during VNPAY order creation: {}", item);
                continue; // Skip null or invalid items
            }

            // Use findById for better null handling and potential locking benefits in high concurrency
            Optional<ChiTietSanPham> chiTietSanPhamOpt = chiTietSanPhamRepository.findById(item.getChiTietSanPhamId());
            if (!chiTietSanPhamOpt.isPresent()) {
                logger.error("Chi tiết sản phẩm không tồn tại: ID {}", item.getChiTietSanPhamId());
                throw new Exception("Sản phẩm với ID " + item.getChiTietSanPhamId() + " không tồn tại trong giỏ hàng của bạn.");
            }
            ChiTietSanPham chiTietSanPham = chiTietSanPhamOpt.get();

            // --- AVAILABLE STOCK CHECK ---
            int availableStock = sanPhamService.calculateAvailableStock(chiTietSanPham);

            if (availableStock < item.getSoLuong()) {
                SanPham sanPham = chiTietSanPham.getSanPham();
                String productName = (sanPham != null && sanPham.getTenSanPham() != null) ? sanPham.getTenSanPham() : "Sản phẩm không xác định";
                String size = (chiTietSanPham.getKichCo() != null) ? " - Size " + chiTietSanPham.getKichCo().getTenKichCo() : "";
                String color = (chiTietSanPham.getMauSac() != null) ? " - Màu " + chiTietSanPham.getMauSac().getTenMauSac() : "";

                logger.error("Insufficient AVAILABLE stock for product '{}' (ID {}){}{} during VNPAY creation. Requested: {}, Available: {}",
                        productName, item.getChiTietSanPhamId(), size, color, item.getSoLuong(), availableStock);
                throw new InsufficientStockException("Sản phẩm '" + productName + size + color + "' không đủ hàng tồn kho khả dụng. Chỉ còn " + availableStock + " sản phẩm.");
            }
            // --- END AVAILABLE STOCK CHECK ---

            // If stock is okay, create the order detail line
            ChiTietHoaDon chiTietHoaDon = new ChiTietHoaDon();
            chiTietHoaDon.setHoaDon(hoaDon); // Link to the saved HoaDon header
            chiTietHoaDon.setChiTietSanPham(chiTietSanPham);
            chiTietHoaDon.setSoLuong(item.getSoLuong());
            chiTietHoaDon.setGia(item.getGia() != null ? item.getGia() : chiTietSanPham.getGiaBan()); // Use cart price, fallback to current
            chiTietHoaDonRepository.save(chiTietHoaDon);
            logger.debug("Saved ChiTietHoaDon for VNPAY order {}, CTSP ID: {}, Qty: {}", orderCode, chiTietSanPham.getChiTietSanPhamId(), item.getSoLuong());

            // --- DEDUCT PHYSICAL STOCK (soLuongTon) ---
            // This reserves the item. Will be restored if payment fails.
            int physicalStockBefore = chiTietSanPham.getSoLuongTon() != null ? chiTietSanPham.getSoLuongTon() : 0;
            int quantityToDeduct = item.getSoLuong();
            int newPhysicalStock = Math.max(0, physicalStockBefore - quantityToDeduct); // Prevent negative stock

            if (physicalStockBefore < quantityToDeduct) {
                // This should theoretically not happen if availableStock check passed, but acts as a safeguard.
                logger.error("CRITICAL: Physical stock ({}) is less than requested quantity ({}) for CTSP ID {} AFTER available stock check passed for VNPAY order {}. Potential race condition or data inconsistency.",
                        physicalStockBefore, quantityToDeduct, chiTietSanPham.getChiTietSanPhamId(), orderCode);
                // Throw exception to rollback transaction, as we cannot fulfill
                throw new IllegalStateException("Lỗi hệ thống: Không thể cập nhật số lượng tồn kho vật lý cho sản phẩm ID " + chiTietSanPham.getChiTietSanPhamId());
            }

            chiTietSanPham.setSoLuongTon(newPhysicalStock);
            chiTietSanPhamRepository.save(chiTietSanPham);
            logger.info("Reduced PHYSICAL stock for CTSP ID: {} for VNPAY order {}. Before: {}, After: {}, Reduced by: {}",
                    chiTietSanPham.getChiTietSanPhamId(), orderCode, physicalStockBefore, newPhysicalStock, quantityToDeduct);
            // --- END PHYSICAL STOCK DEDUCTION ---
        }

        // 3. Create PhuongThucThanhToan (Payment Method Record for VNPAY)
        PhuongThucThanhToan phuongThucThanhToan = new PhuongThucThanhToan();
        phuongThucThanhToan.setHoaDon(hoaDon);
        phuongThucThanhToan.setPhuongThuc("VNPAY");
        phuongThucThanhToan.setNgayThanhToan(null); // Set upon successful VNPAY callback
        phuongThucThanhToan.setTrangThai("Chờ VNPAY phản hồi"); // Initial status for VNPAY flow
        phuongThucThanhToan.setSoTien(calculatedAmountToPay); // The final, verified amount
        phuongThucThanhToanRepository.save(phuongThucThanhToan);
        logger.info("Saved PhuongThucThanhToan for VNPAY order {}. Amount: {}", orderCode, calculatedAmountToPay);

        logger.info("Finished createVnPayOrder successfully for order ID: {}, orderCode: {}. Ready for VNPAY redirection.", hoaDon.getHoaDonId(), orderCode);
        return hoaDon; // Return the created HoaDon object
    }

    // Phương thức cập nhật trạng thái đơn hàng sau khi VNPAY trả về kết quả
    @Transactional(rollbackOn = Exception.class)
    public void updateOrderAfterPayment(String orderCode, boolean paymentSuccess) {
        logger.info("Updating order after VNPAY payment callback for orderCode: {}, paymentSuccess: {}", orderCode, paymentSuccess);

        Optional<HoaDon> optionalHoaDon = hoaDonRepository.findByMaHoaDon(orderCode);

        if (optionalHoaDon.isPresent()) {
            HoaDon hoaDon = optionalHoaDon.get();

            // Lấy bản ghi thanh toán tương ứng
            List<PhuongThucThanhToan> payments = phuongThucThanhToanRepository.findByHoaDon(hoaDon);
            PhuongThucThanhToan payment = payments.isEmpty() ? null : payments.get(0); // Giả định chỉ có 1 PT TT cho 1 Hóa đơn

            if (paymentSuccess) {
                // --- THANH TOÁN THÀNH CÔNG ---
                logger.info("VNPAY payment successful for orderCode: {}. Updating status.", orderCode);

                // Cập nhật trạng thái thanh toán và hóa đơn
                if (payment != null) {
                    payment.setTrangThai("Đã thanh toán");
                    payment.setNgayThanhToan(LocalDateTime.now()); // Cập nhật ngày thanh toán thành công
                    phuongThucThanhToanRepository.save(payment);
                }
                hoaDon.setTrangThai("Đã thanh toán online"); // Hoặc trạng thái tiếp theo trong quy trình của bạn (VD: "Chờ xử lý")
                hoaDonRepository.save(hoaDon);

                // Quan trọng: Tồn kho và mã giảm giá đã được trừ ở bước createVnPayOrder.
                // Nếu thanh toán thành công, chúng ta không cần làm gì với tồn kho và mã giảm giá nữa.
                // Xóa giỏ hàng được xử lý ở Controller sau khi gọi service này thành công.

            } else {
                // --- THANH TOÁN THẤT BẠI HOẶC BỊ HỦY ---
                logger.warn("VNPAY payment failed or cancelled for orderCode: {}. Deleting order and restoring stock/coupon.", orderCode);

                // Lấy các bản ghi liên quan trước khi xóa
                List<ChiTietHoaDon> orderDetails = chiTietHoaDonRepository.findByHoaDon(hoaDon);
                DiaChi diaChi = hoaDon.getDiaChi();
                MaGiamGia maGiamGia = hoaDon.getMaGiamGia();


                // --- HOÀN TÁC CÁC THAY ĐỔI ĐÃ THỰC HIỆN Ở createVnPayOrder ---

                // 1. Hoàn trả tồn kho cho tất cả chi tiết hóa đơn
                if (!orderDetails.isEmpty()) {
                    logger.info("Restoring stock for orderCode: {} ({} items).", orderCode, orderDetails.size());
                    for (ChiTietHoaDon detail : orderDetails) {
                        ChiTietSanPham chiTietSanPham = detail.getChiTietSanPham();
                        int quantityToRestore = detail.getSoLuong();
                        if (chiTietSanPham != null) {
                            logger.debug("Restoring stock for ChiTietSanPham ID: {}, current stock: {}, quantity to restore: {}",
                                    chiTietSanPham.getChiTietSanPhamId(), chiTietSanPham.getSoLuongTon(), quantityToRestore);
                            chiTietSanPham.setSoLuongTon(chiTietSanPham.getSoLuongTon() + quantityToRestore);
                            chiTietSanPhamRepository.save(chiTietSanPham);
                            logger.debug("Stock restored for ChiTietSanPham ID: {}, updated stock: {}",
                                    chiTietSanPham.getChiTietSanPhamId(), chiTietSanPham.getSoLuongTon());
                        } else {
                            logger.warn("ChiTietSanPham with ID {} not found while trying to restore stock for orderCode {}", detail.getChiTietSanPham().getChiTietSanPhamId(), orderCode);
                        }
                    }
                    logger.info("Stock restoration complete for orderCode: {}", orderCode);
                }


                // 2. Hoàn trả số lượng mã giảm giá (nếu có)
                if (maGiamGia != null) {
                    logger.info("Restoring coupon usage for MaGiamGia ID: {}, current quantity: {}",
                            maGiamGia.getMaGiamGiaId(), maGiamGia.getSoLuong());
                    maGiamGia.setSoLuong(maGiamGia.getSoLuong() + 1);
                    maGiamGiaRepository.save(maGiamGia);
                    logger.info("Coupon usage restored for code: {}", maGiamGia.getTenMaGiamGia());
                } else {
                    logger.debug("No coupon to restore for orderCode: {}", orderCode);
                }

                // --- XÓA CÁC BẢN GHI ĐƠN HÀNG RÁC ---
                // Cần xóa các bản ghi con trước khi xóa bản ghi cha để tránh lỗi khóa ngoại.

                // Xóa bản ghi thanh toán
                if (payment != null) {
                    phuongThucThanhToanRepository.delete(payment);
                    logger.debug("Deleted PhuongThucThanhToan for orderCode: {}", orderCode);
                } else {
                    logger.warn("No PhuongThucThanhToan found for orderCode: {}. Cannot delete payment record.", orderCode);
                }

                // Xóa chi tiết hóa đơn
                if (!orderDetails.isEmpty()) {
                    chiTietHoaDonRepository.deleteAll(orderDetails); // Xóa tất cả các chi tiết cùng lúc
                    logger.debug("Deleted ChiTietHoaDon ({} records) for orderCode: {}", orderDetails.size(), orderCode);
                } else {
                    logger.debug("No ChiTietHoaDon found for orderCode: {}. Nothing to delete.", orderCode);
                }


                // Xóa địa chỉ giao hàng (địa chỉ này được tạo riêng cho đơn VNPAY trong createVnPayOrder)
                if (diaChi != null) {
                    // Chỉ xóa địa chỉ nếu nó được tạo mới và không phải là địa chỉ mặc định của khách hàng
                    // Logic hiện tại trong createVnPayOrder luôn tạo mới và set MacDinh=false nên có thể xóa an toàn
                    try {
                        diaChiRepository.delete(diaChi);
                        logger.debug("Deleted DiaChi for orderCode: {}", orderCode);
                    } catch (Exception e) {
                        // Log nếu có lỗi khi xóa địa chỉ (ví dụ: bị tham chiếu bởi bản ghi khác - trường hợp không mong muốn)
                        logger.error("Error deleting DiaChi ID {} for orderCode {}: {}", diaChi.getDiaChiId(), orderCode, e.getMessage(), e);
                    }

                } else {
                    logger.warn("No DiaChi found for orderCode: {}. Cannot delete address.", orderCode);
                }

                // Cuối cùng, xóa hóa đơn
                try {
                    hoaDonRepository.delete(hoaDon);
                    logger.info("Deleted HoaDon for orderCode: {} due to payment failure.", orderCode);
                } catch (Exception e) {
                    // Log nếu có lỗi khi xóa hóa đơn (ví dụ: vẫn còn bản ghi con tham chiếu)
                    logger.error("Error deleting HoaDon ID {} for orderCode {}: {}", hoaDon.getHoaDonId(), orderCode, e.getMessage(), e);
                    // Tùy chọn: Cập nhật lại trạng thái hóa đơn thành "Lỗi xóa" hoặc "Cần xử lý thủ công" nếu xóa thất bại
                    hoaDon.setTrangThai("Lỗi xóa đơn");
                    hoaDonRepository.save(hoaDon);
                }
            }
        } else {
            // Trường hợp không tìm thấy hóa đơn (có thể do callback VNPAY gọi lại nhiều lần
            // và đơn đã bị xóa ở lần xử lý trước, hoặc orderCode không hợp lệ)
            logger.warn("Order not found for orderCode: {} during payment return processing. It might have been deleted already or orderCode is incorrect.", orderCode);
        }
    }

    // Helper methods
    // Điều chỉnh logic này nếu format địa chỉ đầu vào của bạn khác
    private String getAddressPart(String fullAddress, int index) {
        if (fullAddress == null || fullAddress.isEmpty()) {
            return "";
        }
        // Giả định fullAddress có định dạng "Tỉnh/Thành phố, Quận/Huyện, Phường/Xã, Chi tiết nhà/đường"
        String[] parts = fullAddress.split(", ");
        if (index >= 0 && index < parts.length) {
            return parts[index].trim(); // Lấy theo index từ đầu mảng
        }
        logger.warn("Could not parse address part for index {} from address: {}", index, fullAddress);
        return "";
    }

    // Điều chỉnh phương thức tạo mã hóa đơn theo quy tắc của bạn
    private String generateOrderCode() {
        // Example: Simple timestamp + random suffix
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String randomSuffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return "HD" + timestamp + randomSuffix;
    }

    private BigDecimal calculateTotalOrderAmount(List<CartItemDTO> cartItems) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CartItemDTO item : cartItems) {
            // Kiểm tra item và giá trị không null trước khi tính
            if (item != null && item.getGia() != null && item.getSoLuong() != null) {
                totalAmount = totalAmount.add(item.getGia().multiply(BigDecimal.valueOf(item.getSoLuong())));
            } else {
                logger.warn("Cart item or its price/quantity is null: {}", item);
            }
        }
        return totalAmount;
    }

    private BigDecimal calculateDiscountAmount(BigDecimal totalAmount, MaGiamGia maGiamGia) {
        if (maGiamGia == null || maGiamGia.getGiaTriGiamGia() == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal discountAmount = BigDecimal.ZERO;
        if ("Phần trăm".equalsIgnoreCase(maGiamGia.getLoaiGiamGia())) {
            // Kiểm tra để tránh chia cho 0 và giá trị phần trăm hợp lệ
            if(maGiamGia.getGiaTriGiamGia().compareTo(BigDecimal.ZERO) > 0 && maGiamGia.getGiaTriGiamGia().compareTo(BigDecimal.valueOf(100)) <= 0) {
                BigDecimal discountPercent = maGiamGia.getGiaTriGiamGia().divide(BigDecimal.valueOf(100));
                discountAmount = totalAmount.multiply(discountPercent);
            } else {
                logger.warn("Invalid percentage discount value: {}", maGiamGia.getGiaTriGiamGia());
            }
        } else if ("FIXED".equalsIgnoreCase(maGiamGia.getLoaiGiamGia()) || "Số tiền cố định".equalsIgnoreCase(maGiamGia.getLoaiGiamGia())) {
            discountAmount = maGiamGia.getGiaTriGiamGia();
        }
        // Đảm bảo số tiền giảm giá không vượt quá tổng tiền đơn hàng
        return discountAmount.min(totalAmount);
    }

    public CouponResultDTO applyCoupon(String couponCode, BigDecimal subtotal, KhachHang khachHang) {
        MaGiamGia maGiamGia = maGiamGiaRepository.findByTenMaGiamGia(couponCode).orElse(null);

        if (maGiamGia == null) {
            logger.warn("Coupon code not found: {}", couponCode);
            return new CouponResultDTO(false, "Mã giảm giá không hợp lệ.", subtotal, BigDecimal.ZERO);
        }

        if (maGiamGia.getSoLuong() <= 0) {
            logger.warn("Coupon code used up: {}", couponCode);
            return new CouponResultDTO(false, "Mã giảm giá đã hết lượt sử dụng.", subtotal, BigDecimal.ZERO);
        }

        if (maGiamGia.getNgayKetThuc().isBefore(LocalDateTime.now())) {
            logger.warn("Coupon code expired: {} (Expiry: {})", couponCode, maGiamGia.getNgayKetThuc());
            return new CouponResultDTO(false, "Mã giảm giá đã hết hạn.", subtotal, BigDecimal.ZERO);
        }

        // Kiểm tra điều kiện áp dụng khác nếu có (ví dụ: chỉ áp dụng cho khách hàng mới, đơn hàng đầu tiên...)

        if (subtotal == null || subtotal.compareTo(BigDecimal.ZERO) < 0) {
            logger.warn("Invalid subtotal provided for coupon application: {}", subtotal);
            return new CouponResultDTO(false, "Tổng tiền đơn hàng không hợp lệ.", subtotal, BigDecimal.ZERO);
        }

        if (maGiamGia.getGiaTriToiThieuDonHang() != null && subtotal.compareTo(maGiamGia.getGiaTriToiThieuDonHang()) < 0) {
            logger.info("Subtotal {} is below minimum required for coupon {}. Min required: {}", subtotal, couponCode, maGiamGia.getGiaTriToiThieuDonHang());
            return new CouponResultDTO(false, "Đơn hàng chưa đạt giá trị tối thiểu (" + maGiamGia.getGiaTriToiThieuDonHang() + ").", subtotal, BigDecimal.ZERO);
        }

        BigDecimal discountAmount = calculateDiscountAmount(subtotal, maGiamGia);
        BigDecimal discountedTotal = subtotal.subtract(discountAmount).max(BigDecimal.ZERO);

        logger.info("Coupon '{}' applied successfully. Subtotal: {}, Discount: {}, Final Total: {}", couponCode, subtotal, discountAmount, discountedTotal);
        return new CouponResultDTO(true, "Mã giảm giá áp dụng thành công!", discountedTotal, discountAmount, couponCode);
    }

    public List<Map<String, Object>> getAvailableCoupons(KhachHang khachHang, BigDecimal subtotal) {
        // Lấy các mã giảm giá còn hạn sử dụng, còn số lượng và trạng thái "Hoạt động"
        List<MaGiamGia> availableCoupons = maGiamGiaRepository.findByNgayKetThucAfterAndSoLuongGreaterThanAndTrangThai(
                LocalDateTime.now(), 0, "Hoạt động");

        return availableCoupons.stream()
                // Lọc chỉ những mã đáp ứng giá trị đơn hàng tối thiểu
                .filter(coupon -> coupon.getGiaTriToiThieuDonHang() == null || subtotal.compareTo(coupon.getGiaTriToiThieuDonHang()) >= 0)
                // Lọc thêm các điều kiện khác nếu có (ví dụ: mã chỉ dành cho KH mới, chỉ áp dụng 1 lần/KH...)
                // .filter(coupon -> checkAdditionalCouponConditions(coupon, khachHang)) // Cần implement checkAdditionalCouponConditions
                .map(coupon -> {
                    Map<String, Object> couponInfo = new HashMap<>();
                    couponInfo.put("code", coupon.getTenMaGiamGia());
                    couponInfo.put("discountType", coupon.getLoaiGiamGia());
                    couponInfo.put("discountValue", coupon.getGiaTriGiamGia());
                    couponInfo.put("minOrderValue", coupon.getGiaTriToiThieuDonHang());
                    couponInfo.put("expiryDate", coupon.getNgayKetThuc());

                    // Tính số tiền giảm giá ước tính cho coupon này dựa trên subtotal hiện tại
                    BigDecimal discountAmount = calculateDiscountAmount(subtotal, coupon);
                    couponInfo.put("discountAmount", discountAmount);

                    return couponInfo;
                })
                .collect(Collectors.toList());
    }
    // (Có thể thêm các helper method khác nếu cần)
}