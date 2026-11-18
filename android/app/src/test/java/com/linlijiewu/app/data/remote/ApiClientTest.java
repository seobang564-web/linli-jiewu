package com.linlijiewu.app.data.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import retrofit2.Response;

public class ApiClientTest {
    private MockWebServer server;

    @Before
    public void startServer() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    @After
    public void stopServer() throws Exception {
        server.shutdown();
    }

    @Test
    public void helloPreservesApiPrefixAndDecodesTheSharedEnvelope() throws Exception {
        server.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
                .setBody("{\"code\":0,\"message\":\"ok\",\"data\":{\"service\":\"linli-jiewu\",\"message\":\"Hello\",\"time\":\"2026-11-18T12:00:00+08:00\"}}"));
        LinliApi api = ApiClient.create(server.url("/api/").toString(), () -> null);

        Response<ApiResponse<HelloData>> response = api.hello().execute();

        assertNotNull(response.body());
        assertEquals(0, response.body().getCode());
        assertEquals("linli-jiewu", response.body().getData().getService());
        RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("/api/hello", request.getPath());
        assertNull(request.getHeader("Authorization"));
    }

    @Test
    public void authenticatedCallsCarryTheBearerToken() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(401));
        LinliApi api = ApiClient.create(server.url("/api/").toString(), () -> "test-token");

        assertEquals(401, api.hello().execute().code());
        RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("Bearer test-token", request.getHeader("Authorization"));
    }
}
