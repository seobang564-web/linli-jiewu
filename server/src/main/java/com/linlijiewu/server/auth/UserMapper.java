package com.linlijiewu.server.auth;

import org.apache.ibatis.annotations.*;

@Mapper
public interface UserMapper {
    @Select("SELECT id, username, password_hash, nickname, contact FROM users WHERE username = #{username}")
    UserRow findByUsername(String username);
    @Select("SELECT id, username, password_hash, nickname, contact FROM users WHERE id = #{id}")
    UserRow findById(long id);
    @Insert("INSERT INTO users(username, password_hash, nickname, contact) VALUES(#{username}, #{passwordHash}, #{nickname}, #{contact})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserRow user);
    @Update("UPDATE users SET nickname = #{nickname}, contact = #{contact} WHERE id = #{id}")
    int update(UserRow user);
}
