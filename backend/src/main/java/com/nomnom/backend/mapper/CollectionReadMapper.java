package com.nomnom.backend.mapper;

import com.nomnom.backend.model.CollectionItemResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CollectionReadMapper {

    @Select("""
            <script>
            SELECT
                c.id AS id,
                c.dishes_id AS dishesId,
                dp.id AS packageId,
                d.name AS dishName,
                d.cover_url AS dishCover,
                COALESCE(dp.price, 0) AS price,
                d.status AS dishStatus
            FROM collection c
            LEFT JOIN dishes d ON d.id = c.dishes_id
            LEFT JOIN dishes_package dp ON dp.id = (
                SELECT p.id
                FROM dishes_package p
                WHERE p.dishes_id = c.dishes_id
                ORDER BY COALESCE(p.price, 0), p.id
                LIMIT 1
            )
            WHERE c.user_id = #{userId}
            ORDER BY c.id DESC
            </script>
            """)
    List<CollectionItemResponse> selectUserCollections(@Param("userId") Long userId);
}
