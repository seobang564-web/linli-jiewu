-- H2 MySQL-mode fixtures; production schema and additive migration are in docs.
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(20) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL, nickname VARCHAR(32) NOT NULL, contact VARCHAR(64),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE images (
    image_id VARCHAR(64) PRIMARY KEY, owner_id BIGINT NOT NULL REFERENCES users(id),
    file_name VARCHAR(100) NOT NULL UNIQUE, size_bytes BIGINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY, owner_id BIGINT NOT NULL REFERENCES users(id),
    title VARCHAR(50) NOT NULL, category VARCHAR(20) NOT NULL, description VARCHAR(500) NOT NULL DEFAULT '',
    image_url VARCHAR(255), pickup_lat DECIMAL(10,7) NOT NULL, pickup_lng DECIMAL(10,7) NOT NULL,
    pickup_label VARCHAR(100) NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE', active_borrow_id BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE borrows (
    id BIGINT AUTO_INCREMENT PRIMARY KEY, item_id BIGINT NOT NULL REFERENCES items(id),
    owner_id BIGINT NOT NULL REFERENCES users(id), borrower_id BIGINT NOT NULL REFERENCES users(id),
    conversation_id BIGINT, status VARCHAR(20) NOT NULL, due_at DATETIME NOT NULL, expires_at DATETIME NOT NULL,
    note VARCHAR(200) NOT NULL DEFAULT '', cancel_type VARCHAR(20), cancel_reason VARCHAR(200),
    request_id VARCHAR(64) NOT NULL, item_snapshot VARCHAR(4096) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_borrows_request UNIQUE(borrower_id, request_id)
);
CREATE INDEX idx_borrows_borrower ON borrows(borrower_id, status);
CREATE INDEX idx_borrows_owner ON borrows(owner_id, status);
CREATE INDEX idx_borrows_item ON borrows(item_id);
CREATE TABLE borrow_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY, borrow_id BIGINT NOT NULL REFERENCES borrows(id),
    actor_id BIGINT REFERENCES users(id), from_status VARCHAR(20), to_status VARCHAR(20) NOT NULL,
    reason VARCHAR(200), created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE conversations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY, item_id BIGINT NOT NULL REFERENCES items(id),
    owner_id BIGINT NOT NULL REFERENCES users(id), borrower_id BIGINT NOT NULL REFERENCES users(id),
    last_message VARCHAR(100), last_message_at DATETIME, owner_unread INT NOT NULL DEFAULT 0,
    borrower_unread INT NOT NULL DEFAULT 0, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_conv_item_borrower UNIQUE(item_id, borrower_id)
);
CREATE TABLE messages (id BIGINT PRIMARY KEY);
