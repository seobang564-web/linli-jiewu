package com.linlijiewu.server.borrow;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.linlijiewu.server.common.BusinessException;

@Service
public class BorrowQueries {
    static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final Set<String> CONTACT_VISIBLE = Set.of("ACCEPTED", "BORROWING", "RETURNING", "COMPLETED");
    private final BorrowMapper borrows;
    private final ObjectMapper json;

    public BorrowQueries(BorrowMapper borrows, ObjectMapper json) { this.borrows = borrows; this.json = json; }

    @Transactional(readOnly = true)
    public BorrowModels.Detail detail(long id, long viewer) {
        BorrowRow row = requireParty(borrows.find(id), viewer);
        return view(row, now());
    }

    @Transactional(readOnly = true)
    public BorrowModels.Page list(long viewer, String role, String status, int page, int size) {
        if (!"owner".equals(role) && !"borrower".equals(role)) { throw BusinessException.badRequest("role 必须为 owner 或 borrower"); }
        if (page < 1 || size < 1 || size > 50) { throw BusinessException.badRequest("page 必须从 1 开始，size 为 1–50"); }
        List<String> statuses = null;
        if (status != null) {
            try {
                statuses = Arrays.stream(status.split(",", -1)).map(String::trim)
                        .map(value -> BorrowModels.Status.valueOf(value).name()).distinct().toList();
            } catch (IllegalArgumentException exception) { throw BusinessException.badRequest("订单状态不正确"); }
        }
        boolean owner = "owner".equals(role);
        long total = borrows.count(viewer, owner, statuses);
        LocalDateTime now = now();
        var list = borrows.list(viewer, owner, statuses, size, (long) (page - 1) * size).stream()
                .map(row -> view(row, now)).toList();
        return new BorrowModels.Page(total, page, size, list);
    }

    @Transactional(readOnly = true)
    public List<BorrowModels.Log> logs(long id, long viewer) {
        requireParty(borrows.find(id), viewer);
        return borrows.logs(id).stream().map(row -> new BorrowModels.Log(row.fromStatus, row.toStatus,
                row.actorId == null ? null : new BorrowModels.Actor(row.actorId, row.actorNickname),
                row.reason, offset(row.createdAt))).toList();
    }

    static BorrowRow requireParty(BorrowRow row, long viewer) {
        if (row == null) { throw BusinessException.notFound(); }
        if (row.ownerId != viewer && row.borrowerId != viewer) { throw BusinessException.forbidden(); }
        return row;
    }

    static LocalDateTime now() { return LocalDateTime.now(ZONE).truncatedTo(ChronoUnit.SECONDS); }
    private static OffsetDateTime offset(LocalDateTime value) { return value.atZone(ZONE).toOffsetDateTime(); }

    private BorrowModels.Detail view(BorrowRow row, LocalDateTime now) {
        BorrowModels.Item item;
        try { item = json.readValue(row.itemSnapshot, BorrowModels.Item.class); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("订单快照无法读取", exception); }
        boolean visible = CONTACT_VISIBLE.contains(row.status);
        boolean overdue = !Set.of("COMPLETED", "CANCELLED").contains(row.status) && now.isAfter(row.dueAt);
        return new BorrowModels.Detail(row.id, row.status, overdue, item,
                new BorrowModels.Person(row.ownerId, row.ownerNickname, visible ? row.ownerContact : null),
                new BorrowModels.Person(row.borrowerId, row.borrowerNickname, visible ? row.borrowerContact : null),
                offset(row.dueAt), offset(row.expiresAt), row.note, row.cancelType, row.cancelReason,
                row.conversationId, offset(row.createdAt), offset(row.updatedAt));
    }
}
