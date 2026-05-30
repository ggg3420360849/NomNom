package com.nomnom.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nomnom.backend.common.ApiResponse;
import com.nomnom.backend.entity.DishesTableEntity;
import com.nomnom.backend.mapper.DishesTableMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/tables")
public class TableController {

    private static final int TABLE_STATUS_IDLE = 0;
    private static final int TABLE_STATUS_SEATED = 1;
    private static final int TABLE_STATUS_REPAIR = 6;

    private final DishesTableMapper tableMapper;

    public TableController(DishesTableMapper tableMapper) {
        this.tableMapper = tableMapper;
    }

    @GetMapping("/available")
    public ApiResponse<List<DishesTableEntity>> availableTables() {
        return ApiResponse.ok(tableMapper.selectList(new LambdaQueryWrapper<DishesTableEntity>()
                .eq(DishesTableEntity::getStatus, TABLE_STATUS_IDLE)
                .orderByAsc(DishesTableEntity::getNumber)));
    }

    @PutMapping("/{id}/seat")
    public ApiResponse<DishesTableEntity> seatTable(@PathVariable Long id, HttpServletRequest request) {
        Long userId = requireCurrentUserId(request);
        DishesTableEntity table = tableMapper.selectById(id);
        if (table == null) {
            return ApiResponse.fail("桌台不存在");
        }
        Integer status = table.getStatus() != null ? table.getStatus() : TABLE_STATUS_IDLE;
        if (status == TABLE_STATUS_REPAIR) {
            return ApiResponse.fail("桌台维修中，请选择其他桌台");
        }
        if (status != TABLE_STATUS_IDLE) {
            if (isOccupiedByCurrentUser(table, userId)) {
                return ApiResponse.ok("桌台已选择", table);
            }
            return ApiResponse.fail("桌台已入座，请选择其他空闲桌台");
        }
        table.setStatus(TABLE_STATUS_SEATED);
        table.setOccupiedBy(userId);
        tableMapper.updateById(table);
        return ApiResponse.ok("桌台已选择", table);
    }

    @PutMapping("/{id}/release")
    public ApiResponse<DishesTableEntity> releaseTable(@PathVariable Long id, HttpServletRequest request) {
        Long userId = requireCurrentUserId(request);
        DishesTableEntity table = tableMapper.selectById(id);
        if (table == null) {
            return ApiResponse.fail("桌台不存在");
        }
        Integer status = table.getStatus() != null ? table.getStatus() : TABLE_STATUS_IDLE;
        if (status == TABLE_STATUS_REPAIR) {
            return ApiResponse.fail("桌台维修中，不能释放");
        }
        if (!isOccupiedByCurrentUser(table, userId)) {
            return ApiResponse.fail("只能释放自己选择的桌台");
        }
        if (hasActiveAmount(table)) {
            return ApiResponse.fail("桌台已有消费记录，不能直接换桌");
        }
        table.setStatus(TABLE_STATUS_IDLE);
        table.setAmount(BigDecimal.ZERO);
        table.setOccupiedBy(null);
        tableMapper.updateById(table);
        return ApiResponse.ok("桌台已释放", table);
    }

    private boolean hasActiveAmount(DishesTableEntity table) {
        return table.getAmount() != null && table.getAmount().compareTo(BigDecimal.ZERO) > 0;
    }

    private boolean isOccupiedByCurrentUser(DishesTableEntity table, Long userId) {
        return table.getOccupiedBy() != null && table.getOccupiedBy().equals(userId);
    }

    private Long requireCurrentUserId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            throw new IllegalArgumentException("请先登录");
        }
        return userId;
    }
}
