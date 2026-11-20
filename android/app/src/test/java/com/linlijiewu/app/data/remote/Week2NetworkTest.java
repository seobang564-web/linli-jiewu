package com.linlijiewu.app.data.remote;

import static org.junit.Assert.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.mockwebserver.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class Week2NetworkTest {
    private MockWebServer server;
    @Before public void start() throws Exception { server = new MockWebServer(); server.start(); }
    @After public void stop() throws Exception { server.shutdown(); }

    @Test public void registrationUsesContractAndDecodesUserAndToken() throws Exception {
        server.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
                .setBody("{\"code\":0,\"data\":{\"token\":\"signed-token\",\"expiresIn\":604800,\"user\":{\"id\":7,\"username\":\"test_user\",\"nickname\":\"测试\"}}}"));
        var data = ApiClient.create(server.url("/api/").toString(), () -> null)
                .register(new AuthData.Register("test_user", "secret123", "测试")).execute().body().getData();
        assertEquals("signed-token", data.token); assertEquals(7, data.user.id);
        var request = server.takeRequest(2, TimeUnit.SECONDS);
        assertEquals("/api/auth/register", request.getPath()); assertNull(request.getHeader("Authorization"));
        var body = com.google.gson.JsonParser.parseString(request.getBody().readUtf8()).getAsJsonObject();
        assertEquals("secret123", body.get("password").getAsString());
        assertEquals("test_user", body.get("username").getAsString());
    }

    @Test public void uploadUsesFileMultipartFieldAndBearerToken() throws Exception {
        server.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
                .setBody("{\"code\":0,\"data\":{\"imageId\":\"abc\",\"url\":\"/files/abc.png\"}}"));
        var part = MultipartBody.Part.createFormData("file", "photo.jpg", RequestBody.create(new byte[]{1,2,3}, MediaType.parse("image/jpeg")));
        var response = ApiClient.create(server.url("/api/").toString(), () -> "token").upload(part).execute();
        assertEquals("abc", response.body().getData().imageId);
        var request = server.takeRequest(2, TimeUnit.SECONDS);
        assertEquals("/api/images", request.getPath()); assertEquals("Bearer token", request.getHeader("Authorization"));
        assertTrue(request.getHeader("Content-Type").startsWith("multipart/form-data; boundary="));
        assertTrue(request.getBody().readUtf8().contains("name=\"file\"; filename=\"photo.jpg\""));
    }

    @Test public void delayedUnauthorizedResponseIdentifiesOriginalToken() throws Exception {
        AtomicReference<String> current = new AtomicReference<>("old-token");
        AtomicReference<String> rejected = new AtomicReference<>();
        server.setDispatcher(new Dispatcher() {
            @Override public MockResponse dispatch(RecordedRequest request) {
                current.set("new-token"); return new MockResponse().setResponseCode(401);
            }
        });
        TokenProvider provider = new TokenProvider() {
            @Override public String getToken() { return current.get(); }
            @Override public void onUnauthorized(String token) { rejected.set(token); }
        };
        assertEquals(401, ApiClient.create(server.url("/api/").toString(), provider).me().execute().code());
        assertEquals("old-token", rejected.get()); assertEquals("new-token", current.get());
    }

    @Test public void absoluteFilePathResolvesAtServerRootWithoutDoubleSlashes() {
        assertEquals("http://localhost:8080/files/abc.png", FileUrls.resolve("http://localhost:8080/api/", "/files/abc.png"));
        assertNull(FileUrls.resolve("http://localhost:8080/api/", null));
    }
}
