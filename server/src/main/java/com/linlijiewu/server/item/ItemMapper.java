package com.linlijiewu.server.item;

import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface ItemMapper {
    @Insert("""
            INSERT INTO items(owner_id, title, category, description, image_url, pickup_lat, pickup_lng, pickup_label, status)
            VALUES(#{ownerId}, #{title}, #{category}, #{description}, #{imageUrl}, #{pickupLat}, #{pickupLng}, #{pickupLabel}, 'AVAILABLE')
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ItemRow item);
    @Select("SELECT i.*, u.nickname AS owner_nickname FROM items i JOIN users u ON u.id = i.owner_id WHERE i.id = #{id}")
    ItemRow find(long id);
    @Select("SELECT * FROM items WHERE id = #{id} FOR UPDATE")
    ItemRow lock(long id);
    @Update("""
            UPDATE items SET title = #{title}, category = #{category}, description = #{description},
            image_url = #{imageUrl}, pickup_lat = #{pickupLat}, pickup_lng = #{pickupLng}, pickup_label = #{pickupLabel}
            WHERE id = #{id}
            """)
    int update(ItemRow item);
    @Update("UPDATE items SET status = 'OFFLINE' WHERE id = #{id}")
    int offline(long id);
    @Select("""
            <script>
            SELECT i.*, u.nickname AS owner_nickname FROM items i JOIN users u ON u.id = i.owner_id
            WHERE i.owner_id = #{ownerId}
            <if test="statuses != null">AND i.status IN
            <foreach collection="statuses" item="status" open="(" separator="," close=")">#{status}</foreach></if>
            ORDER BY i.id DESC LIMIT #{size} OFFSET #{offset}
            </script>
            """)
    List<ItemRow> mine(@Param("ownerId") long ownerId, @Param("statuses") List<String> statuses,
                       @Param("size") int size, @Param("offset") long offset);
    @Select("""
            <script>SELECT COUNT(*) FROM items WHERE owner_id = #{ownerId}
            <if test="statuses != null">AND status IN
            <foreach collection="statuses" item="status" open="(" separator="," close=")">#{status}</foreach></if>
            </script>
            """)
    long countMine(@Param("ownerId") long ownerId, @Param("statuses") List<String> statuses);

    @Select("""
            <script>
            SELECT i.*, u.nickname AS owner_nickname FROM items i JOIN users u ON u.id = i.owner_id
            WHERE i.status IN
            <foreach collection="statuses" item="status" open="(" separator="," close=")">#{status}</foreach>
            AND i.pickup_lat BETWEEN #{minLat} AND #{maxLat}
            <if test="!allLongitudes">
                <choose><when test="wrap">AND (i.pickup_lng &gt;= #{minLng} OR i.pickup_lng &lt;= #{maxLng})</when>
                <otherwise>AND i.pickup_lng BETWEEN #{minLng} AND #{maxLng}</otherwise></choose>
            </if>
            <if test="category != null">AND i.category = #{category}</if>
            <if test="keyword != null">
                AND (LOCATE(LOWER(#{keyword}), LOWER(i.title)) &gt; 0
                     OR LOCATE(LOWER(#{keyword}), LOWER(i.description)) &gt; 0)
            </if>
            </script>
            """)
    List<ItemRow> nearby(@Param("statuses") List<String> statuses, @Param("category") String category,
            @Param("keyword") String keyword, @Param("minLat") double minLat, @Param("maxLat") double maxLat,
            @Param("minLng") double minLng, @Param("maxLng") double maxLng,
            @Param("wrap") boolean wrap, @Param("allLongitudes") boolean allLongitudes);
}
