package com.sd31.sunday.service;

import com.sd31.sunday.DTO.CartItemDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GHNService {

    private static final Logger logger = LoggerFactory.getLogger(GHNService.class);

    @Value("${ghn.token}")
    private String token;

    @Value("${ghn.shopId}")
    private int shopId;

    @Value("${ghn.fromDistrictId}")
    private int fromDistrictId;

    private final RestTemplate restTemplate;

    public GHNService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /// Lấy danh sách tỉnh/thành
    public List<Map<String, Object>> getProvinces() {
        String url = "https://dev-online-gateway.ghn.vn/shiip/public-api/master-data/province";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Token", token);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        logger.info("Calling GHN API to get provinces: {}", url);
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            logger.info("Provinces API response: {}", response.getBody());
            return (List<Map<String, Object>>) response.getBody().get("data");
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.error("Error calling provinces API: Status {}, Response: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Lỗi khi lấy danh sách tỉnh/thành: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error calling provinces API: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi không xác định khi lấy danh sách tỉnh/thành: " + e.getMessage());
        }
    }

    // Lấy danh sách quận/huyện theo tỉnh
    public List<Map<String, Object>> getDistricts(int provinceId) {
        String url = "https://dev-online-gateway.ghn.vn/shiip/public-api/master-data/district";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Token", token);
        headers.set("Content-Type", "application/json");
        Map<String, Integer> body = new HashMap<>();
        body.put("province_id", provinceId);
        HttpEntity<String> entity;
        try {
            entity = new HttpEntity<>(new ObjectMapper().writeValueAsString(body), headers);
            logger.info("Calling GHN API to get districts with provinceId: {}, body: {}", provinceId, body);
        } catch (Exception e) {
            logger.error("Error creating request body for districts API: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi khi tạo request body: " + e.getMessage());
        }
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            logger.info("Districts API response: {}", response.getBody());
            return (List<Map<String, Object>>) response.getBody().get("data");
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.error("Error calling districts API: Status {}, Response: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Lỗi khi lấy danh sách quận/huyện: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error calling districts API: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi không xác định khi lấy danh sách quận/huyện: " + e.getMessage());
        }
    }

    // Lấy danh sách phường/xã theo quận/huyện
    public List<Map<String, Object>> getWards(int districtId) {
        String url = "https://dev-online-gateway.ghn.vn/shiip/public-api/master-data/ward";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Token", token);
        headers.set("Content-Type", "application/json");
        Map<String, Integer> body = new HashMap<>();
        body.put("district_id", districtId);
        HttpEntity<String> entity;
        try {
            entity = new HttpEntity<>(new ObjectMapper().writeValueAsString(body), headers);
            logger.info("Calling GHN API to get wards with districtId: {}, body: {}", districtId, body);
        } catch (Exception e) {
            logger.error("Error creating request body for wards API: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi khi tạo request body: " + e.getMessage());
        }
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            logger.info("Wards API response: {}", response.getBody());
            return (List<Map<String, Object>>) response.getBody().get("data");
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.error("Error calling wards API: Status {}, Response: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Lỗi khi lấy danh sách phường/xã: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error calling wards API: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi không xác định khi lấy danh sách phường/xã: " + e.getMessage());
        }
    }

    // Tính phí vận chuyển với số lượng từ giỏ hàng
    public BigDecimal calculateShippingFee(int toDistrictId, String toWardCode, List<CartItemDTO> cartItems) {
        String url = "https://dev-online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/fee";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Token", token);
        headers.set("ShopId", String.valueOf(shopId));
        headers.set("Content-Type", "application/json");

        // Tính tổng số lượng sản phẩm
        int totalQuantity = cartItems.stream().mapToInt(CartItemDTO::getSoLuong).sum();
        logger.info("Total quantity from cart items: {}", totalQuantity);

        // Giả định mỗi sản phẩm nặng 500g (có thể điều chỉnh tùy theo thực tế)
        int totalWeight = totalQuantity * 500; // Tổng trọng lượng (gram)

        Map<String, Object> body = new HashMap<>();
        body.put("service_id", 53321); // GHN Tiêu chuẩn
        body.put("from_district_id", fromDistrictId);
        body.put("to_district_id", toDistrictId);
        body.put("to_ward_code", toWardCode);
        body.put("weight", totalWeight); // Sử dụng tổng trọng lượng
        body.put("length", 15);
        body.put("width", 15);
        body.put("height", 15);

        HttpEntity<String> entity;
        try {
            entity = new HttpEntity<>(new ObjectMapper().writeValueAsString(body), headers);
            logger.info("Calling GHN API to calculate shipping fee: URL: {}, Body: {}", url, body);
        } catch (Exception e) {
            logger.error("Error creating request body for shipping fee API: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi khi tạo request body: " + e.getMessage());
        }

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            logger.info("Shipping fee API response: {}", response.getBody());
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null || !responseBody.containsKey("data")) {
                logger.error("Invalid response from GHN: Missing 'data' field");
                throw new RuntimeException("Phản hồi từ GHN không hợp lệ: Thiếu trường 'data'");
            }
            Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
            if (data == null || !data.containsKey("total")) {
                logger.error("Invalid response from GHN: Missing 'total' field in data");
                throw new RuntimeException("Phản hồi từ GHN không hợp lệ: Thiếu trường 'total'");
            }
            return BigDecimal.valueOf((Integer) data.get("total"));
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.error("Error calling shipping fee API: Status {}, Response: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Lỗi khi tính phí vận chuyển: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error calling shipping fee API: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi không xác định khi tính phí vận chuyển: " + e.getMessage());
        }
    }
}