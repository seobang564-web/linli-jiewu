package com.linlijiewu.app.data.remote;

public final class AuthData {
    public String token;
    public long expiresIn;
    public User user;

    public static final class User {
        public long id;
        public String username;
        public String nickname;
        public String contact;
    }

    public static final class Register {
        public final String username;
        public final String password;
        public final String nickname;
        public final String contact;
        public Register(String username, String password, String nickname) {
            this(username, password, nickname, null);
        }
        public Register(String username, String password, String nickname, String contact) {
            this.username = username; this.password = password; this.nickname = nickname; this.contact = contact;
        }
    }

    public static final class Login {
        public final String username;
        public final String password;
        public Login(String username, String password) { this.username = username; this.password = password; }
    }
}
