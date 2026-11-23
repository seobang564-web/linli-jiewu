package com.linlijiewu.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class BorrowCreationIntegrationTest extends BorrowTestSupport {
    @Test
    void applicationReservesItemAndReturnsPrivateSnapshotAndExistingConversation() throws Exception {
        Account owner = account(), borrower = account();
        long item = item(owner);
        jdbc.update("INSERT INTO conversations(item_id, owner_id, borrower_id) VALUES(?, ?, ?)", item, owner.id(), borrower.id());
        var body = application(item, key()); body.put("note", null);
        var order = data(send("/api/borrows", body, borrower));
        long id = order.path("id").asLong();
        assertThat(order.path("status").asText()).isEqualTo("PENDING");
        assertThat(order.path("note").asText()).isEmpty();
        assertThat(order.path("conversationId").asLong()).isPositive();
        assertThat(order.path("owner").path("contact").isNull()).isTrue();
        assertThat(order.path("borrower").path("contact").isNull()).isTrue();
        assertThat(order.path("item").path("title").asText()).isEqualTo(itemBody().get("title"));
        assertThat(OffsetDateTime.parse(order.path("dueAt").asText()).getOffset().getTotalSeconds()).isEqualTo(28800);
        assertThat(Duration.between(OffsetDateTime.parse(order.path("createdAt").asText()),
                OffsetDateTime.parse(order.path("expiresAt").asText())).toHours()).isEqualTo(12);
        assertThat(itemStatus(item)).isEqualTo("RESERVED");
        assertThat(jdbc.queryForObject("SELECT active_borrow_id FROM items WHERE id = ?", Long.class, item)).isEqualTo(id);
        var edit = itemBody(); edit.put("title", "【测试】物品已改名");
        mvc.perform(put("/api/items/" + item).header("Authorization", "Bearer " + owner.token())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(edit))).andExpect(status().isOk());
        read("/api/borrows/" + id, owner).andExpect(jsonPath("$.data.item.title").value(itemBody().get("title")));
        read("/api/borrows/" + id + "/logs", borrower).andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].fromStatus").isEmpty()).andExpect(jsonPath("$.data[0].toStatus").value("PENDING"))
                .andExpect(jsonPath("$.data[0].actor.id").value(borrower.id()));
    }

    @Test
    void sameRequestReturnsOriginalOrderEvenWithChangedItemAndPastDueDate() throws Exception {
        Account owner = account(), borrower = account(), another = account();
        long first = item(owner), second = item(owner);
        String key = key().toUpperCase(java.util.Locale.ROOT);
        long id = data(send("/api/borrows", application(first, key), borrower)).path("id").asLong();
        var retry = application(second, key.toLowerCase(java.util.Locale.ROOT)); retry.put("dueAt", "2020-01-01T00:00:00Z");
        send("/api/borrows", retry, borrower).andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(id));
        assertThat(count(first, "borrows", "item_id")).isEqualTo(1);
        assertThat(count(id, "borrow_logs", "borrow_id")).isEqualTo(1);
        assertThat(itemStatus(second)).isEqualTo("AVAILABLE");
        send("/api/borrows", application(second, key), another).andExpect(status().isOk());
    }

    @Test
    void accessAndRolePaginationAreRestrictedToOrderParticipants() throws Exception {
        Account owner = account(), borrower = account(), stranger = account();
        long a = borrow(item(owner), borrower), b = borrow(item(owner), borrower);
        borrow(item(stranger), owner);
        read("/api/borrows/" + a, stranger).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(40301));
        read("/api/borrows/" + a + "/logs", stranger).andExpect(status().isForbidden());
        read("/api/borrows/" + a, null).andExpect(status().isUnauthorized());
        read("/api/borrows/9223372036854775807", owner).andExpect(status().isNotFound());
        read("/api/borrows?role=borrower&size=1", borrower).andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.list[0].id").value(b));
        read("/api/borrows?role=owner&size=1&page=2&status=PENDING,PENDING", owner)
                .andExpect(jsonPath("$.data.total").value(2)).andExpect(jsonPath("$.data.list[0].id").value(a));
        read("/api/borrows?role=owner&status=COMPLETED", owner).andExpect(jsonPath("$.data.total").value(0));
        read("/api/borrows?role=borrower", stranger).andExpect(jsonPath("$.data.total").value(0));
        for (String query : List.of("", "?role=all", "?role=owner&status=", "?role=owner&status=OFFLINE",
                "?role=owner&page=0", "?role=owner&size=51", "?role=owner&size=0", "?role=owner&page=abc")) {
            read("/api/borrows" + query, borrower).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(40001));
        }
        read("/api/borrows?role=owner&page=2147483647&size=50", owner).andExpect(jsonPath("$.data.list").isEmpty());
    }

    @Test
    void invalidApplicationsAndUnavailableItemsDoNotCreateOrders() throws Exception {
        Account owner = account(), borrower = account();
        long item = item(owner);
        send("/api/borrows", application(item, key()), owner).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(40903));
        send("/api/borrows", application(item, key()), null).andExpect(status().isUnauthorized());
        for (var invalid : List.of(Map.entry("dueAt", "2020-01-01T00:00:00Z"), Map.entry("dueAt", "2026-12-01T00:00:00"),
                Map.entry("dueAt", "+999999999-12-31T23:59:59-18:00"), Map.entry("dueAt", "9999-12-31T23:59:59-18:00"),
                Map.entry("requestId", "bad key"), Map.entry("requestId", "x".repeat(65)), Map.entry("note", "长".repeat(201)),
                Map.entry("itemId", -1))) {
            var body = application(item, key()); body.put(invalid.getKey(), invalid.getValue());
            send("/api/borrows", body, borrower).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(40001));
        }
        send("/api/borrows", Map.of(), borrower).andExpect(status().isBadRequest());
        assertThat(count(item, "borrows", "item_id")).isZero();
        long id = borrow(item, borrower);
        send("/api/borrows", application(item, key()), borrower).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(40901));
        send("/api/items/" + item + "/offline", Map.of(), owner).andExpect(status().isConflict());
        assertThat(count(id, "borrow_logs", "borrow_id")).isEqualTo(1);
        long offline = item(owner);
        send("/api/items/" + offline + "/offline", Map.of(), owner).andExpect(status().isOk());
        send("/api/borrows", application(offline, key()), borrower).andExpect(status().isNotFound());
        send("/api/borrows", application(Long.MAX_VALUE, key()), borrower).andExpect(status().isNotFound());
    }
}
