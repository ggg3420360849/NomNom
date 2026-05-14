package com.nomnom.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("dishes_type")
public class DishesTypeEntity {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private String iconUri;
    private String name;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getIconUri() { return iconUri; }
    public void setIconUri(String iconUri) { this.iconUri = iconUri; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
