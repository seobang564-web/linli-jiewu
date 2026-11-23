package com.linlijiewu.app.data.remote;

import java.util.List;

public final class BorrowData {
    public long id;
    public String status;
    public boolean overdue;
    public ItemData item;
    public Person owner;
    public Person borrower;
    public String dueAt;
    public String expiresAt;
    public String note;
    public String cancelType;
    public String cancelReason;
    public Long conversationId;
    public String createdAt;
    public String updatedAt;
    public static final class Person { public long id; public String nickname; public String contact; }
    public static final class Page { public long total; public int page; public int size; public List<BorrowData> list; }
    public static final class Log { public String fromStatus; public String toStatus; public Person actor; public String reason; public String createdAt; }
    public static final class Create { public long itemId; public String dueAt; public String note; public String requestId; }
    public static final class Action { public String reason; public Action(String reason) { this.reason = reason; } }
}
