package com.linlijiewu.server.image;

import org.apache.ibatis.annotations.*;

@Mapper
public interface ImageMapper {
    @Insert("INSERT INTO images(image_id, owner_id, file_name, size_bytes) VALUES(#{imageId}, #{ownerId}, #{fileName}, #{sizeBytes})")
    int insert(ImageRow image);
    @Select("SELECT image_id, owner_id, file_name, size_bytes FROM images WHERE image_id = #{id}")
    ImageRow find(String id);
    @Select("SELECT image_id, owner_id, file_name, size_bytes FROM images WHERE file_name = #{name}")
    ImageRow findByFileName(String name);
}
