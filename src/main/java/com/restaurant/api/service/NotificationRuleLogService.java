package com.restaurant.api.service;

import com.restaurant.api.entity.NotificationRuleLog;
import com.restaurant.api.repository.NotificationRuleLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Service xử lý logic chống spam cho Notification Rule
 */
@Service
@RequiredArgsConstructor
public class NotificationRuleLogService {

    private final NotificationRuleLogRepository repository;

    /**
     * Kiểm tra rule có được phép trigger hay không.
     *
     * @param ruleKey   key của rule (LOW_STOCK, REVENUE_ZERO...)
     * @param payload   chuỗi đại diện điều kiện (vd: threshold=10)
     */
    public boolean canTrigger(String ruleKey, String payload) {

        String payloadHash = String.valueOf(payload.hashCode());

        return repository.findByRuleKey(ruleKey)
                .map(log -> {
                    // Nếu đã trigger hôm nay + payload không đổi → CHẶN
                    boolean sameDay =
                            log.getLastTriggeredAt().toLocalDate()
                                    .isEqual(LocalDate.now());

                    boolean samePayload =
                            Objects.equals(log.getLastPayloadHash(), payloadHash);

                    return !(sameDay && samePayload);
                })
                .orElse(true); // Chưa có log → cho phép
    }

    /**
     * Ghi nhận rule đã trigger
     */
    public void markTriggered(String ruleKey, String payload) {

        String payloadHash = String.valueOf(payload.hashCode());

        NotificationRuleLog log = repository.findByRuleKey(ruleKey)
                .orElseGet(NotificationRuleLog::new);

        log.setRuleKey(ruleKey);
        log.setLastTriggeredAt(LocalDateTime.now());
        log.setLastPayloadHash(payloadHash);
        log.setUpdatedAt(LocalDateTime.now());

        if (log.getCreatedAt() == null) {
            log.setCreatedAt(LocalDateTime.now());
        }

        repository.save(log);
    }
}
