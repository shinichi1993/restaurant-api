package com.restaurant.api.repository;

import com.restaurant.api.entity.MemberPointHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * MemberPointHistoryRepository – thao tác lịch sử điểm
 */
public interface MemberPointHistoryRepository extends JpaRepository<MemberPointHistory, Long> {

    /**
     * Lấy lịch sử điểm của 1 member, mới nhất trước.
     */
    List<MemberPointHistory> findByMemberIdOrderByCreatedAtDesc(Long memberId);
}
