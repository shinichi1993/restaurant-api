package com.restaurant.api.service;

import com.restaurant.api.dto.member.MemberRequest;
import com.restaurant.api.dto.member.MemberResponse;
import com.restaurant.api.entity.Member;
import com.restaurant.api.entity.MemberPointHistory;
import com.restaurant.api.enums.MemberTier;
import com.restaurant.api.repository.MemberPointHistoryRepository;
import com.restaurant.api.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MemberService – Xử lý nghiệp vụ Membership & Loyalty
 * ------------------------------------------------------------
 * Chức năng chính:
 *  - Tạo / cập nhật hội viên
 *  - Tìm hội viên theo id / phone
 *  - Cộng điểm khi thanh toán (earn point)
 *  - (Chuẩn bị) Trừ điểm khi redeem
 *  - Tự động nâng hạng theo lifetimePoint
 *  - Lưu lịch sử cộng/trừ điểm (MemberPointHistory)
 */
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final MemberPointHistoryRepository memberPointHistoryRepository;
    private final SystemSettingService systemSettingService;

    // ============================================================
    // 1. CRUD CƠ BẢN
    // ============================================================

    /**
     * Tạo mới hoặc cập nhật hội viên từ request.
     * -------------------------------------------------------
     * - Nếu req.id == null  → tạo mới
     * - Nếu req.id != null  → cập nhật hội viên hiện có
     */
    @Transactional
    public MemberResponse save(MemberRequest req) {
        Member member;

        if (req.getId() == null) {
            // Tạo mới
            member = Member.builder()
                    .name(req.getName())
                    .phone(req.getPhone())
                    .email(req.getEmail())
                    .birthday(req.getBirthday())
                    .active(true)
                    .build();
        } else {
            // Cập nhật
            member = memberRepository.findById(req.getId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy hội viên"));
            member.setName(req.getName());
            member.setPhone(req.getPhone());
            member.setEmail(req.getEmail());
            member.setBirthday(req.getBirthday());
        }

        Member saved = memberRepository.save(member);
        return toResponse(saved);
    }

    /**
     * Tìm hội viên theo ID.
     */
    @Transactional(readOnly = true)
    public MemberResponse getById(Long id) {
        Member m = memberRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hội viên"));
        return toResponse(m);
    }

    /**
     * Tìm hội viên theo số điện thoại.
     */
    @Transactional(readOnly = true)
    public List<MemberResponse> searchByPhoneLike(String phone) {
        return memberRepository.searchByPhoneLike(phone)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Tìm hội viên theo keyword (trả LIST)
     * ---------------------------------------------------------
     * Dùng cho MemberPage để tìm theo LIKE
     */
    public List<MemberResponse> search(String keyword) {
        return memberRepository.search(keyword)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ============================================================
    // 2. EARN POINT KHI THANH TOÁN
    // ============================================================

    /**
     * Cộng điểm cho hội viên sau khi thanh toán 1 order.
     * -------------------------------------------------------
     * - Được gọi từ PaymentService.createPayment(...)
     * - earnedPoint thiết kế:
     *      + ĐÃ được tính sẵn ở PaymentService (dựa trên finalAmount)
     *      + Ở đây chỉ cộng vào member + lưu lịch sử + xử lý tier
     *
     * @param memberId      ID hội viên
     * @param earnedPoint   Số điểm cộng thêm
     * @param orderId       ID order/hóa đơn liên quan (dùng để trace)
     */
    @Transactional
    public void earnPoint(Long memberId, int earnedPoint, Long orderId) {
        if (earnedPoint <= 0) {
            return; // Không có điểm để cộng
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hội viên để cộng điểm"));

        int currentTotal = member.getTotalPoint() != null ? member.getTotalPoint() : 0;
        int currentLifetime = member.getLifetimePoint() != null ? member.getLifetimePoint() : 0;

        // Cập nhật điểm
        int newTotalPoint = currentTotal + earnedPoint;
        int newLifetimePoint = currentLifetime + earnedPoint;

        member.setTotalPoint(newTotalPoint);
        member.setLifetimePoint(newLifetimePoint);

        // Tự động tính lại tier dựa trên lifetimePoint
        MemberTier oldTier = member.getTier();
        MemberTier newTier = calculateTierByLifetimePoint(newLifetimePoint);
        member.setTier(newTier);

        memberRepository.save(member);

        // Lưu lịch sử EARN
        MemberPointHistory history = MemberPointHistory.builder()
                .memberId(memberId)
                .changeAmount(earnedPoint)
                .balanceAfter(newTotalPoint)
                .type("EARN")
                .orderId(orderId)
                .description("Cộng điểm khi thanh toán order " + orderId)
                .build();
        memberPointHistoryRepository.save(history);

        // Nếu tier thay đổi → sau này có thể:
        //  - Gửi notification
        //  - Gửi email chúc mừng
        // Ở đây tạm thời chỉ xử lý ở Service, phần Notification/Email
        // sẽ được bổ sung ở bước nâng cao (Option).
        if (oldTier != newTier) {
            // TODO: Gửi notification / email chúc mừng nâng hạng
        }
    }

    // ============================================================
    // 3. (OPTION) REDEEM POINT – SẼ DÙNG SAU
    // ============================================================

    /**
     * Trừ điểm khi hội viên dùng điểm để giảm giá.
     * -------------------------------------------------------
     * - ĐƯỢC gọi trực tiếp từ PaymentService.createPayment(...)
     * - redeemPoint đã được validate & anti-cheat trước đó
     */
    @Transactional
    public void redeemPoint(Long memberId, int redeemPoint, Long orderId) {
        if (redeemPoint <= 0) {
            return;
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hội viên để trừ điểm"));

        if (member.getTotalPoint() < redeemPoint) {
            throw new RuntimeException("Điểm hội viên không đủ để đổi thưởng");
        }

        int newTotalPoint = member.getTotalPoint() - redeemPoint;
        int newUsedPoint = member.getUsedPoint() + redeemPoint;

        member.setTotalPoint(newTotalPoint);
        member.setUsedPoint(newUsedPoint);

        memberRepository.save(member);

        MemberPointHistory history = MemberPointHistory.builder()
                .memberId(memberId)
                .changeAmount(-redeemPoint)
                .balanceAfter(newTotalPoint)
                .type("REDEEM")
                .orderId(orderId)
                .description("Dùng điểm để giảm giá cho order " + orderId)
                .build();
        memberPointHistoryRepository.save(history);
    }

    // ============================================================
    // 4. HÀM TÍNH TIER THEO lifetimePoint
    // ============================================================

    /**
     * Tính tier theo lifetimePoint dựa trên SystemSetting.
     * -------------------------------------------------------
     * Key cấu hình:
     *  - loyalty.tier.silver.min
     *  - loyalty.tier.gold.min
     *  - loyalty.tier.platinum.min
     */
    private MemberTier calculateTierByLifetimePoint(int lifetimePoint) {

        // Đọc ngưỡng điểm từ SystemSetting, nếu thiếu → dùng default
        int silverMin = systemSettingService.getNumberSetting(
                "loyalty.tier.silver.min",
                new BigDecimal("1000")
        ).intValue();

        int goldMin = systemSettingService.getNumberSetting(
                "loyalty.tier.gold.min",
                new BigDecimal("3000")
        ).intValue();

        int platinumMin = systemSettingService.getNumberSetting(
                "loyalty.tier.platinum.min",
                new BigDecimal("7000")
        ).intValue();

        if (lifetimePoint >= platinumMin) {
            return MemberTier.PLATINUM;
        } else if (lifetimePoint >= goldMin) {
            return MemberTier.GOLD;
        } else if (lifetimePoint >= silverMin) {
            return MemberTier.SILVER;
        } else {
            return MemberTier.BRONZE;
        }
    }

    // ============================================================
    // Lấy full data của member
    // ============================================================
    public List<MemberResponse> getAllByActiveTrue() {
        return memberRepository.findByActiveTrue()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ============================================================
    // Lấy full data của member
    // ============================================================
    public List<MemberResponse> getAll() {
        return memberRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Tìm hội viên ACTIVE theo số điện thoại.
     * -------------------------------------------------------
     * Dùng cho Payment / POS:
     *  - Chỉ trả về 1 hội viên duy nhất
     *  - BẮT BUỘC active = true
     *  - Nếu không tồn tại hoặc đã bị vô hiệu hóa → báo lỗi nghiệp vụ
     */
    @Transactional(readOnly = true)
    public MemberResponse getActiveByPhone(String phone) {

        Member member = memberRepository.findByPhoneAndActiveTrue(phone)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Không tìm thấy hội viên hoặc hội viên đã bị vô hiệu hóa"
                        )
                );

        return toResponse(member);
    }

    /**
     * Lấy entity Member theo ID (dùng nội bộ Service).
     * ------------------------------------------------------------
     * Dùng cho:
     *  - PaymentService: validate số điểm khi redeem
     *
     * @param memberId id hội viên
     * @return Member entity
     */
    @Transactional(readOnly = true)
    public Member getEntityById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hội viên"));
    }

    // ============================================================
    // 5. HÀM CHUYỂN ENTITY → DTO
    // ============================================================
    private MemberResponse toResponse(Member m) {
        return MemberResponse.builder()
                .id(m.getId())
                .name(m.getName())
                .phone(m.getPhone())
                .email(m.getEmail())
                .birthday(m.getBirthday())
                .tier(m.getTier())
                .active(m.getActive())
                .totalPoint(m.getTotalPoint())
                .lifetimePoint(m.getLifetimePoint())
                .usedPoint(m.getUsedPoint())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .build();
    }

    // ============================================================
    // 6. Disable / Restore hội viên (Soft delete)
    // ============================================================

    /**
     * Vô hiệu hóa hội viên (soft delete).
     * -------------------------------------------------------
     * - Không xóa dữ liệu khỏi DB
     * - Chỉ set active = false
     * - Sau này có thể cho phép khôi phục lại
     */
    @Transactional
    public void disable(Long id) {
        Member m = memberRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hội viên"));

        m.setActive(false);
        memberRepository.save(m);
    }

    /**
     * Khôi phục hội viên đã bị vô hiệu hóa.
     * -------------------------------------------------------
     * - Set lại active = true
     */
    @Transactional
    public void restore(Long id) {
        Member m = memberRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hội viên"));

        m.setActive(true);
        memberRepository.save(m);
    }

}
