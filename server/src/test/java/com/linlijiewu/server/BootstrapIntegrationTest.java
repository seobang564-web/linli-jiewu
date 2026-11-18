package com.linlijiewu.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BootstrapIntegrationTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void helloUsesTheSharedEnvelopeAndShanghaiTime() throws Exception {
        String body = mvc.perform(get("/api/hello"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.service").value("linli-jiewu"))
                .andExpect(jsonPath("$.data.message").value("Hello, 邻里借物地图！"))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        String time = objectMapper.readTree(body).path("data").path("time").asText();
        assertThat(OffsetDateTime.parse(time).getOffset()).isEqualTo(ZoneOffset.ofHours(8));
    }

    @Test
    void readinessChecksTheDatabaseThroughMyBatis() throws Exception {
        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.database").value("UP"))
                .andExpect(jsonPath("$.data.schema").value("UP"));
    }

    @Test
    void missingTableFailsReadinessWithoutBreakingHelloOrLeakingSql() throws Exception {
        jdbc.execute("DROP TABLE messages");
        try {
            String body = mvc.perform(get("/api/health"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.code").value(50301))
                    .andExpect(jsonPath("$.message").value("数据库连接或基础表未就绪"))
                    .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
            assertThat(body).doesNotContain("SELECT", "jdbc:", "BadSqlGrammarException");
            mvc.perform(get("/api/hello")).andExpect(status().isOk());
        } finally {
            jdbc.execute("CREATE TABLE messages (id BIGINT PRIMARY KEY)");
        }
    }

    @Test
    void unknownRouteUsesTheSharedErrorEnvelope() throws Exception {
        mvc.perform(get("/api/not-implemented"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));
    }
}
