package com.linlijiewu.app.data.remote;

public interface TokenProvider {
    String getToken();
    default void onUnauthorized(String rejectedToken) { }
}
