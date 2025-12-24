package com.project.image_manager.repository;

import com.project.image_manager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * @author XRAY
 * @date 2025/12/22 14:32
 * @Version 1.0
 * @SpecialThanksTo : Unknown
 */
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
}