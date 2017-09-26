package com.example;

/**
 * ClassDescribe:
 * Author :wangzhanhua
 * Date: 2017-09-26
 * Since
 * To change this template use File | Settings | File Templates.
 */
public class StockInfo {
    private Long stockid;
    private Integer stocknum;
    private Integer pickoutnum;

    public Long getStockid() {
        return stockid;
    }

    public void setStockid(Long stockid) {
        this.stockid = stockid;
    }

    public Integer getStocknum() {
        return stocknum;
    }

    public void setStocknum(Integer stocknum) {
        this.stocknum = stocknum;
    }

    public Integer getPickoutnum() {
        return pickoutnum;
    }

    public void setPickoutnum(Integer pickoutnum) {
        this.pickoutnum = pickoutnum;
    }

    @Override
    public String toString() {
        return "StockInfo{" +
                "stockid=" + stockid +
                ", stocknum=" + stocknum +
                ", pickoutnum=" + pickoutnum +
                '}';
    }
}
