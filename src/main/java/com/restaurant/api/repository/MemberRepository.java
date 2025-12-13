package com.restaurant.api.repository;

import com.restaurant.api.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * MemberRepository – thao tác với bảng member
 */
public interface MemberRepository extends JpaRepository<Member, Long> {

    /**
     * Tìm hội viên theo số điện thoại (unique).
     */
    @Query("SELECT m FROM Member m WHERE m.phone LIKE %:phone%")
    List<Member> searchByPhoneLike(@Param("phone") String phone);

    /**
     * Tìm hội viên theo keyword (LIKE theo phone hoặc name)
     * ---------------------------------------------------------
     * Dùng cho màn MemberPage (search realtime)
     */
    @Query("SELECT m FROM Member m WHERE m.phone LIKE %:keyword% OR m.name LIKE %:keyword%")
    List<Member> search(@Param("keyword") String keyword);

    /**
     * Lấy danh sách hội viên đang hoạt động (active = true).
     * Dùng cho màn danh sách mặc định.
     */
    List<Member> findByActiveTrue();

    /**
     * Tìm hội viên theo SĐT và còn đang hoạt động.
     * Có thể dùng sau này nếu muốn chặn tìm hội viên đã disable.
     */
    Optional<Member> findByPhoneAndActiveTrue(String phone);
}
