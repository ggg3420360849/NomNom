package com.nomnom.backend.mapper;

import com.nomnom.backend.model.CartItemResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CartReadMapper {

    @Select("""
            <script>
            SELECT
                sc.id AS id,
                sc.user_id AS userId,
                sc.dishes_id AS dishesId,
                sc.dishes_package_id AS packageId,
                sc.buy_number AS buyNumber,
                sc.is_seleced AS isSelected,
                d.name AS dishName,
                d.cover_url AS dishCover,
                COALESCE(dp.name, '') AS packageName,
                COALESCE(dp.price, 0) AS price,
                COALESCE(dp.specs, '') AS specs
            FROM shopping_car sc
            LEFT JOIN dishes d ON d.id = sc.dishes_id
            LEFT JOIN dishes_package dp ON dp.id = sc.dishes_package_id
            WHERE sc.user_id = #{userId}
            ORDER BY sc.create_time DESC
            </script>
            """)
    List<CartItemResponse> selectCartItems(@Param("userId") Long userId);
}
