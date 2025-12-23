package com.restaurant.api.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class MomoConfig {

    @Value("${momo.mode}")
    private String mode; // sandbox | product

    @Value("${momo.sandbox.endpoint}")
    private String sandboxEndpoint;
    @Value("${momo.sandbox.partner-code}")
    private String sandboxPartnerCode;
    @Value("${momo.sandbox.access-key}")
    private String sandboxAccessKey;
    @Value("${momo.sandbox.secret-key}")
    private String sandboxSecretKey;
    @Value("${momo.sandbox.redirect-url}")
    private String sandboxRedirectUrl;
    @Value("${momo.sandbox.ipn-url}")
    private String sandboxIpnUrl;

    @Value("${momo.product.endpoint}")
    private String productEndpoint;
    @Value("${momo.product.partner-code}")
    private String productPartnerCode;
    @Value("${momo.product.access-key}")
    private String productAccessKey;
    @Value("${momo.product.secret-key}")
    private String productSecretKey;
    @Value("${momo.product.redirect-url}")
    private String productRedirectUrl;
    @Value("${momo.product.ipn-url}")
    private String productIpnUrl;

    // ===== Getter động theo mode =====

    public String endpoint() {
        return isProd() ? productEndpoint : sandboxEndpoint;
    }

    public String partnerCode() {
        return isProd() ? productPartnerCode : sandboxPartnerCode;
    }

    public String accessKey() {
        return isProd() ? productAccessKey : sandboxAccessKey;
    }

    public String secretKey() {
        return isProd() ? productSecretKey : sandboxSecretKey;
    }

    public String redirectUrl() {
        return isProd() ? productRedirectUrl : sandboxRedirectUrl;
    }

    public String ipnUrl() {
        return isProd() ? productIpnUrl : sandboxIpnUrl;
    }

    private boolean isProd() {
        return "product".equalsIgnoreCase(mode);
    }
}
