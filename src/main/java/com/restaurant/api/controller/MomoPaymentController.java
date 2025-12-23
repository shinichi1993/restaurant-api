package com.restaurant.api.controller;

import com.restaurant.api.dto.momo.MomoCreatePaymentRequest;
import com.restaurant.api.dto.momo.MomoCreatePaymentResponse;
import com.restaurant.api.dto.momo.MomoIpnRequest;
import com.restaurant.api.service.MomoPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/**
 * MomoPaymentController – FINAL
 * ------------------------------------------------------------------
 * API riêng cho thanh toán MoMo (ONLINE).
 */
@RestController
@RequestMapping("/api/payments/momo")
@RequiredArgsConstructor
public class MomoPaymentController {

    private final MomoPaymentService momoPaymentService;

    /**
     * FE gọi để tạo giao dịch MoMo.
     */
    @PostMapping("/create")
    public ResponseEntity<MomoCreatePaymentResponse> create(
            @RequestBody MomoCreatePaymentRequest req,
            Principal principal
    ) {
        return ResponseEntity.ok(
                momoPaymentService.createMomoPayment(req, principal.getName())
        );
    }

    /**
     * IPN – MoMo gọi server-to-server.
     * Theo MoMo: phải trả HTTP 204.
     */
    @PostMapping("/ipn")
    public ResponseEntity<Void> ipn(@RequestBody MomoIpnRequest ipn) {
        momoPaymentService.handleMomoIpn(ipn);
        return ResponseEntity.noContent().build();
    }
}
