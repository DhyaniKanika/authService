package com.kd.signOn.repository;

import com.kd.signOn.model.UserStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserStatusHistoryRepository extends JpaRepository<UserStatusHistory, Long> {
}