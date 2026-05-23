package com.nomnom.backend.mapper;

import com.nomnom.backend.entity.CommentEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select; // 必须导入这个！
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CommentMapper {
    @Insert("INSERT INTO comment(order_id, star, content, create_time) VALUES(#{orderId}, #{star}, #{content}, #{createTime})")
    int insertComment(
            @Param("orderId") Long orderId,
            @Param("star") Integer star,
            @Param("content") String content,
            @Param("createTime") LocalDateTime createTime
    );

    @Select("SELECT * FROM comment ORDER BY create_time DESC")
    List<CommentEntity> selectAll();
}