package com.restaurant.api.service;

import com.restaurant.api.config.MomoConfig;
import com.restaurant.api.dto.momo.MomoCreatePaymentRequest;
import com.restaurant.api.dto.momo.MomoCreatePaymentResponse;
import com.restaurant.api.dto.momo.MomoIpnRequest;
import com.restaurant.api.dto.payment.CalcPaymentRequest;
import com.restaurant.api.dto.payment.CalcPaymentResponse;
import com.restaurant.api.entity.Invoice;
import com.restaurant.api.entity.Order;
import com.restaurant.api.entity.Payment;
import com.restaurant.api.entity.User;
import com.restaurant.api.enums.*;
import com.restaurant.api.event.TableChangedEvent;
import com.restaurant.api.repository.OrderRepository;
import com.restaurant.api.repository.PaymentRepository;
import com.restaurant.api.repository.UserRepository;
import com.restaurant.api.util.MomoSignatureUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * MomoPaymentService – FINAL VERSION
 * ======================================================================
 * Nguyên tắc:
 *  - ONLINE PAYMENT (MoMo) KHÔNG dùng PaymentService.createPayment()
 *  - Tạo Payment trước ở trạng thái PENDING
 *  - Chỉ khi IPN SUCCESS mới:
 *      + Tạo Invoice
 *      + Gán invoice vào Payment
 *      + Set Order → PAID
 *      + Giải phóng bàn + bắn event realtime
 *
 * Rule nghiệp vụ:
 *  - 1 Order = 1 Payment
 *  - IPN là source of truth
 *  - Signature bắt buộc verify
 */
@Service
@RequiredArgsConstructor
public class MomoPaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    private final PaymentService paymentService; // dùng calc logic hiện có
    private final InvoiceService invoiceService;

    private final RestaurantTableService restaurantTableService;
    private final NotificationRuleService notificationRuleService;
    private final AuditLogService auditLogService;

    private final ApplicationEventPublisher applicationEventPublisher;

    private final MomoConfig momoConfig;

    private final RestTemplate restTemplate = new RestTemplate();

    // ======================================================
    // 1️⃣ TẠO GIAO DỊCH MOMO (PENDING)
    // ======================================================
    @Transactional
    public MomoCreatePaymentResponse createMomoPayment(
            MomoCreatePaymentRequest req,
            String username
    ) {
        // --------------------------------------------------
        // B1: Validate order
        // --------------------------------------------------
        Order order = orderRepository.findById(req.getOrderId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy order"));

        if (order.getStatus() == OrderStatus.PAID) {
            throw new RuntimeException("Order đã thanh toán");
        }
        if (order.getStatus() != OrderStatus.SERVING) {
            throw new RuntimeException("Chỉ order đang SERVING mới được thanh toán MoMo");
        }

        // --------------------------------------------------
        // B2: Anti-cheat – tính tiền bằng BE
        // --------------------------------------------------
        CalcPaymentResponse calc = paymentService.calcPayment(
                new CalcPaymentRequest(
                        req.getOrderId(),
                        req.getVoucherCode(),
                        req.getMemberId(),
                        req.getRedeemPoint()
                )
        );

        BigDecimal finalAmount =
                calc.getFinalAmount() != null ? calc.getFinalAmount() : BigDecimal.ZERO;

        if (req.getAmount() == null || req.getAmount().compareTo(finalAmount) != 0) {
            throw new RuntimeException("Số tiền thanh toán không khớp BE");
        }

        // --------------------------------------------------
        // B3: Lấy user
        // --------------------------------------------------
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        // --------------------------------------------------
        // B4: Tạo Payment PENDING
        // --------------------------------------------------
        Payment payment = Payment.builder()
                .order(order)
                .invoice(null)                 // ONLINE: chưa có invoice
                .amount(finalAmount)
                .customerPaid(finalAmount)     // ONLINE: coi như khách trả đúng
                .changeAmount(BigDecimal.ZERO)
                .method(PaymentMethod.MOMO)
                .note(req.getNote())
                .status(PaymentStatus.PENDING)
                .paidAt(LocalDateTime.now())
                .createdBy(user.getId())
                .build();

        paymentRepository.save(payment);

        // --------------------------------------------------
        // B5: Sinh momoOrderId / momoRequestId
        // --------------------------------------------------
        String momoOrderId = "PAY_" + payment.getId();
        String momoRequestId = "REQ_" + payment.getId() + "_" + System.currentTimeMillis();

        payment.setMomoOrderId(momoOrderId);
        payment.setMomoRequestId(momoRequestId);
        paymentRepository.save(payment);

        // --------------------------------------------------
        // B6: Ký request gửi MoMo
        // --------------------------------------------------
        String orderInfo = "Thanh toán order " + order.getOrderCode();
        String extraData = "";
        String requestType = "captureWallet";

        String rawData =
                "accessKey=" + momoConfig.accessKey() +
                        "&amount=" + finalAmount.setScale(0).toPlainString() +
                        "&extraData=" + extraData +
                        "&ipnUrl=" + momoConfig.ipnUrl() +
                        "&orderId=" + momoOrderId +
                        "&orderInfo=" + orderInfo +
                        "&partnerCode=" + momoConfig.partnerCode() +
                        "&redirectUrl=" + momoConfig.redirectUrl() +
                        "&requestId=" + momoRequestId +
                        "&requestType=" + requestType;

        String signature = MomoSignatureUtil.hmacSHA256(momoConfig.secretKey(), rawData);

        // --------------------------------------------------
        // B7: Call API MoMo
        // --------------------------------------------------
        Map<String, Object> body = new HashMap<>();
        body.put("partnerCode", momoConfig.partnerCode());
        body.put("accessKey", momoConfig.accessKey());
        body.put("requestId", momoRequestId);
        body.put("amount", finalAmount.setScale(0).longValue());
        body.put("orderId", momoOrderId);
        body.put("orderInfo", orderInfo);
        body.put("redirectUrl", momoConfig.redirectUrl());
        body.put("ipnUrl", momoConfig.ipnUrl());
        body.put("extraData", extraData);
        body.put("requestType", requestType);
        body.put("signature", signature);
        body.put("lang", "vi");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> res = restTemplate.exchange(
                momoConfig.endpoint(),
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class
        );

        Map<String, Object> momoRes = res.getBody();
        if (momoRes == null) {
            throw new RuntimeException("MoMo trả về dữ liệu rỗng");
        }

        // --------------------------------------------------
        // B8: Lưu link trả về
        // --------------------------------------------------
        payment.setMomoPayUrl(getString(momoRes, "payUrl"));
        payment.setMomoQrCodeUrl(getString(momoRes, "qrCodeUrl"));
        payment.setMomoDeeplink(getString(momoRes, "deeplink"));
        paymentRepository.save(payment);

        return MomoCreatePaymentResponse.builder()
                .paymentId(payment.getId())
                .momoOrderId(momoOrderId)
                .momoRequestId(momoRequestId)
                .payUrl(payment.getMomoPayUrl())
                .qrCodeUrl(payment.getMomoQrCodeUrl())
                .deeplink(payment.getMomoDeeplink())
                .message(getString(momoRes, "message"))
                .build();
    }

    // ======================================================
    // 2️⃣ IPN – MOMO CALLBACK (SOURCE OF TRUTH)
    // ======================================================
    @Transactional
    public void handleMomoIpn(MomoIpnRequest ipn) {

        // --------------------------------------------------
        // B1: Tìm payment theo momoOrderId
        // --------------------------------------------------
        Payment payment = paymentRepository.findByMomoOrderId(ipn.getOrderId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy payment theo momoOrderId"));

        // Idempotent
        if (payment.getStatus() == PaymentStatus.SUCCESS
                || payment.getStatus() == PaymentStatus.FAILED
                || payment.getStatus() == PaymentStatus.CANCELED) {
            return;
        }

        // --------------------------------------------------
        // B2: Verify signature IPN
        // --------------------------------------------------
        String rawData =
                "accessKey=" + momoConfig.accessKey() +
                        "&amount=" + ipn.getAmount().setScale(0).toPlainString() +
                        "&extraData=" + safe(ipn.getExtraData()) +
                        "&message=" + safe(ipn.getMessage()) +
                        "&orderId=" + ipn.getOrderId() +
                        "&orderInfo=" + safe(ipn.getOrderInfo()) +
                        "&orderType=" + safe(ipn.getOrderType()) +
                        "&partnerCode=" + ipn.getPartnerCode() +
                        "&payType=" + safe(ipn.getPayType()) +
                        "&requestId=" + safe(ipn.getRequestId()) +
                        "&responseTime=" + ipn.getResponseTime() +
                        "&resultCode=" + ipn.getResultCode() +
                        "&transId=" + ipn.getTransId();

        String expectedSignature = MomoSignatureUtil.hmacSHA256(momoConfig.secretKey(), rawData);

        if (!expectedSignature.equalsIgnoreCase(ipn.getSignature())) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setMomoMessage("Sai chữ ký IPN");
            paymentRepository.save(payment);
            return;
        }

        // --------------------------------------------------
        // B3: Validate amount
        // --------------------------------------------------
        if (payment.getAmount().compareTo(ipn.getAmount()) != 0) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setMomoMessage("Amount IPN không khớp");
            paymentRepository.save(payment);
            return;
        }

        // --------------------------------------------------
        // B4: Lưu dữ liệu IPN
        // --------------------------------------------------
        payment.setMomoTransId(ipn.getTransId());
        payment.setMomoResultCode(ipn.getResultCode());
        payment.setMomoMessage(ipn.getMessage());
        payment.setMomoPayType(ipn.getPayType());
        payment.setMomoResponseTime(ipn.getResponseTime());
        payment.setMomoExtraData(ipn.getExtraData());

        // --------------------------------------------------
        // B5: Xử lý theo resultCode
        // --------------------------------------------------
        if (ipn.getResultCode() != null && ipn.getResultCode() == 0) {
            payment.setStatus(PaymentStatus.SUCCESS);
            paymentRepository.save(payment);
            finalizeSuccess(payment);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
        }
    }

    // ======================================================
    // 3️⃣ FINALIZE – CHỈ KHI MOMO SUCCESS
    // ======================================================
    private void finalizeSuccess(Payment payment) {

        Order order = payment.getOrder();

        if (order.getStatus() == OrderStatus.PAID) {
            return;
        }

        // Tính lại snapshot tiền để tạo invoice
        CalcPaymentResponse calc = paymentService.calcPayment(
                new CalcPaymentRequest(order.getId(), null, order.getMemberId(), 0)
        );

        Invoice invoice = invoiceService.createInvoiceFromOrder(
                order.getId(),
                PaymentMethod.MOMO,
                calc.getAppliedVoucherCode(),
                calc.getOrderTotal(),
                calc.getVoucherDiscount(),
                calc.getDefaultDiscount(),
                calc.getTotalDiscount(),
                calc.getAmountAfterDiscount(),
                calc.getVatPercent(),
                calc.getVatAmount(),
                payment.getAmount(),
                calc.getLoyaltyEarnedPoint(),
                payment.getAmount(),
                BigDecimal.ZERO
        );

        payment.setInvoice(invoice);
        paymentRepository.save(payment);

        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);

        if (order.getTable() != null) {
            Long tableId = order.getTable().getId();
            restaurantTableService.markTableAvailable(tableId);
            applicationEventPublisher.publishEvent(
                    new TableChangedEvent(tableId, PosTableChangeReason.PAYMENT_DONE)
            );
        }

        notificationRuleService.onPaymentSuccess(payment);

        auditLogService.log(
                AuditAction.PAYMENT_CREATE,
                "payment",
                payment.getId(),
                null,
                payment
        );
    }

    // ======================================================
    // UTIL
    // ======================================================
    private String getString(Map<String, Object> map, String key) {
        return map.get(key) != null ? map.get(key).toString() : null;
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
