-- Minimal test fixtures for the read-only readiness query; MySQL DDL stays in docs.
CREATE TABLE users (id BIGINT PRIMARY KEY);
CREATE TABLE items (id BIGINT PRIMARY KEY);
CREATE TABLE borrows (id BIGINT PRIMARY KEY);
CREATE TABLE borrow_logs (id BIGINT PRIMARY KEY);
CREATE TABLE conversations (id BIGINT PRIMARY KEY);
CREATE TABLE messages (id BIGINT PRIMARY KEY);
