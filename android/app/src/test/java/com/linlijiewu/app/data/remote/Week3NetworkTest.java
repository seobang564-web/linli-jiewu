package com.linlijiewu.app.data.remote;

import static org.junit.Assert.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import com.google.gson.JsonParser;
import okhttp3.mockwebserver.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class Week3NetworkTest {
    private MockWebServer server;
    private LinliApi api;
    @Before public void start() throws Exception { server = new MockWebServer(); server.start(); api = ApiClient.create(server.url("/api/").toString(), () -> "test-token"); }
    @After public void stop() throws Exception { server.shutdown(); }
    private void respond(String data) { server.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody("{\"code\":0,\"data\":" + data + "}")); }

    @Test public void nearbyEncodesSearchAndDecodesActualPickupCoordinates() throws Exception {
        respond("{\"total\":1,\"page\":2,\"size\":20,\"list\":[{\"id\":91,\"pickupLat\":30.5123456,\"pickupLng\":114.4123456,\"distanceM\":499,\"status\":\"AVAILABLE\"}]}");
        ItemData.Page page = api.nearby(30.5, 114.4, 500, "帐篷 100%_", "CAMPING", 2, 20).execute().body().getData();
        var request = server.takeRequest(2, TimeUnit.SECONDS);
        assertEquals("帐篷 100%_", request.getRequestUrl().queryParameter("keyword"));
        assertEquals("500", request.getRequestUrl().queryParameter("radius")); assertEquals("2", request.getRequestUrl().queryParameter("page"));
        assertEquals(91, page.list.get(0).id); assertEquals(30.5123456, page.list.get(0).pickupLat, 0.00000001);
    }

    @Test public void retryUsesSameApplicationPayloadWithoutClientSuppliedIdentity() throws Exception {
        BorrowData.Create create = new BorrowData.Create(); create.itemId = 91; create.requestId = "same-request-id";
        create.dueAt = "2026-12-01T10:00:00Z"; create.note = "借用测试";
        String firstBody = null;
        for (int i = 0; i < 2; i++) {
            respond("{\"id\":72,\"status\":\"PENDING\",\"owner\":{\"id\":1,\"contact\":null},\"borrower\":{\"id\":2}}");
            assertEquals(72, api.borrow(create).execute().body().getData().id);
            var request = server.takeRequest(2, TimeUnit.SECONDS); String body = request.getBody().readUtf8();
            assertEquals("/api/borrows", request.getPath()); assertEquals("Bearer test-token", request.getHeader("Authorization"));
            var json = JsonParser.parseString(body).getAsJsonObject(); assertFalse(json.has("userId"));
            assertEquals("same-request-id", json.get("requestId").getAsString());
            if (firstBody != null) { assertEquals(firstBody, body); } firstBody = body;
        }
    }

    @Test public void allStateOperationsUseJsonAndTheRealOrderId() throws Exception {
        for (String operation : Arrays.asList("accept", "reject", "cancel", "handover", "return", "confirm-return", "deny-return")) {
            respond("{\"id\":72,\"status\":\"ACCEPTED\"}");
            assertEquals(72, api.borrowAction(72, operation, new BorrowData.Action("测试原因")).execute().body().getData().id);
            var request = server.takeRequest(2, TimeUnit.SECONDS);
            assertEquals("POST", request.getMethod()); assertEquals("/api/borrows/72/" + operation, request.getPath());
            assertTrue(request.getHeader("Content-Type").startsWith("application/json"));
        }
    }

    @Test public void paginatedOrdersAndSystemLogsDecodeWithoutSampleFallback() throws Exception {
        respond("{\"total\":1,\"page\":2,\"size\":20,\"list\":[{\"id\":72,\"item\":{\"id\":91,\"title\":\"真实物品\"}}]}");
        assertEquals("真实物品", api.borrows("owner", "PENDING", 2, 20).execute().body().getData().list.get(0).item.title);
        assertEquals("owner", server.takeRequest(2, TimeUnit.SECONDS).getRequestUrl().queryParameter("role"));
        respond("[{\"fromStatus\":\"PENDING\",\"toStatus\":\"CANCELLED\",\"actor\":null}]");
        assertNull(api.borrowLogs(72).execute().body().getData().get(0).actor);
    }

    @Test public void clientActionsFollowParticipantAndCurrentState() {
        BorrowData order = new BorrowData(); order.owner = new BorrowData.Person(); order.owner.id = 1;
        order.borrower = new BorrowData.Person(); order.borrower.id = 2;
        order.status = "PENDING";
        assertEquals(Arrays.asList("accept", "reject", "cancel"), BorrowActions.forUser(order, 1));
        assertEquals(Collections.singletonList("cancel"), BorrowActions.forUser(order, 2)); assertTrue(BorrowActions.forUser(order, 3).isEmpty());
        order.status = "ACCEPTED"; assertEquals(Arrays.asList("handover", "cancel"), BorrowActions.forUser(order, 1));
        order.status = "BORROWING"; assertEquals(Collections.singletonList("return"), BorrowActions.forUser(order, 2));
        order.status = "RETURNING"; assertEquals(Arrays.asList("confirm-return", "deny-return"), BorrowActions.forUser(order, 1));
        order.status = "COMPLETED"; assertTrue(BorrowActions.forUser(order, 1).isEmpty());
        order.status = "CANCELLED"; assertTrue(BorrowActions.forUser(order, 2).isEmpty());
    }
}
