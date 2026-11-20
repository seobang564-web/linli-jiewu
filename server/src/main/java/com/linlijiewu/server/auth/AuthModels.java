package com.linlijiewu.server.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthModels {
    private AuthModels() { }
    public record Register(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_]{4,20}") String username,
            @NotBlank @Size(min = 6, max = 32) String password,
            @NotBlank @Size(max = 32) String nickname,
            @Size(max = 64) String contact) { }
    public record Login(@NotBlank @Size(max = 20) String username,
                        @NotBlank @Size(max = 32) String password) { }
    public record Profile(@NotBlank @Size(max = 32) String nickname, @Size(max = 64) String contact) { }
    public record PublicUser(long id, String username, String nickname) { }
    public record Me(long id, String username, String nickname, String contact) { }
    public record Token(String token, long expiresIn, PublicUser user) { }
}
