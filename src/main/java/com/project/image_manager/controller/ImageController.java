package com.project.image_manager.controller;



import com.drew.imaging.ImageMetadataReader;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.project.image_manager.entity.Image;
import com.project.image_manager.entity.User;
import com.project.image_manager.repository.ImageRepository;
import com.project.image_manager.repository.UserRepository;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author XRAY
 * @date 2025/12/25 11:55
 * @Version 1.0
 * @SpecialThanksTo : Unknown
 */
@RestController
@RequestMapping("/api/image")
public class ImageController {

    private static final Logger log = LoggerFactory.getLogger(ImageController.class);

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private UserRepository userRepository;

    private final String UPLOAD_DIR = "E:/image_uploads/";
    private final String THUMB_DIR = UPLOAD_DIR + "thumbs/";

    {
        new File(UPLOAD_DIR).mkdirs();
        new File(THUMB_DIR).mkdirs();
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadImage(@RequestParam("file") MultipartFile file,
                                              @RequestParam("userId") Long userId,
                                              @RequestParam(value = "tags", required = false) String tags) {
        try {
            log.info("收到上传请求，文件名: {}, 大小: {} bytes", file.getOriginalFilename(), file.getSize());

            if (file.isEmpty() || file.getSize() == 0) {
                return ResponseEntity.badRequest().body("文件为空或大小为0");
            }

            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return ResponseEntity.badRequest().body("用户不存在");
            }

            // 保存原图
            String originalFileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            String originalPath = UPLOAD_DIR + originalFileName;

            try (InputStream inputStream = file.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(originalPath)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalWritten = 0;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalWritten += bytesRead;
                }
                log.info("原图写入完成: {}，实际写入 {} bytes", originalPath, totalWritten);
            }

            // 生成缩略图
            String thumbFileName = "thumb_" + originalFileName;
            String thumbPath = THUMB_DIR + thumbFileName;
            try {
                Thumbnails.of(originalPath)
                        .size(300, 300)
                        .keepAspectRatio(true)
                        .toFile(thumbPath);
                log.info("缩略图生成成功: {}", thumbPath);
            } catch (Exception e) {
                log.warn("缩略图生成失败: {}", e.getMessage());
                thumbPath = null;
            }

            // 3. 提取 EXIF 信息并自动生成标签
            Map<String, String> exifMap = new HashMap<>();
            StringBuilder autoTags = new StringBuilder();  // 自动标签

            try {
                Metadata metadata = ImageMetadataReader.readMetadata(new File(originalPath));

                // 拍摄时间
                ExifSubIFDDirectory subDir = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
                if (subDir != null) {
                    if (subDir.containsTag(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)) {
                        String dateTime = subDir.getString(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
                        exifMap.put("拍摄时间", dateTime);
                        if (dateTime != null) {
                            autoTags.append(dateTime.substring(0, 10).replace(":", "-")).append(",");  // 2025-12-25
                        }
                    }
                }

                // GPS 地点
                GpsDirectory gpsDir = metadata.getFirstDirectoryOfType(GpsDirectory.class);
                if (gpsDir != null) {
                    GeoLocation geo = gpsDir.getGeoLocation();
                    if (geo != null && !geo.isZero()) {
                        String location = String.format("%.4f,%.4f", geo.getLatitude(), geo.getLongitude());
                        exifMap.put("拍摄地点", location);
                        autoTags.append("GPS定位,");
                    }
                }

                // 分辨率
                ExifIFD0Directory ifd0Dir = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
                if (ifd0Dir != null) {
                    Integer width = ifd0Dir.getInteger(ExifIFD0Directory.TAG_IMAGE_WIDTH);
                    Integer height = ifd0Dir.getInteger(ExifIFD0Directory.TAG_IMAGE_HEIGHT);
                    if (width != null && height != null) {
                        String resolution = width + "x" + height;
                        exifMap.put("分辨率", resolution);
                        autoTags.append(resolution).append(",");
                    }
                    if (ifd0Dir.containsTag(ExifIFD0Directory.TAG_MODEL)) {
                        exifMap.put("相机型号", ifd0Dir.getString(ExifIFD0Directory.TAG_MODEL));
                    }
                }

                if (exifMap.isEmpty()) {
                    exifMap.put("EXIF", "无EXIF信息");
                }
            } catch (Exception e) {
                exifMap.put("EXIF", "提取失败");
            }

            // 自动标签合并到自定义标签（如果有自定义标签则追加）
            String finalTags = tags != null ? tags : "";
            if (autoTags.length() > 0) {
                finalTags = finalTags.isEmpty() ? autoTags.toString() : finalTags + "," + autoTags.toString();
            }
            finalTags = finalTags.replaceAll(",$", "");  // 去掉末尾逗号



            // 保存数据库
            Image image = new Image();
            image.setUser(user);
            image.setOriginalPath("/uploads/" + originalFileName);
            image.setThumbnailPath(thumbPath != null ? "/uploads/thumbs/" + thumbFileName : null);
            image.setFileName(file.getOriginalFilename());
            image.setExifData(exifMap.toString());
            image.setTags(finalTags);  // 保存自动 + 自定义标签
            imageRepository.save(image);

            return ResponseEntity.ok("上传成功！标签: " + (tags == null ? "无" : tags) + "，EXIF: " + exifMap);
        } catch (Exception e) {
            log.error("上传失败", e);
            return ResponseEntity.badRequest().body("上传失败: " + e.getMessage());
        }
    }

    // 查询我的图片
    @GetMapping("/my")
    public List<Image> getMyImages(@RequestParam Long userId) {
        return imageRepository.findByUserId(userId);
    }

    // 按标签查询
    @GetMapping("/search")
    public List<Image> searchByTag(@RequestParam String tag) {
        return imageRepository.findAll().stream()
                .filter(img -> img.getTags() != null && img.getTags().contains(tag))
                .collect(Collectors.toList());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteImage(@PathVariable Long id, @RequestParam Long userId) {
        Optional<Image> imgOpt = imageRepository.findById(id);
        if (imgOpt.isEmpty() || !imgOpt.get().getUser().getId().equals(userId)) {
            return ResponseEntity.badRequest().body("图片不存在或无权限");
        }
        Image image = imgOpt.get();

        // 删除文件
        new File(UPLOAD_DIR + image.getOriginalPath().substring("/uploads/".length())).delete();
        if (image.getThumbnailPath() != null) {
            new File(THUMB_DIR + image.getThumbnailPath().substring("/uploads/thumbs/".length())).delete();
        }

        imageRepository.delete(image);
        return ResponseEntity.ok("删除成功");
    }
}