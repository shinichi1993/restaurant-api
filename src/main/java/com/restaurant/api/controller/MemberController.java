package com.restaurant.api.controller;

import com.restaurant.api.dto.member.MemberRequest;
import com.restaurant.api.dto.member.MemberResponse;
import com.restaurant.api.service.MemberService;
import com.restaurant.api.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * MemberController – API quản lý hội viên Membership
 * ------------------------------------------------------------
 * Các API chính:
 *  - POST /api/members       : tạo/cập nhật hội viên
 *  - GET  /api/members/{id}  : lấy chi tiết hội viên
 *  - GET  /api/members/by-phone?phone=... : tìm hội viên theo SĐT
 */
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    /**
     * Lấy danh sách hội viên.
     * --------------------------------------------------------
     * - Mặc định trả về danh sách hội viên đang hoạt động
     *   nếu MemberService.getAll() dùng findByActiveTrue()
     */
    @GetMapping
    public ResponseEntity<List<MemberResponse>> getAll() {
        return ResponseEntity.ok(memberService.getAll());
    }

    /**
     * Tạo mới hoặc cập nhật hội viên.
     */
    @PostMapping
    public ResponseEntity<MemberResponse> save(@RequestBody MemberRequest req) {
        return ResponseEntity.ok(memberService.save(req));
    }

    /**
     * Lấy chi tiết hội viên theo ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<MemberResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(memberService.getById(id));
    }

    /**
     * Tìm hội viên theo số điện thoại.
     * --------------------------------------------------------
     * Dùng cho màn Order:
     *  - Nhập số điện thoại → tìm member → gán vào Order.memberId
     */
    @GetMapping("/by-phone")
    public ResponseEntity<List<MemberResponse>> searchByPhoneLike(
            @RequestParam String phone
    ) {
        return ResponseEntity.ok(memberService.searchByPhoneLike(phone));
    }

    /**
     * Tìm hội viên ACTIVE theo số điện thoại (POS).
     * --------------------------------------------------------
     * URL: GET /api/members/active/by-phone?phone=...
     *
     * Quy ước:
     *  - Chỉ trả về 1 hội viên
     *  - active = true
     *  - Nếu không tồn tại / đã disable → trả lỗi
     *
     * DÙNG RIÊNG CHO PAYMENT / POS
     */
    @GetMapping("/active/by-phone")
    public ResponseEntity<MemberResponse> getActiveByPhone(
            @RequestParam String phone
    ) {
        return ResponseEntity.ok(memberService.getActiveByPhone(phone));
    }

    /**
     * API tìm kiếm hội viên theo keyword LIKE
     * ---------------------------------------------------------
     * GET /api/members/search?keyword=0832
     * Trả LIST<MemberResponse>
     */
    @GetMapping("/search")
    public ResponseEntity<List<MemberResponse>> search(@RequestParam String keyword) {
        return ResponseEntity.ok(memberService.search(keyword));
    }

    /**
     * Vô hiệu hóa hội viên (soft delete).
     * --------------------------------------------------------
     * URL: PATCH /api/members/{id}/disable
     * - Dùng cho màn quản lý hội viên khi bấm nút "Vô hiệu hoá"
     */
    @PatchMapping("/{id}/disable")
    public ResponseEntity<String> disable(@PathVariable Long id) {
        memberService.disable(id);
        return ResponseEntity.ok("Đã vô hiệu hoá hội viên");
    }

    /**
     * Khôi phục hội viên đã bị vô hiệu hóa.
     * --------------------------------------------------------
     * URL: PATCH /api/members/{id}/restore
     * - Dùng khi muốn bật lại hội viên cũ
     */
    @PatchMapping("/{id}/restore")
    public ResponseEntity<String> restore(@PathVariable Long id) {
        memberService.restore(id);
        return ResponseEntity.ok("Đã khôi phục hội viên");
    }


}
