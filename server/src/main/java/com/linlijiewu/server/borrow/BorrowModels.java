package com.linlijiewu.server.borrow;

import java.time.OffsetDateTime;
import java.util.List;
import jakarta.validation.constraints.*;

public final class BorrowModels {
    private BorrowModels() { }

    public enum Status { PENDING, ACCEPTED, BORROWING, RETURNING, COMPLETED, CANCELLED }

    public record Create(@NotNull @Positive Long itemId, @NotNull OffsetDateTime dueAt,
                         @Size(max = 200) String note,
                         @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,64}") String requestId) { }
    public record Action(@Size(max = 200) String reason) { }
    public record Item(long id, String title, String coverUrl, String pickupLabel,
                       double pickupLat, double pickupLng) { }
    public record Person(long id, String nickname, String contact) { }
    public record Detail(long id, String status, boolean overdue, Item item, Person owner, Person borrower,
                         OffsetDateTime dueAt, OffsetDateTime expiresAt, String note,
                         String cancelType, String cancelReason, Long conversationId,
                         OffsetDateTime createdAt, OffsetDateTime updatedAt) { }
    public record Actor(long id, String nickname) { }
    public record Log(String fromStatus, String toStatus, Actor actor, String reason, OffsetDateTime createdAt) { }
    public record Page(long total, int page, int size, List<Detail> list) { }
}
