package com.linlijiewu.app.data.remote;

import java.util.concurrent.TimeUnit;
import android.content.Context;
import com.linlijiewu.app.BuildConfig;
import com.linlijiewu.app.ui.common.Session;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiClient {
    private ApiClient() {
    }

    public static LinliApi authenticated(Context context) {
        Context app = context.getApplicationContext();
        return create(BuildConfig.SERVER_BASE_URL, new TokenProvider() {
            @Override public String getToken() { return Session.token(app); }
            @Override public void onUnauthorized(String rejectedToken) { Session.reject(app, rejectedToken); }
        });
    }

    public static LinliApi create(String baseUrl, TokenProvider tokenProvider) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .callTimeout(20, TimeUnit.SECONDS)
                .addInterceptor(new TokenInterceptor(tokenProvider))
                .build();

        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(LinliApi.class);
    }
}
