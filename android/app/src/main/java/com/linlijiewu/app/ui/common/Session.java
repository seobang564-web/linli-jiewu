package com.linlijiewu.app.ui.common;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 原型阶段的登录状态。注册登录接口上线后改为保存 Token（交给 TokenProvider）。
 */
public final class Session {
    private static final String PREFS = "session";

    private Session() {
    }

    public static boolean hasEntered(Context c) {
        return prefs(c).getBoolean("entered", false);
    }

    public static boolean isLoggedIn(Context c) {
        return prefs(c).getBoolean("logged_in", false);
    }

    public static String nickname(Context c) {
        return prefs(c).getString("nickname", "");
    }

    public static String username(Context c) {
        return prefs(c).getString("username", "");
    }

    public static void login(Context c, String username, String nickname) {
        prefs(c).edit().putBoolean("entered", true).putBoolean("logged_in", true)
                .putString("username", username).putString("nickname", nickname).apply();
    }

    public static void guest(Context c) {
        prefs(c).edit().putBoolean("entered", true).putBoolean("logged_in", false).apply();
    }

    public static void logout(Context c) {
        prefs(c).edit().clear().apply();
    }

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
