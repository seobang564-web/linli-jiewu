package com.linlijiewu.server.borrow;

import java.time.LocalDateTime;

public class BorrowRow {
    public Long id;
    public Long itemId;
    public Long ownerId;
    public Long borrowerId;
    public Long conversationId;
    public String status;
    public LocalDateTime dueAt;
    public LocalDateTime expiresAt;
    public String note;
    public String cancelType;
    public String cancelReason;
    public String requestId;
    public String itemSnapshot;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
    public String ownerNickname;
    public String ownerContact;
    public String borrowerNickname;
    public String borrowerContact;
}
