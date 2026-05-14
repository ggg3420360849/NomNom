package com.nomnom.backend.model;

import java.math.BigDecimal;
import java.util.Date;

public class DishListResponse {

    private Integer id;
    private Integer typeId;
    private String typeName;
    private String name;
    private String detail;
    private String coverUrl;
    private Integer status;
    private Integer stock;
    private BigDecimal price;
    private BigDecimal memberPrice;
    private String specs;
    private Date createTime;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getTypeId() { return typeId; }
    public void setTypeId(Integer typeId) { this.typeId = typeId; }

    public String getTypeName() { return typeName; }
    public void setTypeName(String typeName) { this.typeName = typeName; }

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

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getMemberPrice() { return memberPrice; }
    public void setMemberPrice(BigDecimal memberPrice) { this.memberPrice = memberPrice; }

    public String getSpecs() { return specs; }
    public void setSpecs(String specs) { this.specs = specs; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
