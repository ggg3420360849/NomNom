package com.nomnom.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("dishes_table")
public class DishesTableEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String number;
    private Integer personNumber;
    private Integer status;
    private LocalDateTime createTime;
    private String area;
    private BigDecimal amount;
    private BigDecimal minSpend;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }

    public Integer getPersonNumber() { return personNumber; }
    public void setPersonNumber(Integer personNumber) { this.personNumber = personNumber; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getMinSpend() { return minSpend; }
    public void setMinSpend(BigDecimal minSpend) { this.minSpend = minSpend; }
}
