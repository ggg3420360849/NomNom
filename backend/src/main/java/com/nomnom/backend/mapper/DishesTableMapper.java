package com.nomnom.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nomnom.backend.entity.DishesTableEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DishesTableMapper extends BaseMapper<DishesTableEntity> {
}
