package com.linlijiewu.server.health;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface HealthMapper {
    // WHERE 1 = 0 verifies table access without reading business rows.
    @Select("""
            SELECT COUNT(*)
            FROM users, images, items, borrows, borrow_logs, conversations, messages
            WHERE 1 = 0
            """)
    int verifySchema();
}
