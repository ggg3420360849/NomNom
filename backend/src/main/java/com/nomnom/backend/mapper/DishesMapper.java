package com.nomnom.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nomnom.backend.entity.DishesEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DishesMapper extends BaseMapper<DishesEntity> {
}
