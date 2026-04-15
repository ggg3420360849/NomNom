package com.nomnom.backend.service;

import com.nomnom.backend.mapper.MenuReadMapper;
import com.nomnom.backend.model.MenuCategory;
import com.nomnom.backend.model.MenuItem;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MenuService {

    private final MenuReadMapper menuReadMapper;

    public MenuService(MenuReadMapper menuReadMapper) {
        this.menuReadMapper = menuReadMapper;
    }

    public List<MenuCategory> getCategories() {
        return menuReadMapper.selectCategories();
    }

    public List<MenuItem> getMenuItems(String category, String keyword) {
        String normalizedCategory = category == null ? null : category.trim();
        String normalizedKeyword = keyword == null ? null : keyword.trim();
        return menuReadMapper.selectMenuItems(normalizedCategory, normalizedKeyword);
    }
}
