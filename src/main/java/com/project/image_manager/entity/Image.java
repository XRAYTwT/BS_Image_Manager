package com.project.image_manager.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author XRAY
 * @date 2025/12/25 11:53
 * @Version 1.0
 * @SpecialThanksTo : Unknown
 */
@Entity
@Table(name = "images")
@Data
public class Image {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;  // 上传用户

    @Column(nullable = false)
    private String originalPath;  // 原图路径

    @Column
    private String thumbnailPath; // 缩略图路径

    @Column
    private LocalDateTime uploadTime = LocalDateTime.now();

    @Column(length = 1000)
    private String exifData;  // 存储 JSON 格式的 EXIF 信息

    @Column(length = 500)
    private String tags;  // 自定义标签，用逗号分隔，如 "风景,夜晚,杭州"

    // 方便显示的字段（可选）
    private String fileName;

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }
}