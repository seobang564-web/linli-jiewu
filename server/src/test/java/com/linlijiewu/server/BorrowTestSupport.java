package com.linlijiewu.server;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:week3;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000")
abstract class BorrowTestSupport {
    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper json;
    @Autowired protected JdbcTemplate jdbc;

    protected record Account(long id, String token) { }

    protected Account account() throws Exception {
        JsonNode data = data(send("/api/auth/register", Map.of("username", "u" + key().replace("-", "").substring(0, 12),
                "password", "password123", "nickname", "【测试】借还用户", "contact", "私人联系方式"), null));
        return new Account(data.path("user").path("id").asLong(), data.path("token").asText());
    }

    protected long item(Account owner) throws Exception { return data(send("/api/items", itemBody(), owner)).path("id").asLong(); }

    protected Map<String, Object> itemBody() {
        var body = new HashMap<String, Object>();
        body.put("title", "【测试】借还帐篷\"备用\""); body.put("category", "CAMPING"); body.put("description", "第三周验收");
        body.put("pickupLat", 30.5123456); body.put("pickupLng", 114.4123456); body.put("pickupLabel", "【测试】东门交接点");
        return body;
    }

    protected Map<String, Object> application(long itemId, String requestId) {
        var body = new HashMap<String, Object>();
        body.put("itemId", itemId); body.put("requestId", requestId);
        body.put("dueAt", OffsetDateTime.now(ZoneOffset.UTC).plusDays(3).withNano(0).toString());
        body.put("note", "【测试】周末借用");
        return body;
    }

    protected long borrow(long itemId, Account borrower) throws Exception {
        return data(send("/api/borrows", application(itemId, key()), borrower)).path("id").asLong();
    }

    protected ResultActions send(String uri, Object body, Account actor) throws Exception {
        var request = post(uri).contentType(MediaType.APPLICATION_JSON);
        if (body != null) { request.content(json.writeValueAsBytes(body)); }
        if (actor != null) { request.header("Authorization", "Bearer " + actor.token()); }
        return mvc.perform(request);
    }

    protected ResultActions read(String uri, Account actor) throws Exception {
        var request = get(uri);
        if (actor != null) { request.header("Authorization", "Bearer " + actor.token()); }
        return mvc.perform(request);
    }

    protected ResultActions action(long id, String action, Account actor) throws Exception {
        return send("/api/borrows/" + id + "/" + action, null, actor);
    }

    protected void advance(long id, String state, Account owner, Account borrower) throws Exception {
        if ("PENDING".equals(state)) { return; }
        if ("CANCELLED".equals(state)) { action(id, "cancel", borrower).andExpect(status().isOk()); return; }
        action(id, "accept", owner).andExpect(status().isOk());
        if ("ACCEPTED".equals(state)) { return; }
        action(id, "handover", owner).andExpect(status().isOk());
        if ("BORROWING".equals(state)) { return; }
        action(id, "return", borrower).andExpect(status().isOk());
        if ("RETURNING".equals(state)) { return; }
        action(id, "confirm-return", owner).andExpect(status().isOk());
    }

    protected JsonNode data(ResultActions result) throws Exception {
        return json.readTree(result.andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray()).path("data");
    }

    protected int count(long id, String table, String column) {
        // Table and column names are fixed test literals, never request inputs.
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?", Integer.class, id);
    }

    protected String itemStatus(long id) { return jdbc.queryForObject("SELECT status FROM items WHERE id = ?", String.class, id); }
    protected static String key() { return UUID.randomUUID().toString(); }
}
