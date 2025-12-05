package com.restaurant.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * InvoiceItem – Chi tiết từng món trong hóa đơn
 * ------------------------------------------------------------
 * Ghi lại snapshot thông tin món tại thời điểm thanh toán:
 *  - dishId      : ID món (FK)
 *  - dishName    : tên món
 *  - dishPrice   : giá món
 *  - quantity    : số lượng mua
 *  - subtotal    : thành tiền (price × quantity)
 *
 * Thuộc về Invoice (quan hệ nhiều–1).
 *
 * createdAt:
 *  - tự sinh tại thời điểm tạo bản ghi
 * ------------------------------------------------------------
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "invoice_item")
public class InvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Khóa chính

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice; // FK → invoice.id

    @Column(name = "dish_id", nullable = false)
    private Long dishId; // ID món ăn

    @Column(name = "dish_name", nullable = false)
    private String dishName; // Tên món snapshot

    @Column(name = "dish_price", nullable = false)
    private BigDecimal dishPrice; // Giá món snapshot

    @Column(nullable = false)
    private Integer quantity; // Số lượng món

    @Column(nullable = false)
    private BigDecimal subtotal; // Thành tiền = dishPrice × quantity

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt; // Thời điểm tạo bản ghi

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
