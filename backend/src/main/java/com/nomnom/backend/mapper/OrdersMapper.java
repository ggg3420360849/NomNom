package com.nomnom.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nomnom.backend.entity.OrdersEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrdersMapper extends BaseMapper<OrdersEntity> {
}
