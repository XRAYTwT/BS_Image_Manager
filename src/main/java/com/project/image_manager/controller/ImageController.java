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
import net.coobird.thumbnailator.filters.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
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

    // 删除图片
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

    // 编辑图片
    @PostMapping("/edit")
    public ResponseEntity<String> editImage(@RequestParam Long imageId,
                                            @RequestParam(required = false) Integer rotate,  // 旋转
                                            @RequestParam(required = false) Double contrast,  // 对比度调整 0.5-2.0 (明显变化)
                                            @RequestParam(required = false) Double tint,  // 暖冷调
                                            @RequestParam(required = false) Double saturation,  // 饱和度
                                            @RequestParam(required = false) Integer cropX,
                                            @RequestParam(required = false) Integer cropY,
                                            @RequestParam(required = false) Integer cropWidth,
                                            @RequestParam(required = false) Integer cropHeight) {
        Optional<Image> imgOpt = imageRepository.findById(imageId);
        if (imgOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("图片不存在");
        }
        Image image = imgOpt.get();
        String originalPath = UPLOAD_DIR + image.getOriginalPath().substring("/uploads/".length());

        try {
            BufferedImage bufferedImage = ImageIO.read(new File(originalPath));

            // 旋转
            if (rotate != null) {
                bufferedImage = Thumbnails.of(bufferedImage).rotate(rotate).scale(1.0).asBufferedImage();
            }

            // 裁剪
            if (cropX != null && cropY != null && cropWidth != null && cropHeight != null) {
                bufferedImage = Thumbnails.of(bufferedImage).sourceRegion(cropX, cropY, cropWidth, cropHeight).scale(1.0).asBufferedImage();
            }

            // 对比度调整（明显变化！）
            if (contrast != null) {
                double clamped = Math.max(0.5, Math.min(2.0, contrast));
                RescaleOp op = new RescaleOp((float) clamped, 0, null);
                bufferedImage = op.filter(bufferedImage, null);
            }

            // 饱和度调整
            if (saturation != null) {
                double clamped = Math.max(0.0, Math.min(2.0, saturation));
                for (int y = 0; y < bufferedImage.getHeight(); y++) {
                    for (int x = 0; x < bufferedImage.getWidth(); x++) {
                        Color color = new Color(bufferedImage.getRGB(x, y));
                        float[] hsv = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
                        hsv[1] *= clamped;
                        if (hsv[1] > 1.0f) hsv[1] = 1.0f;
                        bufferedImage.setRGB(x, y, Color.HSBtoRGB(hsv[0], hsv[1], hsv[2]));
                    }
                }
            }

            // 色调偏移
            if (tint != null) {
                double clamped = Math.max(-1.0, Math.min(1.0, tint));
                double amount = Math.abs(clamped);
                Color tintColor = clamped > 0 ? new Color(255, 100, 0) : new Color(0, 100, 255);
                bufferedImage = Thumbnails.of(bufferedImage).addFilter(new Colorize(tintColor, (float) amount)).scale(1.0).asBufferedImage();
            }

            // 保存新图片
            String editedFileName = "edited_" + UUID.randomUUID() + "_" + image.getFileName();
            String editedPath = UPLOAD_DIR + editedFileName;
            ImageIO.write(bufferedImage, "jpg", new File(editedPath));

            // 生成缩略图
            String thumbFileName = "thumb_" + editedFileName;
            Thumbnails.of(editedPath).size(300, 300).keepAspectRatio(true).toFile(THUMB_DIR + thumbFileName);

            // 保存数据库
            Image newImage = new Image();
            newImage.setUser(image.getUser());
            newImage.setOriginalPath("/uploads/" + editedFileName);
            newImage.setThumbnailPath("/uploads/thumbs/" + thumbFileName);
            newImage.setFileName("edited_" + image.getFileName());
            newImage.setTags(image.getTags() + ",edited");
            newImage.setExifData(image.getExifData());
            imageRepository.save(newImage);

            return ResponseEntity.ok("编辑成功！新图片ID: " + newImage.getId());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("编辑失败: " + e.getMessage());
        }
    }

    // 添加标签
    @PutMapping("/{id}/tags")
    public ResponseEntity<String> addTags(@PathVariable Long id,
                                          @RequestParam String tags,  // 新标签，逗号分隔
                                          @RequestParam Long userId) {  // 验证权限
        Optional<Image> imgOpt = imageRepository.findById(id);
        if (imgOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("图片不存在");
        }
        Image image = imgOpt.get();
        if (!image.getUser().getId().equals(userId)) {
            return ResponseEntity.badRequest().body("无权限操作");
        }

        // 追加标签（去重，逗号分隔）
        String currentTags = image.getTags() == null ? "" : image.getTags();
        String[] newTags = tags.split(",");
        StringBuilder updatedTags = new StringBuilder(currentTags);

        for (String tag : newTags) {
            tag = tag.trim();
            if (!tag.isEmpty() && !currentTags.contains(tag)) {
                if (updatedTags.length() > 0) {
                    updatedTags.append(",");
                }
                updatedTags.append(tag);
            }
        }

        image.setTags(updatedTags.toString());
        imageRepository.save(image);

        return ResponseEntity.ok("标签添加成功！当前标签: " + image.getTags());
    }
}