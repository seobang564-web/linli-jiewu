package com.linlijiewu.app.data.remote;

import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public final class TokenInterceptor implements Interceptor {
    private final TokenProvider tokenProvider;

    public TokenInterceptor(TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request.Builder builder = chain.request().newBuilder();
        String token = tokenProvider.getToken();
        if (token != null && !token.trim().isEmpty()) {
            builder.header("Authorization", "Bearer " + token);
        }
        Response response = chain.proceed(builder.build());
        if (response.code() == 401 && token != null && !token.trim().isEmpty()) {
            tokenProvider.onUnauthorized(token);
        }
        return response;
    }
}
