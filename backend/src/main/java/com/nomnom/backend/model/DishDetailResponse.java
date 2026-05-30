package com.nomnom.backend.model;

import com.nomnom.backend.entity.DishesPackageEntity;

import java.util.Date;
import java.util.List;

public class DishDetailResponse {

    private Integer id;
    private Integer typeId;
    private String name;
    private String detail;
    private String coverUrl;
    private Integer status;
    private Integer stock;
    private String specs;
    private Integer isRecommend;
    private String recommendIds;
    private Date createTime;
    private List<DishesPackageEntity> packages;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getTypeId() { return typeId; }
    public void setTypeId(Integer typeId) { this.typeId = typeId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public String getSpecs() { return specs; }
    public void setSpecs(String specs) { this.specs = specs; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    public List<DishesPackageEntity> getPackages() { return packages; }
    public void setPackages(List<DishesPackageEntity> packages) { this.packages = packages; }

    public Integer getIsRecommend() { return isRecommend; }
    public void setIsRecommend(Integer isRecommend) { this.isRecommend = isRecommend; }

    public String getRecommendIds() { return recommendIds; }
    public void setRecommendIds(String recommendIds) { this.recommendIds = recommendIds; }
}
