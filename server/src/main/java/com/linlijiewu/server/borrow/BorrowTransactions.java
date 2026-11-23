package com.linlijiewu.server.borrow;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.linlijiewu.server.common.BusinessException;
import com.linlijiewu.server.item.ItemMapper;
import com.linlijiewu.server.item.ItemRow;

@Service
public class BorrowTransactions {
    private final BorrowMapper borrows;
    private final ItemMapper items;
    private final BorrowQueries queries;
    private final ObjectMapper json;

    public BorrowTransactions(BorrowMapper borrows, ItemMapper items, BorrowQueries queries, ObjectMapper json) {
        this.borrows = borrows; this.items = items; this.queries = queries; this.json = json;
    }

    @Transactional
    public BorrowModels.Detail create(long borrower, BorrowModels.Create request, String requestId) {
        ItemRow item = items.lock(request.itemId());
        // The first consistent read is after the item lock, so same-item retries see the winner under MySQL RR.
        BorrowRow existing = borrows.findRequest(borrower, requestId);
        if (existing != null) { return queries.detail(existing.id, borrower); }
        if (item == null || "OFFLINE".equals(item.status)) { throw BusinessException.notFound(); }
        if (item.ownerId == borrower) { throw new BusinessException(409, 40903, "不能借用自己的物品"); }
        if (!"AVAILABLE".equals(item.status) || item.activeBorrowId != null) { throw BusinessException.conflict("物品当前不可借用"); }
        LocalDateTime now = BorrowQueries.now();
        if (request.dueAt().getYear() < 1000 || request.dueAt().getYear() > 9999) {
            throw BusinessException.badRequest("dueAt 年份必须在 1000–9999 内");
        }
        LocalDateTime dueAt = request.dueAt().atZoneSameInstant(BorrowQueries.ZONE).toLocalDateTime().truncatedTo(ChronoUnit.SECONDS);
        if (!dueAt.isAfter(now) || dueAt.getYear() < 1000 || dueAt.getYear() > 9999) {
            throw BusinessException.badRequest("dueAt 必须晚于当前时间且在数据库支持的年份内");
        }
        BorrowRow row = new BorrowRow();
        row.itemId = item.id; row.ownerId = item.ownerId; row.borrowerId = borrower;
        row.conversationId = borrows.conversation(item.id, borrower);
        row.status = "PENDING"; row.dueAt = dueAt; row.expiresAt = now.plusHours(12);
        row.note = request.note() == null ? "" : request.note(); row.requestId = requestId;
        row.createdAt = now; row.updatedAt = now; row.itemSnapshot = snapshot(item);
        borrows.insert(row);
        if (borrows.reserve(item.id, row.id) != 1) { throw BusinessException.conflict("物品当前不可借用"); }
        log(row.id, borrower, null, row.status, null, now);
        return queries.detail(row.id, borrower);
    }

    @Transactional
    public BorrowModels.Detail change(long id, long actor, String action, BorrowModels.Action request) {
        BorrowRow reference = BorrowQueries.requireParty(borrows.find(id), actor);
        ItemRow item = items.lock(reference.itemId);
        BorrowRow row = BorrowQueries.requireParty(borrows.lock(id), actor);
        boolean owner = row.ownerId == actor;
        LocalDateTime now = BorrowQueries.now();
        String from = row.status;
        String next;
        String itemStatus;
        boolean release = false;
        String cancelType = null;
        switch (action) {
            case "accept" -> {
                requireRole(owner);
                requireState(row, "PENDING");
                if (!row.expiresAt.isAfter(now)) { throw BusinessException.conflict("申请已超时，不能接受"); }
                next = "ACCEPTED"; itemStatus = "RESERVED";
            }
            case "reject" -> {
                requireRole(owner);
                requireState(row, "PENDING");
                next = "CANCELLED"; itemStatus = "AVAILABLE"; release = true; cancelType = "REJECTED";
            }
            case "cancel" -> {
                requireState(row, "PENDING", "ACCEPTED");
                next = "CANCELLED"; itemStatus = "AVAILABLE"; release = true;
                cancelType = owner ? "OWNER_CANCEL" : "BORROWER_CANCEL";
            }
            case "handover" -> {
                requireRole(owner);
                requireState(row, "ACCEPTED");
                next = "BORROWING"; itemStatus = "LENT";
            }
            case "return" -> {
                requireRole(!owner);
                requireState(row, "BORROWING");
                next = "RETURNING"; itemStatus = "LENT";
            }
            case "confirm-return" -> {
                requireRole(owner);
                requireState(row, "RETURNING");
                next = "COMPLETED"; itemStatus = "AVAILABLE"; release = true;
            }
            case "deny-return" -> {
                requireRole(owner);
                requireState(row, "RETURNING");
                next = "BORROWING"; itemStatus = "LENT";
            }
            default -> throw BusinessException.badRequest("不支持的订单操作");
        }
        String expectedItemStatus = switch (from) {
            case "PENDING", "ACCEPTED" -> "RESERVED";
            case "BORROWING", "RETURNING" -> "LENT";
            default -> throw BusinessException.conflict("订单已结束");
        };
        if (item == null || !Objects.equals(item.activeBorrowId, row.id) || !expectedItemStatus.equals(item.status)) {
            throw BusinessException.conflict("物品与活动订单不一致");
        }
        String reason = request == null || request.reason() == null ? null : request.reason().trim();
        if (reason != null && reason.isEmpty()) { reason = null; }
        row.status = next; row.cancelType = cancelType; row.cancelReason = cancelType == null ? null : reason; row.updatedAt = now;
        borrows.update(row);
        if (borrows.changeItem(row.itemId, row.id, itemStatus, release ? null : row.id) != 1) {
            throw BusinessException.conflict("活动订单已变化");
        }
        log(id, actor, from, next, reason, now);
        return queries.detail(id, actor);
    }

    private static void requireRole(boolean allowed) {
        if (!allowed) { throw BusinessException.forbidden(); }
    }

    private static void requireState(BorrowRow row, String... allowed) {
        for (String state : allowed) { if (state.equals(row.status)) { return; } }
        throw BusinessException.conflict("订单当前状态不允许此操作");
    }

    private String snapshot(ItemRow item) {
        try {
            return json.writeValueAsString(new BorrowModels.Item(item.id, item.title, item.imageUrl,
                    item.pickupLabel, item.pickupLat, item.pickupLng));
        } catch (JsonProcessingException exception) { throw new IllegalStateException("订单快照无法保存", exception); }
    }

    private void log(long id, long actor, String from, String to, String reason, LocalDateTime now) {
        BorrowLogRow log = new BorrowLogRow();
        log.borrowId = id; log.actorId = actor; log.fromStatus = from; log.toStatus = to;
        log.reason = reason; log.createdAt = now;
        borrows.insertLog(log);
    }
}
