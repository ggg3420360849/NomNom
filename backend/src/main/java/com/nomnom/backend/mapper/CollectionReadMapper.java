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
                d.name AS dishName,
                d.cover_url AS dishCover,
                COALESCE(dp.minPrice, 0) AS price,
                d.status AS dishStatus
            FROM collection c
            LEFT JOIN dishes d ON d.id = c.dishes_id
            LEFT JOIN (
                SELECT dishes_id, MIN(price) AS minPrice
                FROM dishes_package
                GROUP BY dishes_id
            ) dp ON dp.dishes_id = c.dishes_id
            WHERE c.user_id = #{userId}
            ORDER BY c.id DESC
            </script>
            """)
    List<CollectionItemResponse> selectUserCollections(@Param("userId") Long userId);
}
