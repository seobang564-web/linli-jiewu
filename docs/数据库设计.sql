-- 邻里借物地图 MySQL 8 建表脚本
-- 字符集 utf8mb4，时间统一存 Asia/Shanghai 本地时间（JVM 与 MySQL 时区一致设置为 +08:00）

CREATE DATABASE IF NOT EXISTS linli_jiewu DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE linli_jiewu;

CREATE TABLE users (
    id             BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username       VARCHAR(20)  NOT NULL,
    password_hash  VARCHAR(100) NOT NULL,
    nickname       VARCHAR(32)  NOT NULL,
    contact        VARCHAR(64)  NULL COMMENT '联系方式，仅在订单被接受后对双方可见',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_users_username (username)
) ENGINE=InnoDB;

CREATE TABLE items (
    id               BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    owner_id         BIGINT        NOT NULL,
    title            VARCHAR(50)   NOT NULL,
    category         VARCHAR(20)   NOT NULL,
    description      VARCHAR(500)  NOT NULL DEFAULT '',
    image_url        VARCHAR(255)  NULL,
    pickup_lat       DECIMAL(10,7) NOT NULL COMMENT 'GCJ-02',
    pickup_lng       DECIMAL(10,7) NOT NULL COMMENT 'GCJ-02',
    pickup_label     VARCHAR(100)  NOT NULL,
    status           VARCHAR(20)   NOT NULL DEFAULT 'AVAILABLE',
    active_borrow_id BIGINT        NULL COMMENT '当前进行中订单',
    created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_items_owner (owner_id, status),
    KEY idx_items_geo (status, pickup_lat, pickup_lng),
    CONSTRAINT fk_items_owner FOREIGN KEY (owner_id) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE TABLE borrows (
    id             BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    item_id        BIGINT       NOT NULL,
    owner_id       BIGINT       NOT NULL COMMENT '冗余物主，便于按身份查询',
    borrower_id    BIGINT       NOT NULL,
    conversation_id BIGINT      NULL,
    status         VARCHAR(20)  NOT NULL,
    due_at         DATETIME     NOT NULL COMMENT '预计归还时间',
    expires_at     DATETIME     NOT NULL COMMENT '待确认失效时间 = created_at + 12 小时',
    note           VARCHAR(200) NOT NULL DEFAULT '',
    cancel_type    VARCHAR(20)  NULL,
    cancel_reason  VARCHAR(200) NULL,
    request_id     VARCHAR(64)  NOT NULL COMMENT '客户端幂等号',
    item_snapshot  JSON         NOT NULL COMMENT '下单时物品标题、图片、交接点',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_borrows_request (borrower_id, request_id),
    KEY idx_borrows_borrower (borrower_id, status),
    KEY idx_borrows_owner (owner_id, status),
    KEY idx_borrows_expire (status, expires_at),
    KEY idx_borrows_item (item_id),
    CONSTRAINT fk_borrows_item FOREIGN KEY (item_id) REFERENCES items (id),
    CONSTRAINT fk_borrows_owner FOREIGN KEY (owner_id) REFERENCES users (id),
    CONSTRAINT fk_borrows_borrower FOREIGN KEY (borrower_id) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE TABLE borrow_logs (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    borrow_id   BIGINT       NOT NULL,
    actor_id    BIGINT       NULL COMMENT 'NULL 表示系统（超时任务）',
    from_status VARCHAR(20)  NULL,
    to_status   VARCHAR(20)  NOT NULL,
    reason      VARCHAR(200) NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_logs_borrow (borrow_id, id),
    CONSTRAINT fk_logs_borrow FOREIGN KEY (borrow_id) REFERENCES borrows (id)
) ENGINE=InnoDB;

CREATE TABLE conversations (
    id               BIGINT   NOT NULL AUTO_INCREMENT PRIMARY KEY,
    item_id          BIGINT   NOT NULL,
    owner_id         BIGINT   NOT NULL,
    borrower_id      BIGINT   NOT NULL,
    last_message     VARCHAR(100) NULL,
    last_message_at  DATETIME NULL,
    owner_unread     INT      NOT NULL DEFAULT 0,
    borrower_unread  INT      NOT NULL DEFAULT 0,
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_conv_item_borrower (item_id, borrower_id),
    KEY idx_conv_owner (owner_id, last_message_at),
    KEY idx_conv_borrower (borrower_id, last_message_at),
    CONSTRAINT fk_conv_item FOREIGN KEY (item_id) REFERENCES items (id),
    CONSTRAINT fk_conv_owner FOREIGN KEY (owner_id) REFERENCES users (id),
    CONSTRAINT fk_conv_borrower FOREIGN KEY (borrower_id) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE TABLE messages (
    id              BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT        NOT NULL,
    sender_id       BIGINT        NOT NULL,
    content         VARCHAR(1000) NOT NULL,
    client_msg_id   VARCHAR(64)   NOT NULL,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_msg_client (sender_id, client_msg_id),
    KEY idx_msg_conv (conversation_id, id),
    CONSTRAINT fk_msg_conv FOREIGN KEY (conversation_id) REFERENCES conversations (id),
    CONSTRAINT fk_msg_sender FOREIGN KEY (sender_id) REFERENCES users (id)
) ENGINE=InnoDB;

-- 附近检索参考 SQL（包围框粗筛，Java 中再用 Haversine 精确过滤与排序）
-- dLat = radius / 111320.0 ; dLng = radius / (111320.0 * cos(radians(lat)))
-- SELECT id, title, category, image_url, status, pickup_lat, pickup_lng, pickup_label
--   FROM items
--  WHERE status IN (#{statuses})
--    AND pickup_lat BETWEEN #{lat} - #{dLat} AND #{lat} + #{dLat}
--    AND pickup_lng BETWEEN #{lng} - #{dLng} AND #{lng} + #{dLng}
--    AND (#{keyword} IS NULL OR title LIKE CONCAT('%', #{keyword}, '%') OR description LIKE CONCAT('%', #{keyword}, '%'))
--    AND (#{category} IS NULL OR category = #{category});
