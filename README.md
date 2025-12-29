# 图片管理网站

**《B/S体系软件设计》课程大作业**  
**作者**：黄文曦  
**学号**：3230105066  
**完成日期**：2025年12月29日  

## 项目简介

本项目是一个功能完整的图片管理网站，基于 Spring Boot 实现 B/S 架构，支持用户注册登录、图片上传存储、自动 EXIF 标签提取、自定义标签管理（添加/删除）、缩略图生成、标签搜索、图片编辑（旋转、裁剪、对比度/饱和度/色调调整）、删除、响应式展示等功能。

系统严格实现用户数据隔离，支持 PC 和手机浏览器访问，完美满足课程作业全部 11 条要求。采用前后端分离设计，前端纯 HTML + Bootstrap 实现响应式布局，后端使用 JPA 自动建表，Docker Compose 容器化部署数据库。

## 功能亮点

- 用户认证：JWT + BCrypt，登录自动返回 userId，无需手动输入
- 图片上传：支持大文件、自定义标签，自动生成缩略图
- EXIF 自动标签：提取拍摄时间、分辨率、相机型号、GPS 地点
- 标签管理：上传时添加、已有图片支持添加/删除标签（去重）
- 图片编辑：生成新图片保留原图，支持多种参数组合
- 查询搜索：按标签搜索，仅返回当前用户图片（严格隔离）
- 响应式展示：Bootstrap 网格 + Lightbox 轮播，手机自动单列适配
- 退出登录：一键清除凭证，返回登录页

## 技术栈

- **后端**：Spring Boot 3.x、Spring Data JPA、Spring Security、JWT、Thumbnailator、metadata-extractor
- **数据库**：MySQL 8.0
- **前端**：HTML5、Bootstrap 5、Lightbox2、原生 JavaScript
- **构建**：Maven
- **部署**：Docker Compose（数据库容器化）

## 项目结构
src/
main/
java/com/project/image_manager/     # 后端代码（实体、控制器、配置、测试文件）
resources/
static/                           # 前端页面 (index.html) 和资源
application.properties            # 数据库、路径配置
docker-compose.yml                    # MySQL 容器配置


### 1. 启动数据库（推荐 Docker Compose）

确保安装 Docker Desktop，在项目根目录执行：
docker-compose up -d

自动拉取 MySQL 8.0 镜像
创建数据库 image_manager
数据持久化到宿主机 volume

### 2. 启动网站应用

使用 IntelliJ IDEA 打开项目
运行 ImageManagerApplication.java
控制台显示 Started ImageManagerApplication 即成功（端口 8080）

### 3. 访问系统

PC 浏览器：http://localhost:8080/
手机浏览器（同局域网）：http://电脑IPv4地址:8080/
（Windows 查看：ipconfig → WLAN 的 IPv4 地址）

## 重要注意事项

### 图片存储路径
原图和缩略图保存在宿主机 E:/image_uploads/ 和 E:/image_uploads/thumbs/
必须手动创建这两个文件夹，并确保有读写权限，否则上传失败

### 防火墙设置
首次运行时，Windows 防火墙可能拦截 Java 或 8080 端口
请允许 “Java(TM) Platform SE binary” 通过专用和公用网络

### 手机访问适配
推荐使用电脑开启移动热点（设置 → 移动热点 → 频段改为 2.4GHz）
手机连接电脑热点后访问电脑 IP:8080
页面自动响应式适配，网格单列，支持相册上传和触屏操作

### 首次使用
注册任意用户名、邮箱（任意格式）、密码（≥6位）
登录后系统自动保存 userId，后续无需手动输入

### 数据安全
每个用户只能查看、操作自己的图片（严格隔离）
图片文件公开访问（符合图片网站设计）

### 清理环境
停止容器：docker-compose down
删除数据卷（慎用）：docker-compose down -v


### 功能演示
详细功能演示见提交包中的 演示视频.mp4
