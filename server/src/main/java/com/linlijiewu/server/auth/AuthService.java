package com.linlijiewu.server.auth;

import java.nio.charset.StandardCharsets;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.linlijiewu.server.common.BusinessException;

@Service
public class AuthService {
    private final UserMapper users;
    private final JwtService jwt;
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder();
    private final String dummyHash = passwords.encode("unmatchable-placeholder-password");

    public AuthService(UserMapper users, JwtService jwt) { this.users = users; this.jwt = jwt; }

    @Transactional
    public AuthModels.Token register(AuthModels.Register request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw BusinessException.badRequest("密码 UTF-8 长度不能超过 72 字节");
        }
        UserRow user = new UserRow();
        user.username = request.username();
        user.passwordHash = passwords.encode(request.password());
        user.nickname = request.nickname().trim();
        if (user.nickname.isEmpty()) { throw BusinessException.badRequest("昵称不能为空"); }
        user.contact = trimContact(request.contact());
        try { users.insert(user); }
        catch (DuplicateKeyException exception) {
            throw new BusinessException(409, 40902, "用户名已存在");
        }
        return token(user);
    }

    public AuthModels.Token login(AuthModels.Login request) {
        UserRow user = users.findByUsername(request.username());
        boolean match = request.password().getBytes(StandardCharsets.UTF_8).length <= 72
                && passwords.matches(request.password(), user == null ? dummyHash : user.passwordHash);
        if (user == null || !match) { throw new BusinessException(401, 40101, "用户名或密码不正确"); }
        return token(user);
    }

    public AuthModels.Me me(long id) {
        UserRow user = users.findById(id);
        if (user == null) { throw BusinessException.unauthorized(); }
        return new AuthModels.Me(user.id, user.username, user.nickname, user.contact);
    }

    @Transactional
    public AuthModels.Me update(long id, AuthModels.Profile request) {
        UserRow user = users.findById(id);
        if (user == null) { throw BusinessException.unauthorized(); }
        user.nickname = request.nickname().trim();
        if (user.nickname.isEmpty()) { throw BusinessException.badRequest("昵称不能为空"); }
        user.contact = trimContact(request.contact());
        users.update(user);
        return me(id);
    }

    private String trimContact(String contact) { return contact == null ? null : contact.trim(); }
    private AuthModels.Token token(UserRow user) {
        return new AuthModels.Token(jwt.issue(user.id), jwt.expiresIn(),
                new AuthModels.PublicUser(user.id, user.username, user.nickname));
    }
}
