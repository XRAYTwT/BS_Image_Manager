package com.project.image_manager.repository;

import com.project.image_manager.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @author XRAY
 * @date 2025/12/25 11:54
 * @Version 1.0
 * @SpecialThanksTo : Unknown
 */
public interface ImageRepository extends JpaRepository<Image, Long> {
    List<Image> findByUserId(Long userId);
}