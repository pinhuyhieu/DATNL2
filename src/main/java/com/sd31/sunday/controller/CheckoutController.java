package com.sd31.sunday.controller;

import com.sd31.sunday.DTO.CartItemDTO;
import com.sd31.sunday.DTO.CouponRequestDTO;
import com.sd31.sunday.DTO.CouponResultDTO;
import com.sd31.sunday.exception.InsufficientStockException;
import com.sd31.sunday.model.KhachHang;
import com.sd31.sunday.service.CheckoutService;
import com.sd31.sunday.service.EmailService;
import com.sd31.sunday.service.GHNService;
import com.sd31.sunday.service.GioHangService;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/checkout")
public class CheckoutController {

    private static final Logger logger = LoggerFactory.getLogger(CheckoutController.class);

    @Autowired
    private GioHangService gioHangService;

    @Autowired
    private CheckoutService checkoutService;

    @Autowired
    private GHNService ghnService;

    @Autowired
    private EmailService emailService;

    @GetMapping
    public String viewCheckoutPage(Model model, HttpSession session) {
        KhachHang khachHang = (KhachHang) session.getAttribute("loggedInUser");
        if (khachHang == null) {
            logger.warn("User not logged in, redirecting to login page.");
            return "redirect:/login";
        }

        List<CartItemDTO> cartItems = gioHangService.getCartItemsByKhachHangId(khachHang.getKhachHangId());
        if (cartItems.isEmpty()) {
            logger.warn("Cart is empty for user {}, redirecting to cart page.", khachHang.getKhachHangId());
            return "redirect:/gio-hang";
        }

        BigDecimal subtotal = gioHangService.calculateTotalPrice(cartItems);
        logger.info("Checkout page loaded for user {}. Subtotal: {}", khachHang.getKhachHangId(), subtotal);

        model.addAttribute("cartItems", cartItems);
        model.addAttribute("subtotal", subtotal);
        return "customer/checkout";
    }

    @PostMapping("/apply-coupon")
    @ResponseBody
    public CouponResultDTO applyCoupon(@RequestBody CouponRequestDTO requestDTO, HttpSession session) {
        logger.info("Applying coupon: {}", requestDTO);
        KhachHang khachHang = (KhachHang) session.getAttribute("loggedInUser");
        return checkoutService.applyCoupon(requestDTO.getCouponCode(), requestDTO.getSubtotal(), khachHang);
    }

    @GetMapping("/provinces")
    @ResponseBody
    public List<Map<String, Object>> getProvinces() {
        logger.info("Fetching provinces from GHN API.");
        return ghnService.getProvinces();
    }

    @GetMapping("/districts")
    @ResponseBody
    public List<Map<String, Object>> getDistricts(@RequestParam("provinceId") int provinceId) {
        logger.info("Fetching districts for provinceId: {}", provinceId);
        return ghnService.getDistricts(provinceId);
    }

    @GetMapping("/wards")
    @ResponseBody
    public List<Map<String, Object>> getWards(@RequestParam("districtId") int districtId) {
        logger.info("Fetching wards for districtId: {}", districtId);
        return ghnService.getWards(districtId);
    }


    // Thêm endpoint POST mới để truyền giỏ hàng
    @PostMapping("/calculate-shipping-fee")
    @ResponseBody
    public BigDecimal calculateShippingFee(
            @RequestParam("districtCode") int districtCode,
            @RequestParam("wardCode") String wardCode,
            @RequestBody List<CartItemDTO> cartItems) {
        logger.info("Calculating shipping fee for districtCode: {}, wardCode: {}, cartItems: {}", districtCode, wardCode, cartItems);
        try {
            BigDecimal shippingFee = ghnService.calculateShippingFee(districtCode, wardCode, cartItems);
            logger.info("Shipping fee calculated: {}", shippingFee);
            return shippingFee;
        } catch (Exception e) {
            logger.error("Error calculating shipping fee: {}", e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping("/place-order")
    public String placeOrder(
            @RequestParam("fullName") String fullName,
            @RequestParam("phone") String phone,
            @RequestParam("province") String provinceCode,
            @RequestParam("provinceName") String provinceName,
            @RequestParam("district") String districtCode,
            @RequestParam("districtName") String districtName,
            @RequestParam("ward") String wardCode,
            @RequestParam("wardName") String wardName,
            @RequestParam("detailedAddress") String detailedAddress,
            @RequestParam("paymentMethod") String paymentMethod,
            @RequestParam(value = "couponCode", required = false) String couponCode,
            @RequestParam("shippingFee") BigDecimal shippingFee,
            HttpSession session,
            Model model) {

        logger.info("Placing order with params: fullName={}, phone={}, provinceCode={}, districtCode={}, wardCode={}, shippingFee={}",
                fullName, phone, provinceCode, districtCode, wardCode, shippingFee);

        KhachHang khachHang = (KhachHang) session.getAttribute("loggedInUser");
        if (khachHang == null) {
            logger.warn("User not logged in, redirecting to login page.");
            return "redirect:/login";
        }

        List<CartItemDTO> cartItems = gioHangService.getCartItemsByKhachHangId(khachHang.getKhachHangId());
        if (cartItems.isEmpty()) {
            logger.warn("Cart is empty for user {}, redirecting to cart page.", khachHang.getKhachHangId());
            return "redirect:/gio-hang";
        }

        String fullAddress = provinceName + ", " + districtName + ", " + wardName + ", " + detailedAddress;

        try {
            // Đặt hàng
            checkoutService.placeOrder(
                    khachHang, fullName, phone, fullAddress,
                    paymentMethod, couponCode, cartItems, shippingFee
            );

            // Xóa giỏ hàng sau khi đặt hàng thành công
            gioHangService.clearCart(khachHang.getKhachHangId());


            model.addAttribute("successMessage", "Đặt hàng thành công!");
            logger.info("Order placed and confirmation email sent to user {}.", khachHang.getKhachHangId());
            return "customer/order-confirmation";

        } catch (InsufficientStockException e) {
            logger.error("Insufficient stock error: {}", e.getMessage());
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("cartItems", cartItems);
            BigDecimal subtotal = gioHangService.calculateTotalPrice(cartItems);
            model.addAttribute("subtotal", subtotal);
            return "customer/checkout";
        } catch (Exception e) {
            logger.error("Error placing order: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Có lỗi xảy ra khi đặt hàng: " + e.getMessage());
            model.addAttribute("cartItems", cartItems);
            BigDecimal subtotal = gioHangService.calculateTotalPrice(cartItems);
            model.addAttribute("subtotal", subtotal);
            return "customer/checkout";
        }
    }


    @GetMapping("/available-coupons")
    @ResponseBody
    public List<Map<String, Object>> getAvailableCoupons(HttpSession session) {
        KhachHang khachHang = (KhachHang) session.getAttribute("loggedInUser");
        if (khachHang == null) {
            return List.of();
        }
        BigDecimal subtotal = BigDecimal.ZERO;
        List<CartItemDTO> cartItems = gioHangService.getCartItemsByKhachHangId(khachHang.getKhachHangId());
        if (!cartItems.isEmpty()) {
            subtotal = gioHangService.calculateTotalPrice(cartItems);
        }
        return checkoutService.getAvailableCoupons(khachHang, subtotal);
    }
}