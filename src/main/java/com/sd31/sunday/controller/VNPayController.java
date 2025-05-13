package com.sd31.sunday.controller;

import com.sd31.sunday.DTO.CartItemDTO;
import com.sd31.sunday.model.KhachHang;
import com.sd31.sunday.service.CheckoutService;
import com.sd31.sunday.service.GioHangService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Controller
@RequestMapping("/vnpay")
public class VNPayController {

    private static final Logger logger = LoggerFactory.getLogger(VNPayController.class);

    // VNPAY Configuration
    private static final String VNP_TMN_CODE = "EYGNG2ON";
    private static final String VNP_HASH_SECRET = "W2LN97CIB8GEOF3ZID5P76X9RM6FD5H7";
    private static final String VNP_PAY_URL = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Autowired
    private CheckoutService checkoutService;

    @Autowired
    private GioHangService gioHangService;

    @PostMapping("/create-payment")
    public String createPayment(
            @RequestParam("orderId") String orderId,
            @RequestParam("amount") BigDecimal amount,
            @RequestParam("orderInfo") String orderInfo,
            @RequestParam("fullName") String fullName,
            @RequestParam("phone") String phone,
            @RequestParam("provinceName") String provinceName,
            @RequestParam("districtName") String districtName,
            @RequestParam("wardName") String wardName,
            @RequestParam("detailedAddress") String detailedAddress,
            @RequestParam(value = "couponCode", required = false) String couponCode,
            @RequestParam("shippingFee") BigDecimal shippingFee,
            HttpServletRequest request,
            HttpSession session,
            Model model) {

        try {
            // Retrieve logged-in customer from session
            KhachHang khachHang = (KhachHang) session.getAttribute("loggedInUser");
            if (khachHang == null) {
                logger.warn("User not logged in, redirecting to login page.");
                return "redirect:/login";
            }

            // Retrieve cart items for the customer
            List<CartItemDTO> cartItems = gioHangService.getCartItemsByKhachHangId(khachHang.getKhachHangId());
            if (cartItems.isEmpty()) {
                logger.warn("Cart is empty for user {}, redirecting to cart page.", khachHang.getKhachHangId());
                return "redirect:/gio-hang";
            }

            // Construct full address
            String fullAddress = provinceName + ", " + districtName + ", " + wardName + ", " + detailedAddress;

            // Create VNPAY order with full details
            checkoutService.createVnPayOrder(khachHang, orderId, amount, fullAddress, couponCode, cartItems, shippingFee, fullName, phone);

            String vnp_Version = "2.1.0";
            String vnp_Command = "pay";
            String vnp_TxnRef = orderId;
            long amountInVND = amount.longValue() * 100; // Convert to VND (VNPAY requires amount * 100)
            String vnp_IpAddr = getClientIpAddress(request);
            String vnp_TmnCode = VNP_TMN_CODE;

            Map<String, String> vnp_Params = new HashMap<>();
            vnp_Params.put("vnp_Version", vnp_Version);
            vnp_Params.put("vnp_Command", vnp_Command);
            vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
            vnp_Params.put("vnp_Amount", String.valueOf(amountInVND));
            vnp_Params.put("vnp_CurrCode", "VND");

            // Generate order date
            Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            String vnp_CreateDate = formatter.format(cld.getTime());
            vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

            // Generate expiry date (15 minutes after order creation)
            cld.add(Calendar.MINUTE, 15);
            String vnp_ExpireDate = formatter.format(cld.getTime());
            vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

            vnp_Params.put("vnp_IpAddr", vnp_IpAddr);
            vnp_Params.put("vnp_Locale", "vn");
            vnp_Params.put("vnp_OrderInfo", orderInfo);
            vnp_Params.put("vnp_OrderType", "other");
            vnp_Params.put("vnp_ReturnUrl", baseUrl + "/vnpay/payment-return");
            vnp_Params.put("vnp_TxnRef", vnp_TxnRef);

            // Sort parameters by key
            List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
            Collections.sort(fieldNames);

            // Build hash data and create query string
            StringBuilder hashData = new StringBuilder();
            StringBuilder query = new StringBuilder();

            Iterator<String> itr = fieldNames.iterator();
            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = vnp_Params.get(fieldName);
                if ((fieldValue != null) && (!fieldValue.isEmpty())) {
                    hashData.append(fieldName);
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));

                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));

                    if (itr.hasNext()) {
                        query.append('&');
                        hashData.append('&');
                    }
                }
            }

            // Create secure hash
            String vnp_SecureHash = hmacSHA512(VNP_HASH_SECRET, hashData.toString());
            query.append("&vnp_SecureHash=").append(vnp_SecureHash);

            // Create payment URL
            String paymentUrl = VNP_PAY_URL + "?" + query.toString();
            logger.info("VNPAY Payment URL: {}", paymentUrl);

            return "redirect:" + paymentUrl;
        } catch (Exception e) {
            logger.error("Error creating VNPAY payment URL: {}", e.getMessage(), e);
            // Check if khachHang is available
            KhachHang khachHang = (KhachHang) session.getAttribute("loggedInUser");
            if (khachHang != null) {
                List<CartItemDTO> cartItems = gioHangService.getCartItemsByKhachHangId(khachHang.getKhachHangId());
                model.addAttribute("cartItems", cartItems);
                BigDecimal subtotal = gioHangService.calculateTotalPrice(cartItems);
                model.addAttribute("subtotal", subtotal);
            } else {
                model.addAttribute("cartItems", new ArrayList<CartItemDTO>());
                model.addAttribute("subtotal", BigDecimal.ZERO);
            }
            model.addAttribute("errorMessage", "Có lỗi xảy ra khi tạo thanh toán: " + e.getMessage());
            return "customer/checkout";
        }
    }

    @GetMapping("/payment-return")
    public String paymentReturn(HttpServletRequest request, Model model, HttpSession session) {
        Map<String, String> fields = new HashMap<>();
        for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements();) {
            String fieldName = params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if ((fieldValue != null) && (!fieldValue.isEmpty())) {
                fields.put(fieldName, fieldValue);
            }
        }

        // Remove vnp_SecureHash from fields to verify
        String vnp_SecureHash = request.getParameter("vnp_SecureHash");
        fields.remove("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");

        // Sort fields by fieldName
        String signValue = "";
        if (fields.size() > 0) {
            List<String> fieldNames = new ArrayList<>(fields.keySet());
            Collections.sort(fieldNames);

            StringBuilder hashData = new StringBuilder();
            Iterator<String> itr = fieldNames.iterator();
            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = fields.get(fieldName);
                if ((fieldValue != null) && (!fieldValue.isEmpty())) {
                    hashData.append(fieldName);
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));

                    if (itr.hasNext()) {
                        hashData.append('&');
                    }
                }
            }

            signValue = hmacSHA512(VNP_HASH_SECRET, hashData.toString());
        }

        // Verify signature
        boolean isValidSignature = signValue.equals(vnp_SecureHash);

        // Get payment info
        String orderId = request.getParameter("vnp_TxnRef");
        String vnp_ResponseCode = request.getParameter("vnp_ResponseCode");
        String vnp_Amount = request.getParameter("vnp_Amount");
        String vnp_PayDate = request.getParameter("vnp_PayDate");
        String vnp_BankCode = request.getParameter("vnp_BankCode");

        boolean isSuccess = "00".equals(vnp_ResponseCode);

        // Update order payment status in database if signature is valid
        if (isValidSignature) {
            try {
                // Update order status and handle inventory
                checkoutService.updateOrderAfterPayment(orderId, isSuccess);
                if (isSuccess) {
                    // Clear cart on successful payment
                    KhachHang khachHang = (KhachHang) session.getAttribute("loggedInUser");
                    if (khachHang != null) {
                        gioHangService.clearCart(khachHang.getKhachHangId());
                        logger.info("Cart cleared for user {} after successful VNPAY payment.", khachHang.getKhachHangId());
                    }
                }
            } catch (Exception e) {
                logger.error("Error updating payment status: {}", e.getMessage(), e);
                model.addAttribute("errorMessage", "Có lỗi xảy ra khi cập nhật trạng thái thanh toán: " + e.getMessage());
            }
        } else {
            logger.warn("Invalid signature for VNPAY payment return, orderId: {}", orderId);
            model.addAttribute("errorMessage", "Dữ liệu thanh toán không hợp lệ.");
        }

        // Add payment result to model
        model.addAttribute("isSuccess", isSuccess);
        model.addAttribute("orderId", orderId);
        model.addAttribute("amount", new BigDecimal(Long.parseLong(vnp_Amount)).divide(new BigDecimal(100)));
        model.addAttribute("payDate", vnp_PayDate);
        model.addAttribute("bankCode", vnp_BankCode);

        return "customer/vnpay-return";
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }

    private String hmacSHA512(String key, String data) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), "HmacSHA512");
            hmac.init(secretKeySpec);
            byte[] hmacBytes = hmac.doFinal(data.getBytes());
            return bytesToHex(hmacBytes);
        } catch (Exception e) {
            logger.error("Error generating HMAC SHA512: {}", e.getMessage(), e);
            return "";
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}