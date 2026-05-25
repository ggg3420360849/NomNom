package com.nomnom.backend.mapper;

import com.nomnom.backend.model.EvaluationResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface EvaluationReadMapper {

    @Select("""
            <script>
            SELECT
                e.id AS id,
                e.dishes_id AS dishesId,
                e.orders_id AS ordersId,
                o.code AS orderNo,
                e.user_id AS userId,
                u.username AS userName,
                u.avatar AS userAvatar,
                d.name AS dishName,
                e.content AS content,
                e.rating_value AS ratingValue,
                e.reply_content AS replyContent,
                e.reply_time AS replyTime,
                e.reply_status AS replyStatus,
                e.create_time AS createTime
            FROM dishes_evaluations e
            LEFT JOIN user u ON u.id = e.user_id
            LEFT JOIN dishes d ON d.id = e.dishes_id
            LEFT JOIN orders o ON o.id = e.orders_id
            WHERE e.dishes_id = #{dishesId}
            ORDER BY e.create_time DESC
            </script>
            """)
    List<EvaluationResponse> selectDishEvaluations(@Param("dishesId") Long dishesId);

    @Select("""
            <script>
            SELECT
                e.id AS id,
                e.dishes_id AS dishesId,
                e.orders_id AS ordersId,
                o.code AS orderNo,
                e.user_id AS userId,
                u.username AS userName,
                u.avatar AS userAvatar,
                d.name AS dishName,
                e.content AS content,
                e.rating_value AS ratingValue,
                e.reply_content AS replyContent,
                e.reply_time AS replyTime,
                e.reply_status AS replyStatus,
                e.create_time AS createTime
            FROM dishes_evaluations e
            LEFT JOIN user u ON u.id = e.user_id
            LEFT JOIN dishes d ON d.id = e.dishes_id
            LEFT JOIN orders o ON o.id = e.orders_id
            WHERE e.user_id = #{userId}
            ORDER BY e.create_time DESC
            </script>
            """)
    List<EvaluationResponse> selectUserEvaluations(@Param("userId") Long userId);

    @Select("""
            <script>
            SELECT
                e.id AS id,
                e.dishes_id AS dishesId,
                e.orders_id AS ordersId,
                o.code AS orderNo,
                e.user_id AS userId,
                COALESCE(u.username, u.account, '顾客') AS userName,
                u.avatar AS userAvatar,
                d.name AS dishName,
                e.content AS content,
                e.rating_value AS ratingValue,
                e.reply_content AS replyContent,
                e.reply_time AS replyTime,
                e.reply_status AS replyStatus,
                e.create_time AS createTime
            FROM dishes_evaluations e
            LEFT JOIN user u ON u.id = e.user_id
            LEFT JOIN dishes d ON d.id = e.dishes_id
            LEFT JOIN orders o ON o.id = e.orders_id
            ORDER BY e.create_time DESC
            </script>
            """)
    List<EvaluationResponse> selectAllEvaluations();
}
