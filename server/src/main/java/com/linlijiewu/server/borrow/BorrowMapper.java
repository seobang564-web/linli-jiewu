package com.linlijiewu.server.borrow;

import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface BorrowMapper {
    String DETAIL = """
            SELECT b.*, o.nickname AS owner_nickname, o.contact AS owner_contact,
                   u.nickname AS borrower_nickname, u.contact AS borrower_contact
            FROM borrows b JOIN users o ON o.id = b.owner_id JOIN users u ON u.id = b.borrower_id
            """;

    @Select(DETAIL + " WHERE b.id = #{id}")
    BorrowRow find(long id);

    @Select("SELECT * FROM borrows WHERE id = #{id} FOR UPDATE")
    BorrowRow lock(long id);

    @Update("""
            UPDATE borrows SET status = #{status}, cancel_type = #{cancelType},
                               cancel_reason = #{cancelReason}, updated_at = #{updatedAt} WHERE id = #{id}
            """)
    int update(BorrowRow row);

    @Update("""
            UPDATE items SET status = #{status}, active_borrow_id = #{activeBorrowId}
            WHERE id = #{itemId} AND active_borrow_id = #{borrowId}
            """)
    int changeItem(@Param("itemId") long itemId, @Param("borrowId") long borrowId,
                   @Param("status") String status, @Param("activeBorrowId") Long activeBorrowId);

    @Select("SELECT * FROM borrows WHERE borrower_id = #{borrowerId} AND request_id = #{requestId}")
    BorrowRow findRequest(@Param("borrowerId") long borrowerId, @Param("requestId") String requestId);

    @Insert("""
            INSERT INTO borrows(item_id, owner_id, borrower_id, conversation_id, status, due_at, expires_at,
                                note, request_id, item_snapshot, created_at, updated_at)
            VALUES(#{itemId}, #{ownerId}, #{borrowerId}, #{conversationId}, #{status}, #{dueAt}, #{expiresAt},
                   #{note}, #{requestId}, #{itemSnapshot}, #{createdAt}, #{updatedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(BorrowRow row);

    @Update("""
            UPDATE items SET status = 'RESERVED', active_borrow_id = #{borrowId}
            WHERE id = #{itemId} AND status = 'AVAILABLE' AND active_borrow_id IS NULL
            """)
    int reserve(@Param("itemId") long itemId, @Param("borrowId") long borrowId);

    @Select("SELECT id FROM conversations WHERE item_id = #{itemId} AND borrower_id = #{borrowerId}")
    Long conversation(@Param("itemId") long itemId, @Param("borrowerId") long borrowerId);

    @Insert("""
            INSERT INTO borrow_logs(borrow_id, actor_id, from_status, to_status, reason, created_at)
            VALUES(#{borrowId}, #{actorId}, #{fromStatus}, #{toStatus}, #{reason}, #{createdAt})
            """)
    int insertLog(BorrowLogRow row);

    @Select("""
            SELECT l.*, u.nickname AS actor_nickname FROM borrow_logs l LEFT JOIN users u ON u.id = l.actor_id
            WHERE l.borrow_id = #{id} ORDER BY l.id ASC
            """)
    List<BorrowLogRow> logs(long id);

    String FILTER = """
            <choose><when test="owner">b.owner_id</when><otherwise>b.borrower_id</otherwise></choose> = #{userId}
            <if test="statuses != null">AND b.status IN
            <foreach collection="statuses" item="status" open="(" separator="," close=")">#{status}</foreach></if>
            """;

    @Select("<script>" + DETAIL + " WHERE " + FILTER + " ORDER BY b.id DESC LIMIT #{size} OFFSET #{offset}</script>")
    List<BorrowRow> list(@Param("userId") long userId, @Param("owner") boolean owner,
                         @Param("statuses") List<String> statuses, @Param("size") int size, @Param("offset") long offset);

    @Select("<script>SELECT COUNT(*) FROM borrows b WHERE " + FILTER + "</script>")
    long count(@Param("userId") long userId, @Param("owner") boolean owner, @Param("statuses") List<String> statuses);
}
