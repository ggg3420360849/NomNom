package com.nomnom.backend.mapper;

import com.nomnom.backend.model.OrderSummary;
import com.nomnom.backend.model.OrderItemSummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderReadMapper {

    @Select("""
            <script>
            SELECT
                o.id AS id,
                o.code AS orderNo,
                COALESCE(u.username, u.account, 'Guest') AS customerName,
                o.table_id AS tableId,
                COALESCE(o.table_number, '') AS tableNumber,
                CASE COALESCE(o.status, 0)
                    WHEN 1 THEN 'PENDING_PAYMENT'
                    WHEN 2 THEN 'PAID'
                    WHEN 3 THEN 'PREPARING'
                    WHEN 4 THEN 'COMPLETED'
                    WHEN 5 THEN 'CANCELED'
                    ELSE 'UNKNOWN'
                END AS status,
                COALESCE(o.total_price, 0) AS totalAmount,
                COALESCE(SUM(oi.buy_number), 0) AS itemCount,
                o.create_time AS createTime,
                rr.status AS refundStatus,
                rr.refund_cause AS refundCause,
                rr.reject_refund_cause AS rejectRefundCause,
                rr.refun_time AS refundTime
            FROM orders o
            LEFT JOIN `user` u ON u.id = o.user_id
            LEFT JOIN orders_item oi ON oi.orders_id = o.id
            LEFT JOIN orders_refund_reply rr ON rr.orders_id = o.id
            WHERE 1 = 1
            <if test="userId != null">
                AND o.user_id = #{userId}
            </if>
            GROUP BY o.id, o.code, u.username, u.account, o.table_id, o.table_number, o.status, o.total_price, o.create_time,
                rr.status, rr.refund_cause, rr.reject_refund_cause, rr.refun_time
            ORDER BY o.create_time DESC, o.id DESC
            LIMIT 20
            </script>
            """)
    List<OrderSummary> selectCurrentOrders(@Param("userId") Long userId);

    @Select("""
            <script>
            SELECT
                oi.id AS id,
                oi.orders_id AS orderId,
                oi.dishes_package_id AS packageId,
                dp.dishes_id AS dishesId,
                COALESCE(oi.snap_name, '') AS name,
                COALESCE(oi.snap_cover, '') AS coverUrl,
                COALESCE(oi.snap_price, 0) AS price,
                COALESCE(oi.buy_number, 0) AS quantity,
                COALESCE(oi.total_price, 0) AS totalPrice
            FROM orders_item oi
            LEFT JOIN dishes_package dp ON dp.id = oi.dishes_package_id
            WHERE oi.orders_id IN
            <foreach collection="orderIds" item="id" open="(" separator="," close=")">
                #{id}
            </foreach>
            ORDER BY oi.id
            </script>
            """)
    List<OrderItemSummary> selectOrderItems(@Param("orderIds") List<Long> orderIds);
}
