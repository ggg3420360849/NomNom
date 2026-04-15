package com.nomnom.backend.mapper;

import com.nomnom.backend.model.MenuCategory;
import com.nomnom.backend.model.MenuItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MenuReadMapper {

    @Select("""
            SELECT
                CAST(dt.id AS CHAR) AS code,
                dt.name AS name,
                COALESCE(dt.icon_uri, '') AS description
            FROM dishes_type dt
            ORDER BY dt.id
            """)
    List<MenuCategory> selectCategories();

    @Select("""
            <script>
            SELECT
                dp.id AS id,
                CAST(d.type_id AS CHAR) AS category,
                COALESCE(dt.name, 'Uncategorized') AS categoryLabel,
                CONCAT(
                    d.name,
                    CASE WHEN dp.name IS NOT NULL AND dp.name &lt;&gt; '' THEN CONCAT(' - ', dp.name) ELSE '' END,
                    CASE WHEN dp.specs IS NOT NULL AND dp.specs &lt;&gt; '' THEN CONCAT(' / ', dp.specs) ELSE '' END
                ) AS name,
                COALESCE(dp.price, 0) AS price,
                COALESCE(d.detail, '') AS description,
                COALESCE(d.cover_url, '') AS coverUrl,
                CASE WHEN COALESCE(d.status, 0) = 1 THEN 'AVAILABLE' ELSE 'SOLD_OUT' END AS status,
                0 AS monthlySales
            FROM dishes_package dp
            JOIN dishes d ON d.id = dp.dishes_id
            LEFT JOIN dishes_type dt ON dt.id = d.type_id
            WHERE 1 = 1
            <if test="category != null and category != ''">
                AND CAST(d.type_id AS CHAR) = #{category}
            </if>
            <if test="keyword != null and keyword != ''">
                AND (
                    d.name LIKE CONCAT('%', #{keyword}, '%')
                    OR COALESCE(dp.name, '') LIKE CONCAT('%', #{keyword}, '%')
                    OR COALESCE(dp.specs, '') LIKE CONCAT('%', #{keyword}, '%')
                    OR COALESCE(d.detail, '') LIKE CONCAT('%', #{keyword}, '%')
                )
            </if>
            ORDER BY d.create_time DESC, d.id DESC, dp.id DESC
            </script>
            """)
    List<MenuItem> selectMenuItems(@Param("category") String category, @Param("keyword") String keyword);
}
