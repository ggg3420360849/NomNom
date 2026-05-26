package com.nomnom.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nomnom.backend.common.ApiResponse;
import com.nomnom.backend.entity.DishesTableEntity;
import com.nomnom.backend.mapper.DishesTableMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tables")
public class TableController {

    private final DishesTableMapper tableMapper;

    public TableController(DishesTableMapper tableMapper) {
        this.tableMapper = tableMapper;
    }

    @GetMapping("/available")
    public ApiResponse<List<DishesTableEntity>> availableTables() {
        return ApiResponse.ok(tableMapper.selectList(new LambdaQueryWrapper<DishesTableEntity>()
                .orderByAsc(DishesTableEntity::getArea)
                .orderByAsc(DishesTableEntity::getNumber)));
    }
}
