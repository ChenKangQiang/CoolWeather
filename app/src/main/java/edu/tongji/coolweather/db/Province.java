package edu.tongji.coolweather.db;

import org.litepal.crud.DataSupport;

/**
 * Created by chen on 2017/11/15.
 */

public class Province extends DataSupport {
    private int provinceId;
    private String provinceName;

    public int getProvinceId() {
        return provinceId;
    }

    public void setProvinceId(int provinceId) {
        this.provinceId = provinceId;
    }

    public String getProvinceName() {
        return provinceName;
    }

    public void setProvinceName(String provinceName) {
        this.provinceName = provinceName;
    }
}
