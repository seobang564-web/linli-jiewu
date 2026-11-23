package com.linlijiewu.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class BorrowStateIntegrationTest extends BorrowTestSupport {
    private static final List<String> ACTIONS = List.of("accept", "reject", "cancel", "handover", "return", "confirm-return", "deny-return");

    @Test
    void fullCycleIncludingDeniedReturnHasOrderedLogsAndReleasesOnlyOnCompletion() throws Exception {
        Account owner = account(), borrower = account();
        long item = item(owner);
        var body = application(item, key());
        long id = data(send("/api/borrows", body, borrower)).path("id").asLong();
        action(id, "accept", owner).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.data.owner.contact").value("私人联系方式"))
                .andExpect(jsonPath("$.data.borrower.contact").value("私人联系方式"));
        read("/api/borrows?role=owner", owner).andExpect(jsonPath("$.data.list[0].owner.contact").value("私人联系方式"));
        action(id, "handover", owner).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("BORROWING"));
        assertThat(itemStatus(item)).isEqualTo("LENT");
        jdbc.update("UPDATE borrows SET due_at = DATEADD('DAY', -1, CURRENT_TIMESTAMP) WHERE id = ?", id);
        read("/api/borrows/" + id, borrower).andExpect(jsonPath("$.data.overdue").value(true));
        action(id, "return", borrower).andExpect(status().isOk());
        send("/api/borrows/" + id + "/deny-return", Map.of("reason", "  尚有雨渍  "), owner)
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("BORROWING"));
        assertThat(itemStatus(item)).isEqualTo("LENT");
        action(id, "return", borrower).andExpect(status().isOk());
        action(id, "confirm-return", owner).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.overdue").value(false));
        assertThat(itemStatus(item)).isEqualTo("AVAILABLE");
        assertThat(jdbc.queryForObject("SELECT active_borrow_id FROM items WHERE id = ?", Long.class, item)).isNull();
        var logs = data(read("/api/borrows/" + id + "/logs", owner));
        List<String> states = new ArrayList<>(); logs.forEach(log -> states.add(log.path("toStatus").asText()));
        assertThat(states).containsExactly("PENDING", "ACCEPTED", "BORROWING", "RETURNING", "BORROWING", "RETURNING", "COMPLETED");
        assertThat(logs.get(4).path("reason").asText()).isEqualTo("尚有雨渍");
        send("/api/borrows", body, borrower).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("COMPLETED"));
        assertThat(count(id, "borrow_logs", "borrow_id")).isEqualTo(7);
        send("/api/items/" + item + "/offline", Map.of(), owner).andExpect(status().isOk());
        read("/api/borrows/" + id, borrower).andExpect(status().isOk()).andExpect(jsonPath("$.data.item.id").value(item));
    }

    @Test
    void rejectionReleasesItemAndOldOrderCannotReleaseItsReplacement() throws Exception {
        Account owner = account(), borrower = account();
        long item = item(owner), id = borrow(item, borrower);
        send("/api/borrows/" + id + "/reject", Map.of("reason", "  本周自用  "), owner)
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.cancelType").value("REJECTED"))
                .andExpect(jsonPath("$.data.cancelReason").value("本周自用"))
                .andExpect(jsonPath("$.data.owner.contact").isEmpty());
        assertThat(itemStatus(item)).isEqualTo("AVAILABLE");
        long replacement = borrow(item, borrower);
        action(id, "cancel", owner).andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT active_borrow_id FROM items WHERE id = ?", Long.class, item)).isEqualTo(replacement);
        assertThat(count(id, "borrow_logs", "borrow_id")).isEqualTo(2);
    }

    @Test
    void eitherPartyCanCancelBeforeHandoverAndContactsBecomeHidden() throws Exception {
        Account owner = account(), borrower = account();
        for (String source : List.of("PENDING", "ACCEPTED")) {
            for (boolean byOwner : List.of(false, true)) {
                long item = item(owner), id = borrow(item, borrower);
                advance(id, source, owner, borrower);
                send("/api/borrows/" + id + "/cancel", Map.of("reason", "  "), byOwner ? owner : borrower)
                        .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("CANCELLED"))
                        .andExpect(jsonPath("$.data.cancelType").value(byOwner ? "OWNER_CANCEL" : "BORROWER_CANCEL"))
                        .andExpect(jsonPath("$.data.cancelReason").isEmpty()).andExpect(jsonPath("$.data.owner.contact").isEmpty())
                        .andExpect(jsonPath("$.data.borrower.contact").isEmpty()).andExpect(jsonPath("$.data.overdue").value(false));
                assertThat(itemStatus(item)).isEqualTo("AVAILABLE");
                read("/api/borrows?role=borrower&status=CANCELLED", borrower).andExpect(jsonPath("$.data.list[0].owner.contact").isEmpty());
            }
        }
    }

    @Test
    void everyActionRejectsAllOtherSourceStatesWithoutAddingLogs() throws Exception {
        Account owner = account(), borrower = account();
        Map<String, List<String>> allowed = Map.of("PENDING", List.of("accept", "reject", "cancel"),
                "ACCEPTED", List.of("handover", "cancel"), "BORROWING", List.of("return"),
                "RETURNING", List.of("confirm-return", "deny-return"), "COMPLETED", List.of(), "CANCELLED", List.of());
        for (String source : List.of("PENDING", "ACCEPTED", "BORROWING", "RETURNING", "COMPLETED", "CANCELLED")) {
            long item = item(owner), id = borrow(item, borrower);
            advance(id, source, owner, borrower);
            int before = count(id, "borrow_logs", "borrow_id");
            String beforeItem = itemStatus(item);
            for (String operation : ACTIONS) {
                if (!allowed.get(source).contains(operation)) {
                    action(id, operation, "return".equals(operation) ? borrower : owner)
                            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(40901));
                }
            }
            assertThat(count(id, "borrow_logs", "borrow_id")).isEqualTo(before);
            assertThat(itemStatus(item)).isEqualTo(beforeItem);
            read("/api/borrows/" + id, owner).andExpect(jsonPath("$.data.status").value(source));
        }
    }

    @Test
    void operationPermissionsAreCheckedForEveryEndpoint() throws Exception {
        Account owner = account(), borrower = account(), stranger = account();
        for (String operation : ACTIONS) {
            long id = borrow(item(owner), borrower);
            action(id, operation, stranger).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(40301));
            action(id, operation, null).andExpect(status().isUnauthorized());
            if (!"cancel".equals(operation)) {
                action(id, operation, "return".equals(operation) ? owner : borrower)
                        .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(40301));
            }
            assertThat(count(id, "borrow_logs", "borrow_id")).isEqualTo(1);
        }
    }

    @Test
    void expiredApplicationCannotBeAcceptedAndDueDateAloneDoesNotReleaseItem() throws Exception {
        Account owner = account(), borrower = account();
        long item = item(owner), id = borrow(item, borrower);
        jdbc.update("UPDATE borrows SET expires_at = DATEADD('SECOND', -1, CURRENT_TIMESTAMP), due_at = DATEADD('DAY', -1, CURRENT_TIMESTAMP) WHERE id = ?", id);
        action(id, "accept", owner).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(40901));
        read("/api/borrows/" + id, owner).andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.overdue").value(true)).andExpect(jsonPath("$.data.owner.contact").isEmpty());
        assertThat(itemStatus(item)).isEqualTo("RESERVED");
        assertThat(count(id, "borrow_logs", "borrow_id")).isEqualTo(1);
        action(id, "cancel", borrower).andExpect(status().isOk());
    }

    @Test
    void mismatchedActivePointerOrItemStateCannotBeMutated() throws Exception {
        Account owner = account(), borrower = account();
        long item = item(owner), id = borrow(item, borrower);
        long otherItem = item(owner), other = borrow(otherItem, borrower);
        jdbc.update("UPDATE items SET active_borrow_id = ? WHERE id = ?", other, item);
        action(id, "cancel", borrower).andExpect(status().isConflict());
        assertThat(itemStatus(otherItem)).isEqualTo("RESERVED");
        jdbc.update("UPDATE items SET active_borrow_id = ?, status = 'AVAILABLE' WHERE id = ?", id, item);
        action(id, "accept", owner).andExpect(status().isConflict());
        assertThat(count(id, "borrow_logs", "borrow_id")).isEqualTo(1);
        read("/api/borrows/" + id, owner).andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void invalidReasonAndLogFailureLeaveOrderAndItemUnchanged() throws Exception {
        Account owner = account(), borrower = account();
        long item = item(owner), id = borrow(item, borrower);
        send("/api/borrows/" + id + "/reject", Map.of("reason", "长".repeat(201)), owner)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(40001));
        action(id, "unsupported", owner).andExpect(status().isNotFound());
        jdbc.execute("ALTER TABLE borrow_logs ADD CONSTRAINT ck_week3_accept_log CHECK(actor_id <> " + owner.id() + " OR to_status <> 'ACCEPTED')");
        try {
            action(id, "accept", owner).andExpect(status().isInternalServerError());
            read("/api/borrows/" + id, owner).andExpect(jsonPath("$.data.status").value("PENDING"));
            assertThat(itemStatus(item)).isEqualTo("RESERVED");
            assertThat(count(id, "borrow_logs", "borrow_id")).isEqualTo(1);
        } finally { jdbc.execute("ALTER TABLE borrow_logs DROP CONSTRAINT ck_week3_accept_log"); }
    }

    @Test
    void unsupportedRequestMediaUsesTheSharedInputError() throws Exception {
        Account owner = account(), borrower = account();
        long id = borrow(item(owner), borrower);
        mvc.perform(post("/api/borrows/" + id + "/accept").header("Authorization", "Bearer " + owner.token())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED).content("reason=test"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(40001));
        assertThat(count(id, "borrow_logs", "borrow_id")).isEqualTo(1);
    }
}
