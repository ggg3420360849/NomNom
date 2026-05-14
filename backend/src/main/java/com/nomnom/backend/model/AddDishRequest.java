package com.nomnom.backend.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

public class AddDishRequest {

    private String name;

    @JsonProperty("type_id")
    private Integer typeId;

    private String detail;

    @JsonProperty("cover_url")
    private String coverUrl;

    private Integer status;
    private Integer stock;
    private BigDecimal price;
    private BigDecimal memberPrice;
    private List<String> specs;
    private List<String> tastes;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getTypeId() { return typeId; }
    public void setTypeId(Integer typeId) { this.typeId = typeId; }

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

    public List<String> getSpecs() { return specs; }
    public void setSpecs(List<String> specs) { this.specs = specs; }

    public List<String> getTastes() { return tastes; }
    public void setTastes(List<String> tastes) { this.tastes = tastes; }
}
