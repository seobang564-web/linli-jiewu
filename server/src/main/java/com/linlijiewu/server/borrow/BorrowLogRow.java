package com.linlijiewu.server.borrow;

import java.time.LocalDateTime;

public class BorrowLogRow {
    public Long borrowId;
    public Long actorId;
    public String actorNickname;
    public String fromStatus;
    public String toStatus;
    public String reason;
    public LocalDateTime createdAt;
}
