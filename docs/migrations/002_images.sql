-- 第 2 周：在已有 linli_jiewu 库新增图片元数据，不修改或清空原有六张表。
-- 在部署新 JAR 前执行：mysql -u <有建表权限的用户> -p linli_jiewu < 002_images.sql
-- 可重复执行。若 images 已由其他版本创建，须核对 SHOW CREATE TABLE images。
USE linli_jiewu;
CREATE TABLE IF NOT EXISTS images (
    image_id       VARCHAR(64)  NOT NULL PRIMARY KEY,
    owner_id       BIGINT       NOT NULL,
    file_name      VARCHAR(100) NOT NULL,
    size_bytes     BIGINT       NOT NULL,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_images_file_name (file_name),
    KEY idx_images_owner (owner_id),
    CONSTRAINT fk_images_owner FOREIGN KEY (owner_id) REFERENCES users (id)
) ENGINE=InnoDB;
