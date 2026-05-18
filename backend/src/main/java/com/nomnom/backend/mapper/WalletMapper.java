package com.nomnom.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nomnom.backend.entity.WalletEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WalletMapper extends BaseMapper<WalletEntity> {
}
