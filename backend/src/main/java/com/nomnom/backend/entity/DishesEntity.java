package com.nomnom.backend.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("dishes")
public class DishesEntity {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer typeId;         // 菜品分类ID
    private String name;            // 菜品名称
    private String detail;          // 菜品详情
    private String coverUrl;        // 封面图URL
    private Integer status;         // 状态（0下架，1上架）
    private Date createTime;        // 创建时间

    // --- 下面这两个是我们为了完善功能刚刚新增的字段 ---
    private Integer stock;          // 库存
    private String specs;           // 规格口味 (JSON格式字符串)

    private Integer isRecommend;    // 是否店长推荐
    private String recommendIds;    // 推荐搭配菜品ID (JSON数组字符串)


    // ==========================================
    // 下面是 Getter 和 Setter 方法 (如果没有用 Lombok，必须要有这些)
    // ==========================================

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getTypeId() {
        return typeId;
    }

    public void setTypeId(Integer typeId) {
        this.typeId = typeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public String getSpecs() {
        return specs;
    }

    public void setSpecs(String specs) {
        this.specs = specs;
    }

    public Integer getIsRecommend() { return isRecommend; }
    public void setIsRecommend(Integer isRecommend) { this.isRecommend = isRecommend; }

    public String getRecommendIds() { return recommendIds; }
    public void setRecommendIds(String recommendIds) { this.recommendIds = recommendIds; }
}
