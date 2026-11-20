package com.linlijiewu.app.data.remote;

import okhttp3.HttpUrl;

public final class FileUrls {
    private FileUrls() { }
    public static String resolve(String baseUrl, String path) {
        if (path == null || path.isEmpty()) { return null; }
        HttpUrl result = HttpUrl.get(baseUrl).resolve(path);
        return result == null ? null : result.toString();
    }
}
