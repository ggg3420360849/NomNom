package com.nomnom.backend.mapper;

import com.nomnom.backend.model.OrderSummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderReadMapper {

    @Select("""
            <script>
            SELECT
                o.code AS orderNo,
                COALESCE(u.username, u.account, 'Guest') AS customerName,
                CASE COALESCE(o.status, 0)
                    WHEN 1 THEN 'PENDING_PAYMENT'
                    WHEN 2 THEN 'PAID'
                    WHEN 3 THEN 'PREPARING'
                    WHEN 4 THEN 'COMPLETED'
                    ELSE 'UNKNOWN'
                END AS status,
                COALESCE(o.total_price, 0) AS totalAmount,
                COALESCE(SUM(oi.buy_number), 0) AS itemCount
            FROM orders o
            LEFT JOIN `user` u ON u.id = o.user_id
            LEFT JOIN orders_item oi ON oi.orders_id = o.id
            WHERE 1 = 1
            <if test="userId != null">
                AND o.user_id = #{userId}
            </if>
            GROUP BY o.id, o.code, u.username, u.account, o.status, o.total_price, o.create_time
            ORDER BY o.create_time DESC, o.id DESC
            LIMIT 20
            </script>
            """)
    List<OrderSummary> selectCurrentOrders(@Param("userId") Long userId);
}
