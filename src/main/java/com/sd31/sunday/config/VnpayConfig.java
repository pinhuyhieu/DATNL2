package com.sd31.sunday.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VnpayConfig {

    @Value("${vnpay.tmnCode}")
    private String tmnCode;

    @Value("${vnpay.hashSecret}")
    private String hashSecret;

    @Value("${vnpay.url}")
    private String url;

    @Value("${vnpay.returnUrl}")
    private String returnUrl;

    // Getters
    public String getTmnCode() {
        return tmnCode;
    }

    public String getHashSecret() {
        return hashSecret;
    }

    public String getUrl() {
        return url;
    }

    public String getReturnUrl() {
        return returnUrl;
    }
}