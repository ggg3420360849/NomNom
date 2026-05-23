package com.nomnom.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nomnom.backend.entity.OrdersEntity;
import com.nomnom.backend.model.PackageSnapshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrdersMapper extends BaseMapper<OrdersEntity> {

    /**
     * 获取订单中的套餐列表（用于评价弹窗）
     */
    @Select("SELECT " +
            "oi.id as order_item_id, " +
            "oi.dishes_package_id as package_id, " +
            "oi.buy_number as buy_number, " +
            "oi.snap_price as price, " +
            "oi.snap_name as package_name, " +
            "oi.snap_cover as cover_url, " +
            "d.name as dish_name " +
            "FROM orders_item oi " +
            "LEFT JOIN dishes_package dp ON oi.dishes_package_id = dp.id " +
            "LEFT JOIN dishes d ON dp.dishes_id = d.id " +
            "WHERE oi.orders_id = #{orderId}")
    List<PackageSnapshot> getOrderPackagesForEvaluation(@Param("orderId") Long orderId);
}