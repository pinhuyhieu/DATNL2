package com.sd31.sunday.service;

import com.sd31.sunday.DTO.VnPayPaymentDTO;
import com.sd31.sunday.config.VnpayConfig;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

@Service
public class VnPayService {

    @Autowired
    private VnpayConfig vnpayConfig;

    // Method để tạo URL thanh toán VNPAY
    public String createPaymentUrl(VnPayPaymentDTO paymentDTO, String ipAddress) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", vnpayConfig.getTmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(paymentDTO.getAmount() * 100)); // Số tiền nhân 100
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", paymentDTO.getOrderId()); // Mã đơn hàng của bạn
        vnpParams.put("vnp_OrderInfo", paymentDTO.getOrderInfo());
        vnpParams.put("vnp_OrderType", "other"); // Loại đơn hàng
        vnpParams.put("vnp_Locale", paymentDTO.getLocale());
        vnpParams.put("vnp_ReturnUrl", vnpayConfig.getReturnUrl());
        vnpParams.put("vnp_IpAddr", ipAddress);
        vnpParams.put("vnp_CreateDate", paymentDTO.getCreateDate()); // YYYYMMDDHHmmss

        // Sắp xếp các tham số theo key
        TreeMap<String, String> sortedVnpParams = new TreeMap<>(vnpParams);

        // Tạo chuỗi dữ liệu để ký
        StringBuilder signData = new StringBuilder();
        for (Map.Entry<String, String> entry : sortedVnpParams.entrySet()) {
            // Chỉ encode value
            signData.append(URLEncoder.encode(entry.getKey(), StandardCharsets.US_ASCII.toString()));
            signData.append('=');
            signData.append(URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII.toString()));
            signData.append('&');
        }
        signData.deleteCharAt(signData.length() - 1); // Xóa ký tự '&' cuối cùng

        // Tạo chữ ký HMAC SHA512
        String secureHash = hmacSHA512(vnpayConfig.getHashSecret(), signData.toString());

        // Xây dựng URL cuối cùng
        StringBuilder paymentUrl = new StringBuilder(vnpayConfig.getUrl());
        paymentUrl.append('?');
        for (Map.Entry<String, String> entry : sortedVnpParams.entrySet()) {
            paymentUrl.append(URLEncoder.encode(entry.getKey(), StandardCharsets.US_ASCII.toString()));
            paymentUrl.append('=');
            paymentUrl.append(URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII.toString()));
            paymentUrl.append('&');
        }
        paymentUrl.append("vnp_SecureHash=").append(secureHash);

        return paymentUrl.toString();
    }

    // Method để xác minh chữ ký (hash) từ callback VNPAY
    public boolean verifyVnPayCallback(HttpServletRequest request) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
        Map<String, String> vnpParams = new HashMap<>();
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String paramName = parameterNames.nextElement();
            vnpParams.put(paramName, request.getParameter(paramName));
        }

        String secureHash = vnpParams.get("vnp_SecureHash");
        if (secureHash == null) {
            return false; // Không có secureHash
        }

        vnpParams.remove("vnp_SecureHash");
        vnpParams.remove("vnp_SecureHashType"); // VNPAY 2.1.0 không dùng SecureHashType

        // Sắp xếp các tham số theo key
        TreeMap<String, String> sortedVnpParams = new TreeMap<>(vnpParams);

        // Tạo chuỗi dữ liệu để ký
        StringBuilder signData = new StringBuilder();
        for (Map.Entry<String, String> entry : sortedVnpParams.entrySet()) {
            // Chỉ encode value
            signData.append(URLEncoder.encode(entry.getKey(), StandardCharsets.US_ASCII.toString()));
            signData.append('=');
            signData.append(URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII.toString()));
            signData.append('&');
        }
        signData.deleteCharAt(signData.length() - 1); // Xóa ký tự '&' cuối cùng


        // Tạo chữ ký HMAC SHA512 và so sánh
        String myChecksum = hmacSHA512(vnpayConfig.getHashSecret(), signData.toString());

        return myChecksum.equalsIgnoreCase(secureHash);
    }

    // Hàm tạo HMAC SHA512 (copy từ VNPAY documentation)
    public String hmacSHA512(String key, String data) throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        Mac hmacSHA512 = Mac.getInstance("HmacSHA512");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        hmacSHA512.init(secretKey);
        byte[] hashBytes = hmacSHA512.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // Lấy địa chỉ IP của client (có thể cần hoặc không tùy môi trường setup)
    // Lưu ý: Trong môi trường thực tế, cần xử lý qua proxy, load balancer,...
    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    // Helper method to format date (copy from VNPAY demo JS)
    private String formatDate(java.util.Date date) {
        java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyyMMddHHmmss");
        return formatter.format(date);
    }
}