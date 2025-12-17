package com.restaurant.api.repository;

import com.restaurant.api.entity.NotificationRuleLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationRuleLogRepository
        extends JpaRepository<NotificationRuleLog, Long> {

    Optional<NotificationRuleLog> findByRuleKey(String ruleKey);
}
