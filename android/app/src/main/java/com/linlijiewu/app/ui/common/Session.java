package com.linlijiewu.app.ui.common;

import android.content.Context;
import android.content.SharedPreferences;
import com.linlijiewu.app.data.remote.AuthData;

/**
 * 登录会话：仅保存服务端签发的 Token 与公开用户信息，不保存密码。
 */
public final class Session {
    private static final String PREFS = "session";

    private Session() {
    }

    public static boolean hasEntered(Context c) {
        return prefs(c).getBoolean("entered", false);
    }

    public static boolean isLoggedIn(Context c) {
        return token(c) != null;
    }

    public static String nickname(Context c) {
        return prefs(c).getString("nickname", "");
    }

    public static String username(Context c) {
        return prefs(c).getString("username", "");
    }

    public static long userId(Context c) { return prefs(c).getLong("user_id", 0); }

    public static synchronized void login(Context c, AuthData data) {
        prefs(c).edit().clear().putBoolean("entered", true)
                .putString("token", data.token).putLong("user_id", data.user.id)
                .putLong("expires_at", System.currentTimeMillis() + Math.min(data.expiresIn, 604800L) * 1000L)
                .putString("username", data.user.username).putString("nickname", data.user.nickname).apply();
    }

    public static synchronized String token(Context c) {
        SharedPreferences p = prefs(c);
        String token = p.getString("token", null);
        if (token == null) { return null; }
        if (p.getLong("expires_at", 0) <= System.currentTimeMillis()) {
            p.edit().clear().putBoolean("entered", true).apply();
            return null;
        }
        return token;
    }

    public static synchronized void reject(Context c, String rejectedToken) {
        if (rejectedToken.equals(prefs(c).getString("token", null))) {
            prefs(c).edit().clear().putBoolean("entered", true).apply();
        }
    }

    public static synchronized void guest(Context c) {
        prefs(c).edit().clear().putBoolean("entered", true).apply();
    }

    public static synchronized void logout(Context c) {
        prefs(c).edit().clear().apply();
    }

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
