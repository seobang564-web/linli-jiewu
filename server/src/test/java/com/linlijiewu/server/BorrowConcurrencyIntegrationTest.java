package com.linlijiewu.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class BorrowConcurrencyIntegrationTest extends BorrowTestSupport {
    @Test
    void competingBorrowersCanReserveAnItemOnlyOnce() throws Exception {
        Account owner = account(), a = account(), b = account();
        long item = item(owner);
        List<Integer> responses = race(List.of(
                () -> send("/api/borrows", application(item, key()), a).andReturn().getResponse().getStatus(),
                () -> send("/api/borrows", application(item, key()), b).andReturn().getResponse().getStatus()));
        assertThat(responses).containsExactlyInAnyOrder(200, 409);
        assertThat(count(item, "borrows", "item_id")).isEqualTo(1);
        long id = jdbc.queryForObject("SELECT active_borrow_id FROM items WHERE id = ?", Long.class, item);
        assertThat(count(id, "borrow_logs", "borrow_id")).isEqualTo(1);
    }

    @Test
    void concurrentSameKeyAcrossDifferentItemsRollsBackEveryLosingReservation() throws Exception {
        Account owner = account(), borrower = account();
        String key = key();
        List<Long> items = new ArrayList<>();
        List<Callable<Long>> calls = new ArrayList<>();
        for (int index = 0; index < 4; index++) {
            long item = item(owner); items.add(item);
            calls.add(() -> data(send("/api/borrows", application(item, key), borrower)).path("id").asLong());
            calls.add(() -> data(send("/api/borrows", application(item, key), borrower)).path("id").asLong());
        }
        List<Long> ids = race(calls);
        assertThat(ids.stream().distinct().count()).isEqualTo(1);
        assertThat(count(borrower.id(), "borrows", "borrower_id")).isEqualTo(1);
        assertThat(items.stream().filter(item -> "RESERVED".equals(itemStatus(item))).count()).isEqualTo(1);
        assertThat(count(ids.get(0), "borrow_logs", "borrow_id")).isEqualTo(1);
        for (long item : items) {
            if ("AVAILABLE".equals(itemStatus(item))) {
                assertThat(jdbc.queryForObject("SELECT active_borrow_id FROM items WHERE id = ?", Long.class, item)).isNull();
            }
        }
    }

    @Test
    void logFailureRollsBackBothOrderAndItemReservation() throws Exception {
        Account owner = account(), borrower = account();
        long item = item(owner);
        jdbc.execute("ALTER TABLE borrow_logs ADD CONSTRAINT ck_week3_create_log CHECK(actor_id <> " + borrower.id() + ")");
        try {
            send("/api/borrows", application(item, key()), borrower).andExpect(status().isInternalServerError());
            assertThat(count(item, "borrows", "item_id")).isZero();
            assertThat(itemStatus(item)).isEqualTo("AVAILABLE");
            assertThat(jdbc.queryForObject("SELECT active_borrow_id FROM items WHERE id = ?", Long.class, item)).isNull();
        } finally { jdbc.execute("ALTER TABLE borrow_logs DROP CONSTRAINT ck_week3_create_log"); }
    }

    @Test
    void simultaneousAcceptsProduceOnlyOneTransition() throws Exception {
        Account owner = account(), borrower = account();
        long id = borrow(item(owner), borrower);
        List<Integer> results = race(List.of(
                () -> action(id, "accept", owner).andReturn().getResponse().getStatus(),
                () -> action(id, "accept", owner).andReturn().getResponse().getStatus()));
        assertThat(results).containsExactlyInAnyOrder(200, 409);
        assertThat(count(id, "borrow_logs", "borrow_id")).isEqualTo(2);
    }

    @Test
    void acceptRacingCancellationNeverLeavesItemReservedAfterCancellation() throws Exception {
        Account owner = account(), borrower = account();
        long item = item(owner), id = borrow(item, borrower);
        List<Integer> results = race(List.of(
                () -> action(id, "accept", owner).andReturn().getResponse().getStatus(),
                () -> action(id, "cancel", borrower).andReturn().getResponse().getStatus()));
        assertThat(results.get(0)).isIn(200, 409);
        assertThat(results.get(1)).isEqualTo(200);
        assertThat(itemStatus(item)).isEqualTo("AVAILABLE");
        assertThat(jdbc.queryForObject("SELECT active_borrow_id FROM items WHERE id = ?", Long.class, item)).isNull();
        assertThat(jdbc.queryForObject("SELECT status FROM borrows WHERE id = ?", String.class, id)).isEqualTo("CANCELLED");
        assertThat(count(id, "borrow_logs", "borrow_id")).isEqualTo(results.get(0) == 200 ? 3 : 2);
    }

    @Test
    void borrowingAndTakingItemOfflineCannotBothSucceed() throws Exception {
        Account owner = account(), borrower = account();
        long item = item(owner);
        List<Integer> results = race(List.of(
                () -> send("/api/borrows", application(item, key()), borrower).andReturn().getResponse().getStatus(),
                () -> send("/api/items/" + item + "/offline", java.util.Map.of(), owner).andReturn().getResponse().getStatus()));
        if (results.get(0) == 200) {
            assertThat(results.get(1)).isEqualTo(409);
            assertThat(itemStatus(item)).isEqualTo("RESERVED");
            assertThat(count(item, "borrows", "item_id")).isEqualTo(1);
        } else {
            assertThat(results).containsExactly(404, 200);
            assertThat(itemStatus(item)).isEqualTo("OFFLINE");
            assertThat(count(item, "borrows", "item_id")).isZero();
        }
    }

    @Test
    void returningAndReborrowingNeverOverwritesTheNextActiveOrder() throws Exception {
        Account owner = account(), borrower = account(), nextBorrower = account();
        long item = item(owner), id = borrow(item, borrower);
        advance(id, "RETURNING", owner, borrower);
        List<Integer> results = race(List.of(
                () -> action(id, "confirm-return", owner).andReturn().getResponse().getStatus(),
                () -> send("/api/borrows", application(item, key()), nextBorrower).andReturn().getResponse().getStatus()));
        assertThat(results.get(0)).isEqualTo(200);
        assertThat(results.get(1)).isIn(200, 409);
        assertThat(jdbc.queryForObject("SELECT status FROM borrows WHERE id = ?", String.class, id)).isEqualTo("COMPLETED");
        if (results.get(1) == 200) {
            assertThat(itemStatus(item)).isEqualTo("RESERVED");
            assertThat(jdbc.queryForObject("SELECT active_borrow_id FROM items WHERE id = ?", Long.class, item)).isNotEqualTo(id);
        } else {
            assertThat(itemStatus(item)).isEqualTo("AVAILABLE");
            assertThat(jdbc.queryForObject("SELECT active_borrow_id FROM items WHERE id = ?", Long.class, item)).isNull();
        }
        assertThat(count(id, "borrow_logs", "borrow_id")).isEqualTo(5);
    }

    protected static <T> List<T> race(List<Callable<T>> calls) throws Exception {
        var executor = Executors.newFixedThreadPool(calls.size());
        var ready = new CountDownLatch(calls.size());
        var start = new CountDownLatch(1);
        try {
            List<Future<T>> futures = new ArrayList<>();
            for (Callable<T> call : calls) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) { throw new IllegalStateException("并发测试启动超时"); }
                    return call.call();
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) { results.add(future.get(20, TimeUnit.SECONDS)); }
            return results;
        } finally {
            start.countDown();
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
