package com.nomnom.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nomnom.backend.entity.MessagesEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MessagesMapper extends BaseMapper<MessagesEntity> {
}
