package com.linlijiewu.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.linlijiewu.server.image.ImageService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Week2IntegrationTest {
    private static final Path UPLOADS = temporaryUploads();
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ImageService imageService;

    private static Path temporaryUploads() {
        try { return Files.createTempDirectory("linli-week2-test-"); }
        catch (java.io.IOException exception) { throw new IllegalStateException(exception); }
    }
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("linli.upload-dir", () -> UPLOADS.toString());
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:week2;MODE=MySQL;DB_CLOSE_DELAY=-1");
    }

    @Test
    void realPasswordsDuplicateNamesAndPrivateProfile() throws Exception {
        JsonNode user = register();
        String token = user.path("token").asText();
        String username = user.path("user").path("username").asText();
        String stored = jdbc.queryForObject("SELECT password_hash FROM users WHERE username = ?", String.class, username);
        assertThat(stored).isNotEqualTo("password123");
        assertThat(new BCryptPasswordEncoder().matches("password123", stored)).isTrue();
        postJson("/api/auth/login", Map.of("username", username, "password", "password123"), null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.token").isString());
        postJson("/api/auth/login", Map.of("username", username, "password", "incorrect"), null)
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(40101));
        postJson("/api/auth/register", Map.of("username", username, "password", "password123", "nickname", "重复"), null)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(40902));
        mvc.perform(put("/api/users/me").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(Map.of("nickname", "新昵称", "contact", "私人号码"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.contact").value("私人号码"))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.nickname").value("新昵称"));
    }

    @Test
    void missingForgedAndExpiredTokensAreRejected() throws Exception {
        mvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer forged"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(40101));
        var key = Keys.hmacShaKeyFor(Base64.getDecoder().decode("dGVzdC1vbmx5LXNlY3JldC1mb3ItbG9jYWwtdGVzdHMtMzItYnl0ZXM="));
        String expired = Jwts.builder().issuer("linli-jiewu").subject("1")
                .expiration(Date.from(Instant.now().minusSeconds(60))).signWith(key).compact();
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + expired)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/hello")).andExpect(status().isOk());
    }

    @Test
    void twoAccountsShareItemAndImageButCannotModifyEachOther() throws Exception {
        JsonNode owner = register();
        String a = owner.path("token").asText();
        String b = register().path("token").asText();
        JsonNode image = data(mvc.perform(multipart("/api/images")
                .file(new MockMultipartFile("file", "original.png", "image/png", png()))
                .header("Authorization", "Bearer " + a)));
        String imageId = image.path("imageId").asText();
        String url = image.path("url").asText();
        assertThat(jdbc.queryForObject("SELECT owner_id FROM images WHERE image_id = ?", Long.class, imageId))
                .isEqualTo(owner.path("user").path("id").asLong());
        byte[] saved = mvc.perform(get(url)).andExpect(status().isOk()).andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(ImageIO.read(new java.io.ByteArrayInputStream(saved)).getWidth()).isEqualTo(2);
        long id = data(postJson("/api/items", itemBody(imageId), a)).path("id").asLong();
        mvc.perform(get("/api/items/" + id).header("Authorization", "Bearer " + b))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.isMine").value(false))
                .andExpect(jsonPath("$.data.title").value("【测试】帐篷"))
                .andExpect(jsonPath("$.data.coverUrl").value(url)).andExpect(jsonPath("$.data.owner.contact").doesNotExist());
        mvc.perform(get("/api/items/" + id)).andExpect(status().isOk());
        mvc.perform(put("/api/items/" + id).header("Authorization", "Bearer " + b)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(itemBody(imageId))))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(40301));
        postJson("/api/items/" + id + "/offline", Map.of(), b).andExpect(status().isForbidden());
        postJson("/api/items", itemBody(imageId), b).andExpect(status().isForbidden());
        mvc.perform(get("/api/users/me/items").header("Authorization", "Bearer " + a))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1));
        mvc.perform(get("/api/users/me/items").header("Authorization", "Bearer " + b))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(0));
        postJson("/api/items/" + id + "/offline", Map.of(), a).andExpect(status().isOk());
        mvc.perform(get("/api/items/" + id)).andExpect(status().isNotFound());
        mvc.perform(get("/api/items/" + id).header("Authorization", "Bearer " + a))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("OFFLINE"));
    }

    @Test
    void activeOrdersProtectPickupAndOfflineWhileDescriptionCanChange() throws Exception {
        String token = register().path("token").asText();
        long id = data(postJson("/api/items", itemBody(null), token)).path("id").asLong();
        jdbc.update("UPDATE items SET active_borrow_id = 123, status = 'RESERVED' WHERE id = ?", id);
        var changed = itemBody(null); changed.put("pickupLat", 31.0);
        mvc.perform(put("/api/items/" + id).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(changed))).andExpect(status().isConflict());
        postJson("/api/items/" + id + "/offline", Map.of(), token).andExpect(status().isConflict());
        var edit = itemBody(null); edit.put("description", "更新描述");
        mvc.perform(put("/api/items/" + id).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(edit)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.description").value("更新描述"));
    }

    @Test
    void invalidRequestsAndUploadsUseSharedErrors() throws Exception {
        String token = register().path("token").asText();
        var invalid = itemBody(null); invalid.put("pickupLat", 91);
        postJson("/api/items", invalid, token).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(40001));
        postJson("/api/items", itemBody("../../secret"), token).andExpect(status().isBadRequest());
        mvc.perform(get("/api/users/me/items").param("size", "51").header("Authorization", "Bearer " + token)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/users/me/items").param("status", "BOGUS").header("Authorization", "Bearer " + token)).andExpect(status().isBadRequest());
        mvc.perform(get("/files/secret.env")).andExpect(status().isNotFound());
        mvc.perform(multipart("/api/images").file(new MockMultipartFile("file", "fake.jpg", "image/jpeg", "not an image".getBytes()))
                .header("Authorization", "Bearer " + token)).andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/images").file(new MockMultipartFile("file", "huge.jpg", "image/jpeg", new byte[5 * 1024 * 1024 + 1]))
                .header("Authorization", "Bearer " + token)).andExpect(status().isPayloadTooLarge()).andExpect(jsonPath("$.code").value(41301));
        mvc.perform(multipart("/api/images").file(new MockMultipartFile("file", "pic.png", "image/png", png()))).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/items").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    void missingImageTableFailsHealthWithoutRemovingExistingAccounts() throws Exception {
        long count = jdbc.queryForObject("SELECT COUNT(*) FROM users", Long.class);
        jdbc.execute("ALTER TABLE images RENAME TO images_backup");
        try { mvc.perform(get("/api/health")).andExpect(status().isServiceUnavailable()); }
        finally { jdbc.execute("ALTER TABLE images_backup RENAME TO images"); }
        mvc.perform(get("/api/health")).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users", Long.class)).isEqualTo(count);
    }

    @Test
    void webpAndJpegAreDecodedAndReencodedWithoutOriginalMetadata() throws Exception {
        String token = register().path("token").asText();
        byte[] webp;
        try (var input = getClass().getResourceAsStream("/images/sample.webp")) { webp = input.readAllBytes(); }
        var jpeg = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB), "jpeg", jpeg);
        for (byte[] bytes : new byte[][]{webp, jpeg.toByteArray()}) {
            JsonNode image = data(mvc.perform(multipart("/api/images")
                    .file(new MockMultipartFile("file", "untrusted.filename", "application/octet-stream", bytes))
                    .header("Authorization", "Bearer " + token)));
            byte[] output = mvc.perform(get(image.path("url").asText())).andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.IMAGE_PNG)).andReturn().getResponse().getContentAsByteArray();
            assertThat(output).startsWith(new byte[]{(byte) 137, 80, 78, 71});
            assertThat(ImageIO.read(new java.io.ByteArrayInputStream(output)).getHeight()).isEqualTo(2);
        }
    }

    @Test
    void failedMetadataInsertRollsBackTheNewFile() throws Exception {
        long before;
        Files.createDirectories(UPLOADS);
        try (var files = Files.list(UPLOADS)) { before = files.count(); }
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> imageService.upload(-999,
                new MockMultipartFile("file", "pic.png", "image/png", png())))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        try (var files = Files.list(UPLOADS)) { assertThat(files.count()).isEqualTo(before); }
    }

    private JsonNode register() throws Exception {
        String username = "u" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return data(postJson("/api/auth/register", Map.of("username", username, "password", "password123", "nickname", "测试用户"), null));
    }
    private Map<String, Object> itemBody(String imageId) {
        var body = new java.util.HashMap<String, Object>();
        body.put("title", "【测试】帐篷"); body.put("category", "CAMPING"); body.put("description", null);
        body.put("imageId", imageId); body.put("pickupLat", 30.5123456); body.put("pickupLng", 114.4123456); body.put("pickupLabel", "东门");
        return body;
    }
    private ResultActions postJson(String url, Object body, String token) throws Exception {
        var request = post(url).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body));
        if (token != null) { request.header("Authorization", "Bearer " + token); }
        return mvc.perform(request);
    }
    private JsonNode data(ResultActions result) throws Exception {
        return json.readTree(result.andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray()).path("data");
    }
    private byte[] png() throws Exception {
        var output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", output);
        return output.toByteArray();
    }
}
